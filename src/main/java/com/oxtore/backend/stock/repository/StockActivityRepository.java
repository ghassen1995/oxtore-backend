package com.oxtore.backend.stock.repository;

import com.oxtore.backend.stock.entity.StockActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockActivityRepository extends JpaRepository<StockActivity, Long> {
    List<StockActivity>  findByStock_Store_Id(Long boutiqueId);

    List<StockActivity> findByStock_Store_IdOrderBySoldAtDesc(Long boutiqueId);

    /** Turnover of a store: total of its recorded sales (quantity × unit price), 0 when none. */
    @Query("""
            select coalesce(sum(a.quantitySold * a.unitPrice), 0) from StockActivity a
            where a.stock.store.id = :storeId and a.quantitySold is not null and a.unitPrice is not null
            """)
    java.math.BigDecimal sumSalesByStoreId(@Param("storeId") Long storeId);

    /** Sales made by other stores of products created by the source store, newest first. */
    @Query("""
            select a from StockActivity a join fetch a.stock s join fetch s.product p join fetch s.store st
            where p.storeId = :sourceStoreId and st.id <> :sourceStoreId
              and (:resellerStoreId is null or st.id = :resellerStoreId)
              and a.soldAt >= :from and a.soldAt < :to
            order by a.soldAt desc
            """)
    List<StockActivity> findResellerSales(@Param("sourceStoreId") Long sourceStoreId,
                                          @Param("resellerStoreId") Long resellerStoreId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
