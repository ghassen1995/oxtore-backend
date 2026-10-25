package com.oxtore.backend.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    long countByProductId(Long productId);

    @Modifying
    @Query("delete from ProductLike l where l.productId = :productId and l.userId = :userId")
    int deleteLike(@Param("productId") Long productId, @Param("userId") Long userId);

    /** [productId, likeCount] for the given products (products without likes are absent). */
    @Query("select l.productId, count(l) from ProductLike l where l.productId in :productIds group by l.productId")
    List<Object[]> countByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("select l.productId from ProductLike l where l.userId = :userId and l.productId in :productIds")
    List<Long> likedAmong(@Param("userId") Long userId, @Param("productIds") Collection<Long> productIds);
}
