package com.oxtore.backend.store.repository;

import com.oxtore.backend.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
        @Query(value = """
        SELECT s.* FROM stores s
        JOIN store_staff sf
        ON sf.user_id = :currentUser
        WHERE sf.store_id = s.id
        """, nativeQuery = true)
        List<Store> findStoresByCurrentUser(@Param("currentUser") Long currentUser);
}
