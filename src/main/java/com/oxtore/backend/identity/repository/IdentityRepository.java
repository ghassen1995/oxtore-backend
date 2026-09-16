package com.oxtore.backend.identity.repository;

import com.oxtore.backend.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByPublicId(String publicId);
    Optional<User> findByPublicId(String publicId);
}
