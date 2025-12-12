package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.entity.Supplier;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.PurchaseStatus;
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
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    Page<Purchase> findByWarehouseOrderByPurchaseDateDesc(Warehouse warehouse, Pageable pageable);

    Page<Purchase> findBySupplierOrderByPurchaseDateDesc(Supplier supplier, Pageable pageable);

    @Query("SELECT p FROM Purchase p WHERE p.warehouse = :warehouse " +
           "AND p.purchaseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.purchaseDate DESC")
    List<Purchase> findByWarehouseAndDateRange(@Param("warehouse") Warehouse warehouse,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Purchase p WHERE p.supplier = :supplier " +
           "AND p.purchaseDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.purchaseDate DESC")
    List<Purchase> findBySupplierAndDateRange(@Param("supplier") Supplier supplier,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Purchase p WHERE p.warehouse = :warehouse " +
           "AND p.status = :status ORDER BY p.purchaseDate DESC")
    Page<Purchase> findByWarehouseAndStatus(@Param("warehouse") Warehouse warehouse,
                                            @Param("status") PurchaseStatus status,
                                            Pageable pageable);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE " +
           "p.purchaseDate >= :date")
    Long countPurchasesFromDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE " +
           "p.warehouse.id IN :warehouseIds AND p.purchaseDate >= :date")
    Long countPurchasesByWarehousesAndDate(@Param("warehouseIds") List<Long> warehouseIds,
                                           @Param("date") LocalDate date);

    boolean existsByPurchaseNumber(String purchaseNumber);

    @Query("SELECT p FROM Purchase p WHERE " +
           "(:warehouseId IS NULL OR p.warehouse.id = :warehouseId) AND " +
           "(:supplierId IS NULL OR p.supplier.id = :supplierId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(CAST(:startDate AS date) IS NULL OR p.purchaseDate >= :startDate) AND " +
           "(CAST(:endDate AS date) IS NULL OR p.purchaseDate <= :endDate) " +
           "ORDER BY p.purchaseDate DESC")
    List<Purchase> findWithFilters(@Param("warehouseId") Long warehouseId,
                                   @Param("supplierId") Long supplierId,
                                   @Param("status") PurchaseStatus status,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);
}