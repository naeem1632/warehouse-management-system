package com.warehouse.wms.repository;

import com.warehouse.wms.entity.CustomerPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Long> {

    Optional<CustomerPayment> findByReceiptNumber(String receiptNumber);

    @Query("SELECT cp FROM CustomerPayment cp WHERE cp.receiptNumber LIKE :prefix% ORDER BY cp.receiptNumber DESC")
    Optional<CustomerPayment> findLatestByReceiptNumberPrefix(@Param("prefix") String prefix);

    List<CustomerPayment> findByCustomerIdOrderByPaymentDateDesc(Long customerId);

    Page<CustomerPayment> findAllByOrderByPaymentDateDesc(Pageable pageable);

    @Query("SELECT cp FROM CustomerPayment cp WHERE " +
           "(:customerId IS NULL OR cp.customer.id = :customerId) AND " +
           "(:startDate IS NULL OR cp.paymentDate >= :startDate) AND " +
           "(:endDate IS NULL OR cp.paymentDate <= :endDate) " +
           "ORDER BY cp.paymentDate DESC")
    Page<CustomerPayment> findWithFilters(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    @Query("SELECT SUM(cp.amount) FROM CustomerPayment cp WHERE cp.customer.id = :customerId")
    java.math.BigDecimal getTotalPaymentsByCustomer(@Param("customerId") Long customerId);
}
