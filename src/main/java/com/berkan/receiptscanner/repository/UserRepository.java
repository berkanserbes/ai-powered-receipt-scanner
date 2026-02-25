package com.berkan.receiptscanner.repository;

import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    List<User> findByCreatedAtAfter(Instant date);

    @Query("SELECT r.user FROM Receipt r " +
           "GROUP BY r.user " +
           "ORDER BY COUNT(r) DESC")
    List<User> findTopUsersByReceiptCount();
}
