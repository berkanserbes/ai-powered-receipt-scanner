package com.berkan.receiptscanner.repository;

import com.berkan.receiptscanner.entity.Receipt;
import com.berkan.receiptscanner.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Page<Receipt> findByUser(User user, Pageable pageable);

    Page<Receipt> findByUserAndTransactionDateBetween(
            User user, 
            Instant startDate, 
            Instant endDate, 
            Pageable pageable);

    Page<Receipt> findByUserAndMerchantNameContainingIgnoreCase(
            User user, 
            String merchantName, 
            Pageable pageable);
    
    long countByUser(User user);
    
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Receipt r WHERE r.user = :user")
    Double getTotalSpendingByUser(@Param("user") User user);
}
