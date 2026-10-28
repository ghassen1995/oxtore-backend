package com.oxtore.backend.order.repository;

import com.oxtore.backend.order.entity.CustomerOrder;
import com.oxtore.backend.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<CustomerOrder> findByStore_IdOrderByCreatedAtDesc(Long storeId);

    List<CustomerOrder> findByStore_IdAndStatusOrderByCreatedAtDesc(Long storeId, OrderStatus status);

    boolean existsByReference(String reference);
}
