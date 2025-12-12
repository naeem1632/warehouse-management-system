package com.warehouse.wms.repository;

import com.warehouse.wms.entity.StockAdjustment;
import com.warehouse.wms.enums.AdjustmentStatus;
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
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    Optional<StockAdjustment> findByAdjustmentNumber(String adjustmentNumber);

    @Query("SELECT sa FROM StockAdjustment sa WHERE sa.adjustmentNumber LIKE :prefix% ORDER BY sa.adjustmentNumber DESC")
    Optional<StockAdjustment> findLatestByAdjustmentNumberPrefix(@Param("prefix") String prefix);

    Page<StockAdjustment> findAllByOrderByAdjustmentDateDesc(Pageable pageable);

    List<StockAdjustment> findByWarehouseIdAndStatusOrderByAdjustmentDateDesc(Long warehouseId, AdjustmentStatus status);

    List<StockAdjustment> findByStatusOrderByAdjustmentDateDesc(AdjustmentStatus status);

    @Query("SELECT sa FROM StockAdjustment sa WHERE " +
           "(:warehouseId IS NULL OR sa.warehouse.id = :warehouseId) AND " +
           "(:productId IS NULL OR sa.product.id = :productId) AND " +
           "(:status IS NULL OR sa.status = :status) AND " +
           "(:startDate IS NULL OR sa.adjustmentDate >= :startDate) AND " +
           "(:endDate IS NULL OR sa.adjustmentDate <= :endDate) " +
           "ORDER BY sa.adjustmentDate DESC")
    Page<StockAdjustment> findWithFilters(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId,
        @Param("status") AdjustmentStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );

    Long countByStatus(AdjustmentStatus status);
}
