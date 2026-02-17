package com.berkan.receiptscanner.repository;

import com.berkan.receiptscanner.entity.Receipt;
import com.berkan.receiptscanner.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Page<Receipt> findByUser(User user, Pageable pageable);

    Page<Receipt> findByUserAndTransactionDateBetween(
            User user, 
            LocalDate startDate, 
            LocalDate endDate, 
            Pageable pageable);

    Page<Receipt> findByUserAndMerchantNameContainingIgnoreCase(
            User user, 
            String merchantName, 
            Pageable pageable);
    
    //Count total receipts for a user
    long countByUser(User user);
    
    //Get total spending for a user 
    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Receipt r WHERE r.user = :user")
    Double getTotalSpendingByUser(@Param("user") User user);

    // Get receipts by user ordered by transaction date descending
    List<Receipt> findTop10ByUserOrderByTransactionDateDesc(User user);
}
