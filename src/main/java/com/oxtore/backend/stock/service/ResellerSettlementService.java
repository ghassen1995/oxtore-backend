package com.oxtore.backend.stock.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.notification.enums.NotificationType;
import com.oxtore.backend.notification.service.NotificationRecipients;
import com.oxtore.backend.notification.service.NotificationService;
import com.oxtore.backend.stock.DTO.RecordResellerPaymentRequest;
import com.oxtore.backend.stock.DTO.ResellerPaymentResponse;
import com.oxtore.backend.stock.DTO.SupplierBalance;
import com.oxtore.backend.stock.entity.ResellerPayment;
import com.oxtore.backend.stock.entity.StockActivity;
import com.oxtore.backend.stock.enums.SettlementStatus;
import com.oxtore.backend.stock.repository.ResellerPaymentRepository;
import com.oxtore.backend.stock.repository.StockActivityRepository;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Money a reseller owes its source store: every sale of a source product adds quantity x unit price, every payment
 * the source records takes it down. Payments settle the oldest sales first; a sale's settlement status is derived
 * from that and kept in sync on stock_activity whenever a payment is recorded or removed.
 */
@Service
@RequiredArgsConstructor
public class ResellerSettlementService {

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0);
    private static final String ICON = "cash-outline";

    private final ResellerPaymentRepository paymentRepository;
    private final StockActivityRepository stockActivityRepository;
    private final StockRepository stockRepository;
    private final StoreRepository storeRepository;
    private final IdentityRepository identityRepository;
    private final NotificationService notificationService;
    private final NotificationRecipients notificationRecipients;

    public static BigDecimal lineTotal(StockActivity sale) {
        BigDecimal unitPrice = sale.getUnitPrice() != null ? sale.getUnitPrice() : BigDecimal.ZERO;
        int quantity = sale.getQuantitySold() != null ? sale.getQuantitySold() : 0;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * What is still unpaid on each sale, by sale id. Each reseller's payments cover its sales oldest first,
     * so a sale is fully paid (0), partly paid, or not paid at all.
     */
    public static Map<Long, BigDecimal> unpaidBySale(List<StockActivity> sales, Map<Long, BigDecimal> paidByReseller) {
        List<StockActivity> oldestFirst = new ArrayList<>(sales);
        oldestFirst.sort(Comparator.comparing(StockActivity::getSoldAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(StockActivity::getId));
        Map<Long, BigDecimal> credit = new HashMap<>(paidByReseller);
        Map<Long, BigDecimal> unpaid = new HashMap<>();
        for (StockActivity sale : oldestFirst) {
            Long resellerId = sale.getStock().getStore().getId();
            BigDecimal total = lineTotal(sale);
            BigDecimal available = credit.getOrDefault(resellerId, BigDecimal.ZERO);
            BigDecimal covered = available.min(total);
            credit.put(resellerId, available.subtract(covered));
            unpaid.put(sale.getId(), total.subtract(covered));
        }
        return unpaid;
    }

    /** Total paid per reseller store. */
    public static Map<Long, BigDecimal> paidByReseller(List<ResellerPayment> payments) {
        return payments.stream().collect(Collectors.groupingBy(p -> p.getResellerStore().getId(),
                Collectors.reducing(BigDecimal.ZERO, ResellerPayment::getAmount, BigDecimal::add)));
    }

    @Transactional
    public ResellerPaymentResponse record(Long sourceStoreId, Long userId, RecordResellerPaymentRequest body) {
        if (body == null || body.resellerStoreId() == null) {
            throw new IllegalArgumentException("resellerStoreId is required");
        }
        if (body.amount() == null || body.amount().signum() <= 0) {
            throw new IllegalArgumentException("The amount must be greater than 0");
        }
        Long resellerStoreId = body.resellerStoreId();
        if (resellerStoreId.equals(sourceStoreId)
                || stockRepository.findResellerStocks(sourceStoreId, resellerStoreId).isEmpty()) {
            throw new EntityNotFoundException("This boutique is not one of your resellers");
        }

        BigDecimal balance = balance(sourceStoreId, resellerStoreId);
        if (body.amount().compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    "The amount is more than the balance due (" + balance.stripTrailingZeros().toPlainString() + ")");
        }

        Store source = storeRepository.findById(sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + sourceStoreId));
        Store reseller = storeRepository.findById(resellerStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + resellerStoreId));
        String note = body.note() == null || body.note().isBlank() ? null : body.note().trim();
        if (note != null && note.length() > 500) note = note.substring(0, 500);

        ResellerPayment payment = paymentRepository.save(ResellerPayment.builder()
                .sourceStore(source)
                .resellerStore(reseller)
                .amount(body.amount())
                .note(note)
                .recordedBy(userId)
                .build());
        syncStatuses(sourceStoreId, resellerStoreId);

        BigDecimal remaining = balance.subtract(body.amount());
        notificationService.notify(
                notificationRecipients.managersOf(resellerStoreId),
                userId,
                NotificationType.PAYMENT,
                "Paiement enregistré",
                "%s a reçu %s de votre part. Reste dû : %s".formatted(source.getStoreName(),
                        body.amount().stripTrailingZeros().toPlainString(),
                        remaining.stripTrailingZeros().toPlainString()),
                ICON,
                Map.of("sourceStoreId", String.valueOf(sourceStoreId),
                        "resellerStoreId", String.valueOf(resellerStoreId),
                        "audience", "reseller"));
        return toResponse(payment, names(List.of(payment)));
    }

    /** Undo a payment recorded by mistake. */
    @Transactional
    public void delete(Long sourceStoreId, Long paymentId) {
        ResellerPayment payment = paymentRepository.findByIdAndSourceStore_Id(paymentId, sourceStoreId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        Long resellerStoreId = payment.getResellerStore().getId();
        paymentRepository.delete(payment);
        paymentRepository.flush();
        syncStatuses(sourceStoreId, resellerStoreId);
    }

    public List<ResellerPaymentResponse> paymentsForSource(Long sourceStoreId, Long resellerStoreId) {
        List<ResellerPayment> payments = paymentRepository.findForSource(sourceStoreId, resellerStoreId);
        Map<Long, String> names = names(payments);
        return payments.stream().map(p -> toResponse(p, names)).toList();
    }

    /** Reseller side: one entry per source store whose products it sold or paid for, biggest balance first. */
    @Transactional(readOnly = true)
    public List<SupplierBalance> supplierBalances(Long resellerStoreId) {
        List<StockActivity> sales = stockActivityRepository.findSalesOfSourcedProducts(resellerStoreId);
        List<ResellerPayment> payments = paymentRepository.findForReseller(resellerStoreId);
        Map<Long, String> names = names(payments);

        Map<Long, List<StockActivity>> salesBySource = sales.stream().collect(Collectors.groupingBy(
                a -> a.getStock().getProduct().getStoreId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<ResellerPayment>> paymentsBySource = payments.stream().collect(Collectors.groupingBy(
                p -> p.getSourceStore().getId(), LinkedHashMap::new, Collectors.toList()));

        Set<Long> sourceIds = new LinkedHashSet<>(salesBySource.keySet());
        sourceIds.addAll(paymentsBySource.keySet());
        Map<Long, String> storeNames = storeRepository.findAllById(sourceIds).stream()
                .collect(Collectors.toMap(Store::getId, Store::getStoreName));

        return sourceIds.stream().map(sourceId -> {
                    List<StockActivity> s = salesBySource.getOrDefault(sourceId, List.of());
                    List<ResellerPayment> p = paymentsBySource.getOrDefault(sourceId, List.of());
                    BigDecimal totalSales = s.stream().map(ResellerSettlementService::lineTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalPaid = p.stream().map(ResellerPayment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int sold = s.stream().mapToInt(a -> a.getQuantitySold() != null ? a.getQuantitySold() : 0).sum();
                    return new SupplierBalance(sourceId, storeNames.get(sourceId), sold, totalSales, totalPaid,
                            totalSales.subtract(totalPaid).max(BigDecimal.ZERO),
                            p.stream().map(x -> toResponse(x, names)).toList());
                })
                .sorted(Comparator.comparing(SupplierBalance::balance).reversed())
                .toList();
    }

    private BigDecimal balance(Long sourceStoreId, Long resellerStoreId) {
        BigDecimal sales = stockActivityRepository.findResellerSales(sourceStoreId, resellerStoreId, MIN_DATE, MAX_DATE)
                .stream().map(ResellerSettlementService::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = paymentRepository.findForSource(sourceStoreId, resellerStoreId).stream()
                .map(ResellerPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sales.subtract(paid).max(BigDecimal.ZERO);
    }

    /** Re-derive SETTLED / UNSETTLED for the reseller's sales of the source's products from the payments made. */
    private void syncStatuses(Long sourceStoreId, Long resellerStoreId) {
        List<StockActivity> sales =
                stockActivityRepository.findResellerSales(sourceStoreId, resellerStoreId, MIN_DATE, MAX_DATE);
        Map<Long, BigDecimal> unpaid = unpaidBySale(sales,
                paidByReseller(paymentRepository.findForSource(sourceStoreId, resellerStoreId)));
        List<StockActivity> changed = new ArrayList<>();
        for (StockActivity sale : sales) {
            SettlementStatus status = unpaid.get(sale.getId()).signum() > 0
                    ? SettlementStatus.UNSETTLED : SettlementStatus.SETTLED;
            if (sale.getSettlementStatus() != status) {
                sale.setSettlementStatus(status);
                changed.add(sale);
            }
        }
        stockActivityRepository.saveAll(changed);
    }

    private Map<Long, String> names(List<ResellerPayment> payments) {
        List<Long> ids = payments.stream().map(ResellerPayment::getRecordedBy).filter(Objects::nonNull).distinct().toList();
        return identityRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));
    }

    private static ResellerPaymentResponse toResponse(ResellerPayment p, Map<Long, String> names) {
        return new ResellerPaymentResponse(
                p.getId(),
                p.getSourceStore().getId(),
                p.getSourceStore().getStoreName(),
                p.getResellerStore().getId(),
                p.getResellerStore().getStoreName(),
                p.getAmount(),
                p.getNote(),
                names.getOrDefault(p.getRecordedBy(), ""),
                p.getPaidAt());
    }
}
