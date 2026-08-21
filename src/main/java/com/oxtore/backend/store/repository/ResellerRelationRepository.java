package com.oxtore.backend.store.repository;

import com.oxtore.backend.store.entity.ResellerRelation;
import com.oxtore.backend.store.enums.ResellerRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}