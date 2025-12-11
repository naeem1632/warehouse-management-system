package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.StockCurrent;
import com.warehouse.wms.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockCurrentRepository extends JpaRepository<StockCurrent, Long> {

    Optional<StockCurrent> findByWarehouseAndProduct(Warehouse warehouse, Product product);

    List<StockCurrent> findByWarehouse(Warehouse warehouse);

    List<StockCurrent> findByProduct(Product product);

    @Query("SELECT sc FROM StockCurrent sc WHERE " +
           "sc.warehouse.id = :warehouseId AND sc.product.id = :productId")
    Optional<StockCurrent> findByWarehouseIdAndProductId(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId
    );

    // Low stock feature disabled - minimum stock field removed
    // Returns empty list since there's no minimum stock threshold to compare against
    @Query("SELECT sc FROM StockCurrent sc " +
           "WHERE sc.warehouse.id = :warehouseId AND " +
           "1 = 0")
    List<StockCurrent> findLowStockItems(@Param("warehouseId") Long warehouseId);

    @Query("SELECT sc FROM StockCurrent sc " +
           "WHERE sc.warehouse.id = :warehouseId AND " +
           "sc.currentQuantity = :quantity")
    List<StockCurrent> findByWarehouseIdAndQuantity(
        @Param("warehouseId") Long warehouseId,
        @Param("quantity") BigDecimal quantity
    );

    @Query("SELECT sc FROM StockCurrent sc " +
           "WHERE sc.currentQuantity <= :threshold " +
           "ORDER BY sc.currentQuantity ASC")
    List<StockCurrent> findAllLowStock(@Param("threshold") BigDecimal threshold);
}
