package com.oxtore.backend.stock.service;

import com.oxtore.backend.identity.entity.User;
import com.oxtore.backend.identity.repository.IdentityRepository;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.stock.DTO.ResellerActivityResponse;
import com.oxtore.backend.stock.DTO.ResellerProductLine;
import com.oxtore.backend.stock.DTO.ResellerSaleLine;
import com.oxtore.backend.stock.DTO.ResellerSummary;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.StockActivity;
import com.oxtore.backend.stock.enums.SettlementStatus;
import com.oxtore.backend.stock.enums.SupplyRequestStatus;
import com.oxtore.backend.stock.repository.ResellerPaymentRepository;
import com.oxtore.backend.stock.repository.StockActivityRepository;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.stock.repository.SupplyRequestRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.upload.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResellerActivityServiceImpl implements ResellerActivityService {

    /** Open-ended date bounds, so the queries never bind a null timestamp. */
    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 12, 31, 0, 0);

    private final StockRepository stockRepository;
    private final StockActivityRepository stockActivityRepository;
    private final SupplyRequestRepository supplyRequestRepository;
    private final IdentityRepository identityRepository;
    private final ImageUrlResolver imageUrlResolver;
    private final ResellerPaymentRepository resellerPaymentRepository;
    private final ResellerSettlementService resellerSettlementService;

    private record Key(Long resellerStoreId, Long productId) {}

    /** Mutable accumulator for one (reseller, product) pair. */
    private static final class Line {
        Store reseller;
        Product product;
        int received;
        int sold;
        int remaining;
        /** All-time, like the balance: what is still unpaid on this product's sales. */
        BigDecimal owed = BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public ResellerActivityResponse getResellerActivity(Long sourceStoreId, Long resellerStoreId,
                                                        LocalDate from, LocalDate to) {
        LocalDateTime fromTime = from != null ? from.atStartOfDay() : MIN_DATE;
        // "to" includes the whole day.
        LocalDateTime toTime = to != null ? to.plusDays(1).atStartOfDay() : MAX_DATE;

        List<Stock> resellerStocks = stockRepository.findResellerStocks(sourceStoreId, resellerStoreId);
        // The balance is all-time: payments settle the oldest sales, whatever period is shown.
        List<StockActivity> allSales = stockActivityRepository
                .findResellerSales(sourceStoreId, resellerStoreId, MIN_DATE, MAX_DATE);
        List<StockActivity> sales = allSales.stream()
                .filter(a -> !a.getSoldAt().isBefore(fromTime) && a.getSoldAt().isBefore(toTime))
                .toList();
        Map<Long, BigDecimal> paidByReseller = ResellerSettlementService.paidByReseller(
                resellerPaymentRepository.findForSource(sourceStoreId, resellerStoreId));
        Map<Long, BigDecimal> unpaid = ResellerSettlementService.unpaidBySale(allSales, paidByReseller);
        Map<Long, BigDecimal> salesByReseller = new HashMap<>();

        Map<Key, Line> lines = new LinkedHashMap<>();
        for (Stock stock : resellerStocks) {
            Line line = lineFor(lines, stock);
            line.remaining = stock.getQuantity();
        }

        for (Object[] row : supplyRequestRepository
                .sumQuantityByResellerAndProduct(sourceStoreId, SupplyRequestStatus.CONFIRMED, resellerStoreId)) {
            Line line = lines.get(new Key((Long) row[0], (Long) row[1]));
            // A confirmed request always creates or tops up a reseller Stock row; skip orphans defensively.
            if (line != null) {
                line.received = ((Number) row[2]).intValue();
            }
        }

        Map<Long, String> sellerNames = identityRepository
                .findAllById(sales.stream().map(StockActivity::getSoldBy).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u.getFirstName() + " " + u.getLastName()));

        for (StockActivity sale : allSales) {
            Line line = lineFor(lines, sale.getStock());
            line.owed = line.owed.add(unpaid.get(sale.getId()));
            salesByReseller.merge(sale.getStock().getStore().getId(),
                    ResellerSettlementService.lineTotal(sale), BigDecimal::add);
        }

        List<ResellerSaleLine> activities = new ArrayList<>(sales.size());
        for (StockActivity sale : sales) {
            Stock stock = sale.getStock();
            BigDecimal unitPrice = sale.getUnitPrice() != null ? sale.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = ResellerSettlementService.lineTotal(sale);

            Line line = lineFor(lines, stock);
            line.sold += sale.getQuantitySold();

            activities.add(new ResellerSaleLine(
                    sale.getId(),
                    sale.getSoldAt(),
                    stock.getProduct().getId(),
                    stock.getProduct().getProductName(),
                    stock.getStore().getId(),
                    stock.getStore().getStoreName(),
                    sale.getQuantitySold(),
                    unitPrice,
                    lineTotal,
                    sellerNames.getOrDefault(sale.getSoldBy(), ""),
                    unpaid.get(sale.getId()).signum() > 0 ? SettlementStatus.UNSETTLED : SettlementStatus.SETTLED));
        }

        Map<Long, List<Line>> byReseller = lines.values().stream()
                .collect(Collectors.groupingBy(l -> l.reseller.getId(), LinkedHashMap::new, Collectors.toList()));

        List<ResellerSummary> resellers = byReseller.values().stream()
                .map(l -> toSummary(l, salesByReseller, paidByReseller))
                .sorted(Comparator.comparing(ResellerSummary::resellerStoreName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        BigDecimal totalOwed = resellers.stream().map(ResellerSummary::owedUnsettled)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = resellers.stream().map(ResellerSummary::totalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResellerActivityResponse(resellers, activities,
                resellerSettlementService.paymentsForSource(sourceStoreId, resellerStoreId), totalPaid, totalOwed);
    }

    private static Line lineFor(Map<Key, Line> lines, Stock stock) {
        Line line = lines.computeIfAbsent(
                new Key(stock.getStore().getId(), stock.getProduct().getId()), k -> new Line());
        line.reseller = stock.getStore();
        line.product = stock.getProduct();
        return line;
    }

    private ResellerSummary toSummary(List<Line> lines, Map<Long, BigDecimal> salesByReseller,
                                      Map<Long, BigDecimal> paidByReseller) {
        Store reseller = lines.get(0).reseller;
        List<ResellerProductLine> products = lines.stream()
                .sorted(Comparator.comparing((Line l) -> l.product.getProductName(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(l -> new ResellerProductLine(
                        l.product.getId(),
                        l.product.getProductName(),
                        imageUrlResolver.firstUrl(l.product),
                        l.received,
                        l.sold,
                        l.remaining,
                        l.owed))
                .toList();
        int totalSold = lines.stream().mapToInt(l -> l.sold).sum();
        BigDecimal owed = lines.stream().map(l -> l.owed).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResellerSummary(reseller.getId(), reseller.getStoreName(), products, totalSold,
                salesByReseller.getOrDefault(reseller.getId(), BigDecimal.ZERO),
                paidByReseller.getOrDefault(reseller.getId(), BigDecimal.ZERO),
                owed);
    }
}
