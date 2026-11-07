package com.oxtore.backend.store.repository;

import com.oxtore.backend.store.entity.StoreFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreFollowRepository extends JpaRepository<StoreFollow, Long> {

    boolean existsByStore_IdAndUserId(Long storeId, Long userId);

    @Query("select f from StoreFollow f join fetch f.store where f.userId = :userId order by f.createdAt desc")
    List<StoreFollow> findByUserIdWithStore(@Param("userId") Long userId);

    @Query("select f.store.id from StoreFollow f where f.userId = :userId")
    List<Long> followedStoreIds(@Param("userId") Long userId);

    @Modifying
    @Query("delete from StoreFollow f where f.store.id = :storeId and f.userId = :userId")
    int deleteFollow(@Param("storeId") Long storeId, @Param("userId") Long userId);
}
