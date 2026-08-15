package com.oxtore.backend.store.repository;

import com.oxtore.backend.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
        List<Store> findAllByOwnerId(Long OwnerId);
}
