package com.oxtore.backend.stock.repository;

import com.oxtore.backend.stock.entity.StockActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockActivityRepository extends JpaRepository<StockActivity, Long> {
}
