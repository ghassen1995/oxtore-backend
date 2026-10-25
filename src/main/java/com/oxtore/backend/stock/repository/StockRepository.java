package com.oxtore.backend.stock.repository;

import com.oxtore.backend.product.enums.SaleType;
import com.oxtore.backend.product.enums.TransactionMode;
import com.oxtore.backend.stock.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProduct_IdAndStore_Id(Long productId, Long storeId);

    List<Stock> findByStore_Id(Long storeId);

    List<Stock> findByStore_IdAndSaleTypeAndTransactionMode(Long storeId, SaleType saleType, TransactionMode transactionMode);

    List<Stock> findBySaleType(SaleType saleType);

    /** Items a store offers: its stock rows whose product isn't archived. */
    @Query("select count(s) from Stock s where s.store.id = :storeId and s.product.status <> com.oxtore.backend.product.enums.ProductStatus.ARCHIVED")
    long countActiveByStoreId(@Param("storeId") Long storeId);

    List<Stock> findByStore_IdAndQuantityGreaterThan(Long storeId, Integer quantity);

    /** Row-locks the stock so concurrent sales can't both pass the quantity check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Optional<Stock> findByIdForUpdate(@Param("id") Long id);

    /** Every Stock row of a store with its product, for the "My stock" report. */
    @Query("select s from Stock s join fetch s.product p where s.store.id = :storeId order by p.productName")
    List<Stock> findWithProductByStoreId(@Param("storeId") Long storeId);

    /** Stock held by other stores of products created by the source store (i.e. what resellers still have). */
    @Query("""
            select s from Stock s join fetch s.product p join fetch s.store st
            where p.storeId = :sourceStoreId and st.id <> :sourceStoreId
              and (:resellerStoreId is null or st.id = :resellerStoreId)
            """)
    List<Stock> findResellerStocks(@Param("sourceStoreId") Long sourceStoreId,
                                   @Param("resellerStoreId") Long resellerStoreId);

}
