package com.berkan.receiptscanner.repository;

import com.berkan.receiptscanner.entity.RefreshToken;
import com.berkan.receiptscanner.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Find token by token string
    Optional<RefreshToken> findByToken(String token);

    // Delete all tokens for a specific user
    void deleteByUser(User user);

    // Find all tokens for a user
    List<RefreshToken> findByUser(User user);

    // Find all expired tokens
    List<RefreshToken> findByExpiryDateBefore(Instant date);

    // Delete all expired tokens
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :date")
    void deleteExpiredTokens(@Param("date") Instant date);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true")
    void deleteRevokedTokens();

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiryDate > :now")
    long countActiveTokensByUser(@Param("user") User user, @Param("now") Instant now);
}
