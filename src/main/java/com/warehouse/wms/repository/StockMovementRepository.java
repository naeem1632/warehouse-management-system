package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.StockMovement;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByWarehouseOrderByCreatedAtDesc(Warehouse warehouse);

    List<StockMovement> findByProductOrderByCreatedAtDesc(Product product);

    List<StockMovement> findByWarehouseAndProductOrderByCreatedAtDesc(Warehouse warehouse, Product product);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "sm.warehouse.id = :warehouseId AND " +
           "sm.product.id = :productId AND " +
           "sm.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findMovementsByDateRange(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "sm.referenceType = :refType AND sm.referenceId = :refId " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findByReference(
        @Param("refType") String referenceType,
        @Param("refId") Long referenceId
    );

    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "(:warehouseId IS NULL OR sm.warehouse.id = :warehouseId) AND " +
           "(:productId IS NULL OR sm.product.id = :productId) AND " +
           "(:movementType IS NULL OR sm.movementType = :movementType) AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR sm.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR sm.createdAt <= :endDate) " +
           "ORDER BY sm.createdAt DESC")
    Page<StockMovement> findWithFilters(@Param("warehouseId") Long warehouseId,
                                       @Param("productId") Long productId,
                                       @Param("movementType") MovementType movementType,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE " +
           "sm.warehouse.id = :warehouseId AND " +
           "sm.product.id = :productId AND " +
           "(CAST(:startDate AS timestamp) IS NULL OR sm.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS timestamp) IS NULL OR sm.createdAt <= :endDate) " +
           "ORDER BY sm.createdAt DESC")
    List<StockMovement> findByWarehouseAndProductWithDateFilter(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
