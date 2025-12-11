package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.WarehouseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);

    List<Warehouse> findByStatus(WarehouseStatus status);

    // Count related records for deletion validation
    @Query("SELECT COUNT(p) FROM Product p WHERE p.warehouse.id = :warehouseId")
    long countProductsByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(sm) FROM StockMovement sm WHERE sm.warehouse.id = :warehouseId")
    long countStockMovementsByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(p) FROM Purchase p WHERE p.warehouse.id = :warehouseId")
    long countPurchasesByWarehouseId(@Param("warehouseId") Long warehouseId);
}