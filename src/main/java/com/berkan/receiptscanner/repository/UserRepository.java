package com.berkan.receiptscanner.repository;

import com.berkan.receiptscanner.entity.User;
import com.berkan.receiptscanner.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username
    Optional<User> findByUsername(String username);

    // Check if username already exists
    boolean existsByUsername(String username);

    // Find all users by role
    List<User> findByRole(Role role);

    // Count users by role
    long countByRole(Role role);

    // Find users created after a specific date
    List<User> findByCreatedAtAfter(LocalDateTime date);

    // Find users with most receipts
    @Query("SELECT r.user FROM Receipt r " +
           "GROUP BY r.user " +
           "ORDER BY COUNT(r) DESC")
    List<User> findTopUsersByReceiptCount();
}
