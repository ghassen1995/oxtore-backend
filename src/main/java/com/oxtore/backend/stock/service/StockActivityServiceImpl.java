package com.oxtore.backend.stock.service;

import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.stock.entity.Stock;
import com.oxtore.backend.stock.entity.StockActivity;
import com.oxtore.backend.stock.enums.SettlementStatus;
import com.oxtore.backend.stock.exceptions.InsufficientStockException;
import com.oxtore.backend.stock.repository.StockActivityRepository;
import com.oxtore.backend.stock.repository.StockRepository;
import com.oxtore.backend.stock.entity.StockActivity;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class StockActivityServiceImpl implements StockActivityService {

    private final StockRepository stockRepository;
    private final StockActivityRepository stockActivityRepository;

    @Override
    @Transactional
    public StockActivity recordSale(Long stockId, Integer quantitySold, Long soldBy) {
        Stock stock = stockRepository.findById(stockId)
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
                        ? SettlementStatus.UNSETTLED : SettlementStatus.SETTLED)
                .build();

        return stockActivityRepository.save(activity);
    }
}
