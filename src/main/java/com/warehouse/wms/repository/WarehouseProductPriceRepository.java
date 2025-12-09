package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.entity.WarehouseProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseProductPriceRepository extends JpaRepository<WarehouseProductPrice, Long> {

    Optional<WarehouseProductPrice> findByWarehouseAndProduct(Warehouse warehouse, Product product);

    List<WarehouseProductPrice> findByWarehouse(Warehouse warehouse);

    List<WarehouseProductPrice> findByProduct(Product product);

    @Query("SELECT wpp FROM WarehouseProductPrice wpp " +
           "WHERE wpp.warehouse.id = :warehouseId AND wpp.product.id = :productId")
    Optional<WarehouseProductPrice> findByWarehouseIdAndProductId(
        @Param("warehouseId") Long warehouseId,
        @Param("productId") Long productId
    );

    void deleteByWarehouseId(Long warehouseId);

    void deleteByProductId(Long productId);
}
