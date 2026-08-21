package com.oxtore.backend.stock.repository;

import com.oxtore.backend.stock.entity.SupplyRequest;
import com.oxtore.backend.stock.enums.SupplyRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRequestRepository extends JpaRepository<SupplyRequest, Long> {

    List<SupplyRequest> findBySourceStore_IdAndStatus(Long sourceStoreId, SupplyRequestStatus status);

    List<SupplyRequest> findByResellerStore_IdOrderByRequestedAtDesc(Long resellerStoreId);

    Optional<SupplyRequest> findByIdAndSourceStore_Id(Long id, Long sourceStoreId);

    Optional<SupplyRequest> findByIdAndResellerStore_Id(Long requestId, Long resellerStoreId);
}
