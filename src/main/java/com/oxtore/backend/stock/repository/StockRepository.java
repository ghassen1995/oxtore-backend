package com.oxtore.backend.stock.repository;

import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProduct_IdAndStore_Id(Long productId, Long storeId);

    List<Stock> findByStore_Id(Long storeId);

    List<Stock> findByStore_IdAndSaleTypeAndTransactionMode(Long storeId, SaleType saleType, TransactionMode transactionMode);
}
