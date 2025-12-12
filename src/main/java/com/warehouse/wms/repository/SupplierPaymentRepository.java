package com.warehouse.wms.repository;

import com.warehouse.wms.entity.SupplierPayment;
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
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    Optional<SupplierPayment> findByPaymentNumber(String paymentNumber);

    @Query("SELECT sp FROM SupplierPayment sp WHERE sp.paymentNumber LIKE :prefix% ORDER BY sp.paymentNumber DESC")
    Optional<SupplierPayment> findLatestByPaymentNumberPrefix(@Param("prefix") String prefix);

    List<SupplierPayment> findBySupplierIdOrderByPaymentDateDesc(Long supplierId);

    Page<SupplierPayment> findAllByOrderByPaymentDateDesc(Pageable pageable);

    @Query("SELECT sp FROM SupplierPayment sp WHERE " +
           "(:supplierId IS NULL OR sp.supplier.id = :supplierId) AND " +
           "(:startDate IS NULL OR sp.paymentDate >= :startDate) AND " +
           "(:endDate IS NULL OR sp.paymentDate <= :endDate) " +
           "ORDER BY sp.paymentDate DESC")
    Page<SupplierPayment> findWithFilters(
        @Param("supplierId") Long supplierId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    @Query("SELECT SUM(sp.amount) FROM SupplierPayment sp WHERE sp.supplier.id = :supplierId")
    java.math.BigDecimal getTotalPaymentsBySupplier(@Param("supplierId") Long supplierId);
}
