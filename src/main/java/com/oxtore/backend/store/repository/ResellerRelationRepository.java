package com.oxtore.backend.store.repository;

import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResellerRelationRepository extends JpaRepository<ResellerRelation, Long> {

    Optional<ResellerRelation> findBySourceStore_IdAndResellerStore_IdAndStatus(
            Long sourceStoreId, Long resellerStoreId, ResellerRelationStatus status);

    Optional<ResellerRelation> findByIdAndSourceStore_Id(Long id, Long sourceStoreId);

    Optional<ResellerRelation> findBySourceStore_IdAndResellerStore_Id(Long sourceStoreId, Long resellerStoreId);

    List<ResellerRelation> findBySourceStore_IdAndStatus(Long sourceStoreId, ResellerRelationStatus status);

    List<ResellerRelation> findByResellerStore_IdAndStatus(Long resellerStoreId, ResellerRelationStatus status);

    List<ResellerRelation> findBySourceStore_IdInOrResellerStore_IdIn(List<Long> sourceIds, List<Long> resellerIds);

    /** Network members of a store: distinct boutiques linked to it by an accepted relation, in either direction. */
    @Query("""
            select count(distinct case when r.sourceStore.id = :storeId then r.resellerStore.id else r.sourceStore.id end)
            from ResellerRelation r
            where (r.sourceStore.id = :storeId or r.resellerStore.id = :storeId)
              and r.status = com.oxtore.backend.store.enums.ResellerRelationStatus.ACCEPTED
            """)
    long countNetworkMembers(@Param("storeId") Long storeId);

}