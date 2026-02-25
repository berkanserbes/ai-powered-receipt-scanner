package com.berkan.receiptscanner.repository;

import com.berkan.receiptscanner.entity.Receipt;
import com.berkan.receiptscanner.entity.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
    List<ReceiptItem> findByReceipt(Receipt receipt);

    List<ReceiptItem> findByNameContainingIgnoreCase(String name);

    @Query("SELECT ri FROM ReceiptItem ri " +
           "WHERE ri.receipt.user.id = :userId " +
           "GROUP BY ri.id " +
           "ORDER BY COUNT(ri) DESC")
    List<ReceiptItem> findMostPurchasedItemsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(ri.totalPrice), 0) FROM ReceiptItem ri " +
           "WHERE ri.receipt.user.id = :userId " +
           "AND LOWER(ri.name) LIKE LOWER(CONCAT('%', :productName, '%'))")
    BigDecimal getTotalSpentOnProduct(@Param("userId") Long userId, @Param("productName") String productName);
}
