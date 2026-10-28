package com.oxtore.backend.stock.repository;

import com.oxtore.backend.stock.entity.ResellerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResellerPaymentRepository extends JpaRepository<ResellerPayment, Long> {

    /** Payments received by the source store, newest first; resellerStoreId null = all resellers. */
    @Query("""
            select p from ResellerPayment p join fetch p.sourceStore join fetch p.resellerStore
            where p.sourceStore.id = :sourceStoreId
              and (:resellerStoreId is null or p.resellerStore.id = :resellerStoreId)
            order by p.paidAt desc
            """)
    List<ResellerPayment> findForSource(@Param("sourceStoreId") Long sourceStoreId,
                                        @Param("resellerStoreId") Long resellerStoreId);

    /** Payments made by the reseller store to any source, newest first. */
    @Query("""
            select p from ResellerPayment p join fetch p.sourceStore join fetch p.resellerStore
            where p.resellerStore.id = :resellerStoreId
            order by p.paidAt desc
            """)
    List<ResellerPayment> findForReseller(@Param("resellerStoreId") Long resellerStoreId);

    Optional<ResellerPayment> findByIdAndSourceStore_Id(Long id, Long sourceStoreId);
}
