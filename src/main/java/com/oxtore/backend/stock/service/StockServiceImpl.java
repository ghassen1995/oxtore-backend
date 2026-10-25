package com.oxtore.backend.stock.service;

import com.oxtore.backend.product.DTOs.CommissionRuleRequest;
import com.oxtore.backend.product.DTOs.WholesalePriceTierRequest;
import com.oxtore.backend.product.entity.CommissionRule;
import com.oxtore.backend.product.entity.Product;
import com.oxtore.backend.product.entity.WholesalePriceTier;
import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.product.exceptions.MarketPriceNotAllowedException;
import com.oxtore.backend.product.exceptions.WholeSaleIsEmptyException;
import com.oxtore.backend.product.exceptions.WholeSaleIsNotAllowedException;
import com.oxtore.backend.product.repository.ProductRepository;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.StockActivity;
import com.oxtore.backend.stock.enums.SettlementStatus;
import com.oxtore.backend.stock.exceptions.InsufficientStockException;
import com.oxtore.backend.stock.exceptions.StockAlreadyExistsException;
import com.oxtore.backend.stock.repository.StockActivityRepository;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.store.entity.Store;
import com.oxtore.backend.store.repository.StoreRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StockActivityRepository stockActivityRepository;

    @Override
    @Transactional
    public Stock createStock(Long storeId, Long productId, Integer quantity,
                             SaleType saleType, TransactionMode transactionMode,
                             BigDecimal retailPrice, BigDecimal marketPrice,
                             CommissionRuleRequest commissionRuleRequest,
                             List<WholesalePriceTierRequest> wholesalePriceTierRequests,
                             Long sourceSupplyRequestId) {

        if (stockRepository.findByProduct_IdAndStore_Id(productId, storeId).isPresent()) {
            throw new StockAlreadyExistsException(productId, storeId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new EntityNotFoundException("Store not found: " + storeId));

       // validateStockRules(saleType, marketPrice, commissionRuleRequest, wholesalePriceTierRequests);

        Stock stock = Stock.builder()
                .product(product)
                .store(store)
                .quantity(quantity)
                .saleType(saleType)
                .transactionMode(transactionMode)
                .retailPrice(retailPrice)
                .marketPrice(marketPrice)
                .build();

        CommissionRule commissionRule = mapCommissionRule(commissionRuleRequest);
        if (commissionRule != null) {
            commissionRule.setStock(stock);
            stock.setCommissionRule(commissionRule);
        }

        List<WholesalePriceTier> tiers = mapWholesaleTiers(wholesalePriceTierRequests);
        tiers.forEach(tier -> tier.setStock(stock));
        stock.setWholesalePriceTiers(tiers);

        return stockRepository.save(stock);
    }

    private void validateStockRules(SaleType saleType, BigDecimal marketPrice,
                                    CommissionRuleRequest commissionRule,
                                    List<WholesalePriceTierRequest> wholesalePriceTiers) {

        if (saleType != SaleType.WHOLESALE && marketPrice != null) {
            throw new MarketPriceNotAllowedException();
        }
        if (saleType == SaleType.WHOLESALE && (wholesalePriceTiers == null || wholesalePriceTiers.isEmpty())) {
            throw new WholeSaleIsEmptyException();
        }
        if (saleType == SaleType.RETAIL && wholesalePriceTiers != null && !wholesalePriceTiers.isEmpty()) {
            throw new WholeSaleIsNotAllowedException();
        }
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public StockActivity recordSale(Long stockId, Integer quantitySold, Long soldBy) {
        if (quantitySold == null || quantitySold <= 0) {
            throw new IllegalArgumentException("Sale quantity must be positive");
        }
        Stock stock = stockRepository.findByIdForUpdate(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found: " + stockId));
        if (stock.getQuantity() < quantitySold) {
            throw new InsufficientStockException(stockId, stock.getQuantity(), quantitySold);
        }

        stock.setQuantity(stock.getQuantity() - quantitySold);
        stockRepository.save(stock);

        StockActivity activity = StockActivity.builder()
                .stock(stock)
                .quantitySold(quantitySold)
                .unitPrice(stock.getRetailPrice())
                .soldBy(soldBy)
                .settlementStatus(stock.getTransactionMode() == TransactionMode.CONSIGNMENT
                        ? SettlementStatus.UNSETTLED
                        : SettlementStatus.SETTLED)
                .build();
        return stockActivityRepository.save(activity);
    }

    @Override
    @Transactional
    public Stock reclaimStock(Long stockId, Integer reclaimQuantity) {
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found: " + stockId));

        if (reclaimQuantity <= 0) {
            throw new IllegalArgumentException("Reclaim quantity must be positive");
        }
        if (stock.getQuantity() < reclaimQuantity) {
            throw new InsufficientStockException(stockId, stock.getQuantity(), reclaimQuantity);
        }

        stock.setQuantity(stock.getQuantity() - reclaimQuantity);
        return stockRepository.save(stock);
    }

    @Override
    public List<Stock> getStoreStock(Long storeId) {
        return stockRepository.findByStore_Id(storeId);
    }

    private CommissionRule mapCommissionRule(CommissionRuleRequest request) {
        if (request == null) {
            return null;
        }
        return CommissionRule.builder()
                .levelOneCommission(request.levelOneCommission())
                .levelTwoCommission(request.levelTwoCommission())
                .levelThreeCommission(request.levelThreeCommission())
                .build();
    }

    private List<WholesalePriceTier> mapWholesaleTiers(List<WholesalePriceTierRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        return requests.stream()
                .map(r -> WholesalePriceTier.builder()
                        .minQuantity(r.minQuantity())
                        .unitPrice(r.unitPrice())
                        .build())
                .toList();
    }
    @Override
    public Stock getStock(Long productId, Long storeId) {
        return stockRepository.findByProduct_IdAndStore_Id(productId, storeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for product %d at store %d".formatted(productId, storeId)));
    }
}
