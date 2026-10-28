package com.oxtore.backend.order.service;

import com.oxtore.backend.common.security.StoreAccess;
import com.oxtore.backend.notification.enums.NotificationType;
import com.oxtore.backend.notification.service.NotificationRecipients;
import com.oxtore.backend.notification.service.NotificationService;
import com.oxtore.backend.order.DTO.OrderResponse;
import com.oxtore.backend.order.DTO.PlaceOrderRequest;
import com.oxtore.backend.order.entity.CustomerOrder;
import com.oxtore.backend.order.entity.CustomerOrderItem;
import com.oxtore.backend.order.enums.OrderStatus;
import com.oxtore.backend.order.enums.PaymentMethod;
import com.oxtore.backend.order.repository.CustomerOrderRepository;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.enums.ProductStatus;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.stock.service.StockService;
import com.oxtore.backend.upload.ImageUrlResolver;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Marketplace orders paid cash on delivery. Placing an order only checks stock; stock is taken (as a sale, so it
 * shows in the store's sales history) when the store confirms. Then the store ships and marks it delivered.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ICON = "receipt-outline";

    private final CustomerOrderRepository orderRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final StoreAccess storeAccess;
    private final NotificationService notificationService;
    private final NotificationRecipients notificationRecipients;
    private final ImageUrlResolver imageUrlResolver;

    // ─── Buyer ────────────────────────────────────────────────────────

    /** One order per boutique in the cart, all or nothing. */
    @Transactional
    public List<OrderResponse> place(Long buyerId, PlaceOrderRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Your cart is empty");
        }
        String name = required(request.customerName(), "Full name", 100);
        String phone = required(request.customerPhone(), "Phone number", 30);
        String city = required(request.city(), "City", 100);
        String address = required(request.address(), "Address", 255);
        String note = optional(request.note(), "Note", 500);
        PaymentMethod paymentMethod = PaymentMethod.fromApi(request.paymentMethod());
        BigDecimal deliveryFee = request.deliveryFee() == null ? BigDecimal.ZERO : request.deliveryFee();
        if (deliveryFee.signum() < 0) throw new IllegalArgumentException("Delivery fee can't be negative");

        // boutiqueId → (productId → quantity), in cart order, the same product added twice merged
        Map<Long, Map<Long, Integer>> byStore = new LinkedHashMap<>();
        for (PlaceOrderRequest.Item item : request.items()) {
            if (item == null || item.productId() == null || item.boutiqueId() == null) {
                throw new IllegalArgumentException("Each item needs a product and a boutique");
            }
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }
            byStore.computeIfAbsent(item.boutiqueId(), k -> new LinkedHashMap<>())
                    .merge(item.productId(), item.quantity(), Integer::sum);
        }

        List<CustomerOrder> orders = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Integer>> storeEntry : byStore.entrySet()) {
            CustomerOrder order = CustomerOrder.builder()
                    .reference(newReference())
                    .buyerId(buyerId)
                    .status(OrderStatus.PENDING)
                    .paymentMethod(paymentMethod)
                    .customerName(name)
                    .customerPhone(phone)
                    .deliveryCity(city)
                    .deliveryAddress(address)
                    .note(note)
                    .deliveryFee(deliveryFee)
                    .build();

            BigDecimal subtotal = BigDecimal.ZERO;
            for (Map.Entry<Long, Integer> line : storeEntry.getValue().entrySet()) {
                Stock stock = orderableStock(line.getKey(), storeEntry.getKey(), line.getValue());
                order.setStore(stock.getStore());
                BigDecimal lineTotal = stock.getRetailPrice().multiply(BigDecimal.valueOf(line.getValue()));
                order.addItem(CustomerOrderItem.builder()
                        .product(stock.getProduct())
                        .productName(stock.getProduct().getProductName())
                        .unitPrice(stock.getRetailPrice())
                        .quantity(line.getValue())
                        .lineTotal(lineTotal)
                        .build());
                subtotal = subtotal.add(lineTotal);
            }
            order.setSubtotal(subtotal);
            order.setTotal(subtotal.add(deliveryFee));
            orders.add(orderRepository.save(order));
        }

        for (CustomerOrder order : orders) {
            notificationService.notify(
                    notificationRecipients.managersOf(order.getStore().getId()),
                    buyerId,
                    NotificationType.ORDER,
                    "Nouvelle commande",
                    "%s · %d article(s) · %s, %s".formatted(
                            order.getReference(), itemCount(order), order.getCustomerName(), order.getDeliveryCity()),
                    ICON,
                    meta(order, "store"));
        }
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getForBuyer(Long buyerId, Long orderId) {
        return toResponse(buyersOrder(buyerId, orderId));
    }

    /** Only while the store hasn't confirmed it yet; nothing to give back since stock is taken on confirmation. */
    @Transactional
    public OrderResponse cancelByBuyer(Long buyerId, Long orderId) {
        CustomerOrder order = buyersOrder(buyerId, orderId);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("This order can no longer be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        notificationService.notify(
                notificationRecipients.managersOf(order.getStore().getId()),
                buyerId,
                NotificationType.ORDER,
                "Commande annulée",
                "%s a été annulée par le client".formatted(order.getReference()),
                ICON,
                meta(order, "store"));
        return toResponse(order);
    }

    // ─── Store (supervisors and managers) ─────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderResponse> listForStore(Long userId, Long storeId, String status) {
        storeAccess.require(userId, storeId, StoreAccess.MANAGERS);
        List<CustomerOrder> orders = status == null || status.isBlank() || status.equalsIgnoreCase("all")
                ? orderRepository.findByStore_IdOrderByCreatedAtDesc(storeId)
                : orderRepository.findByStore_IdAndStatusOrderByCreatedAtDesc(storeId, OrderStatus.fromApi(status));
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getForStore(Long userId, Long storeId, Long orderId) {
        storeAccess.require(userId, storeId, StoreAccess.MANAGERS);
        return toResponse(storesOrder(storeId, orderId));
    }

    /**
     * confirmed (takes the stock, recorded as sales), shipped, delivered (cash collected) or rejected, following
     * {@link OrderStatus#storeTransitions()}.
     */
    @Transactional
    public OrderResponse updateStatusByStore(Long userId, Long storeId, Long orderId, String status, String reason) {
        storeAccess.require(userId, storeId, StoreAccess.MANAGERS);
        CustomerOrder order = storesOrder(storeId, orderId);
        OrderStatus target = OrderStatus.fromApi(status);
        if (!order.getStatus().storeTransitions().contains(target)) {
            throw new IllegalStateException("An order that is %s can't be marked %s"
                    .formatted(order.getStatus().apiValue(), target.apiValue()));
        }

        if (target == OrderStatus.CONFIRMED) {
            for (CustomerOrderItem item : order.getItems()) {
                Stock stock = stockRepository.findByProduct_IdAndStore_Id(item.getProduct().getId(), storeId)
                        .orElseThrow(() -> new IllegalStateException(
                                "%s is no longer in this boutique's stock".formatted(item.getProductName())));
                if (stock.getQuantity() < item.getQuantity()) {
                    throw new IllegalStateException("Not enough stock for %s: %d left, the order needs %d"
                            .formatted(item.getProductName(), stock.getQuantity(), item.getQuantity()));
                }
                stockService.recordSale(stock.getId(), item.getQuantity(), userId);
            }
        }
        if (target == OrderStatus.REJECTED) {
            order.setRejectionReason(optional(reason, "Reason", 255));
        }
        order.setStatus(target);

        notificationService.notify(
                List.of(order.getBuyerId()),
                userId,
                NotificationType.ORDER,
                buyerTitle(target),
                "%s · %s".formatted(order.getReference(), order.getStore().getStoreName()),
                ICON,
                meta(order, "buyer"));
        return toResponse(order);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Stock orderableStock(Long productId, Long storeId, int quantity) {
        Stock stock = stockRepository.findByProduct_IdAndStore_Id(productId, storeId)
                .filter(s -> s.getSaleType() == SaleType.RETAIL)
                .filter(s -> s.getProduct().getStatus() != ProductStatus.ARCHIVED)
                .filter(s -> s.getRetailPrice() != null)
                .orElseThrow(() -> new IllegalArgumentException("A product in your cart is no longer available"));
        if (stock.getQuantity() < quantity) {
            Product p = stock.getProduct();
            throw new IllegalArgumentException(stock.getQuantity() == 0
                    ? "%s is out of stock".formatted(p.getProductName())
                    : "Only %d left of %s".formatted(stock.getQuantity(), p.getProductName()));
        }
        return stock;
    }

    private CustomerOrder buyersOrder(Long buyerId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getBuyerId().equals(buyerId))
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    private CustomerOrder storesOrder(Long storeId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getStore().getId().equals(storeId))
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    private String newReference() {
        String reference;
        do {
            StringBuilder sb = new StringBuilder("OXT-");
            for (int i = 0; i < 8; i++) sb.append(REFERENCE_ALPHABET.charAt(RANDOM.nextInt(REFERENCE_ALPHABET.length())));
            reference = sb.toString();
        } while (orderRepository.existsByReference(reference));
        return reference;
    }

    private static String buyerTitle(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Commande confirmée";
            case SHIPPED -> "Commande expédiée";
            case DELIVERED -> "Commande livrée";
            case REJECTED -> "Commande refusée";
            default -> "Commande mise à jour";
        };
    }

    /** audience: "store" (opens the boutique's order) or "buyer" (opens My orders). */
    private static Map<String, String> meta(CustomerOrder order, String audience) {
        return Map.of("orderId", String.valueOf(order.getId()),
                "boutiqueId", String.valueOf(order.getStore().getId()),
                "reference", order.getReference(),
                "audience", audience);
    }

    private static int itemCount(CustomerOrder order) {
        return order.getItems().stream().mapToInt(CustomerOrderItem::getQuantity).sum();
    }

    private static String required(String value, String field, int max) {
        String v = optional(value, field, max);
        if (v == null) throw new IllegalArgumentException(field + " is required");
        return v;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() > max) throw new IllegalArgumentException(field + " is too long (max " + max + ")");
        return v;
    }

    private OrderResponse toResponse(CustomerOrder o) {
        List<OrderResponse.Item> items = o.getItems().stream()
                .map(i -> new OrderResponse.Item(
                        String.valueOf(i.getProduct().getId()),
                        i.getProductName(),
                        imageUrlResolver.firstUrl(i.getProduct()),
                        i.getUnitPrice(),
                        i.getQuantity(),
                        i.getLineTotal()))
                .toList();
        return new OrderResponse(
                String.valueOf(o.getId()),
                o.getReference(),
                o.getStatus().apiValue(),
                o.getPaymentMethod().apiValue(),
                String.valueOf(o.getStore().getId()),
                o.getStore().getStoreName(),
                imageUrlResolver.toPublicUrl(o.getStore().getLogo()),
                o.getCustomerName(),
                o.getCustomerPhone(),
                o.getDeliveryCity(),
                o.getDeliveryAddress(),
                o.getNote(),
                o.getSubtotal(),
                o.getDeliveryFee(),
                o.getTotal(),
                itemCount(o),
                o.getRejectionReason(),
                o.getCreatedAt(),
                o.getUpdatedAt(),
                items);
    }
}
