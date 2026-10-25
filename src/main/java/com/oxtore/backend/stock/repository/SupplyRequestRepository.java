package com.oxtore.backend.stock.repository;

import com.oxtore.backend.stock.entity.SupplyRequest;
import com.oxtore.backend.stock.enums.SupplyRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRequestRepository extends JpaRepository<SupplyRequest, Long> {

    List<SupplyRequest> findBySourceStore_IdAndStatus(Long sourceStoreId, SupplyRequestStatus status);

    List<SupplyRequest> findByResellerStore_IdOrderByRequestedAtDesc(Long resellerStoreId);

    Optional<SupplyRequest> findByIdAndSourceStore_Id(Long id, Long sourceStoreId);

    Optional<SupplyRequest> findByIdAndResellerStore_Id(Long requestId, Long resellerStoreId);

    List<SupplyRequest> findBySourceStore_IdInOrResellerStore_IdIn(List<Long> sourceIds, List<Long> resellerIds);

    /** Rows of [resellerStoreId, productId, total quantity] for requests of the given status, per reseller and product. */
    @Query("""
            select r.resellerStore.id, r.product.id, sum(r.requestedQuantity) from SupplyRequest r
            where r.sourceStore.id = :sourceStoreId and r.status = :status
              and (:resellerStoreId is null or r.resellerStore.id = :resellerStoreId)
            group by r.resellerStore.id, r.product.id
            """)
    List<Object[]> sumQuantityByResellerAndProduct(@Param("sourceStoreId") Long sourceStoreId,
                                                   @Param("status") SupplyRequestStatus status,
                                                   @Param("resellerStoreId") Long resellerStoreId);
}
