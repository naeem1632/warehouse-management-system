package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.ProductCategory;
import com.warehouse.wms.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Warehouse-specific queries
    List<Product> findByWarehouseId(Long warehouseId);

    List<Product> findByWarehouseIdAndStatus(Long warehouseId, ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.warehouse.id IN :warehouseIds ORDER BY p.name ASC")
    List<Product> findByWarehouseIdIn(@Param("warehouseIds") List<Long> warehouseIds);

    @Query("SELECT p FROM Product p WHERE p.warehouse.id IN :warehouseIds AND p.status = :status ORDER BY p.name ASC")
    List<Product> findByWarehouseIdInAndStatus(@Param("warehouseIds") List<Long> warehouseIds,
                                                 @Param("status") ProductStatus status);

    // Duplicate checking - warehouse-specific
    boolean existsByWarehouseIdAndSku(Long warehouseId, String sku);

    boolean existsByWarehouseIdAndSkuAndIdNot(Long warehouseId, String sku, Long id);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p " +
           "WHERE p.warehouse.id = :warehouseId AND LOWER(p.name) = LOWER(:name)")
    boolean existsByWarehouseIdAndNameIgnoreCase(@Param("warehouseId") Long warehouseId,
                                                  @Param("name") String name);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p " +
           "WHERE p.warehouse.id = :warehouseId AND LOWER(p.name) = LOWER(:name) AND p.id != :id")
    boolean existsByWarehouseIdAndNameIgnoreCaseAndIdNot(@Param("warehouseId") Long warehouseId,
                                                          @Param("name") String name,
                                                          @Param("id") Long id);

    // Search within warehouse
    @Query("SELECT p FROM Product p WHERE p.warehouse.id = :warehouseId AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchProductsByWarehouse(@Param("warehouseId") Long warehouseId,
                                            @Param("search") String search);

    @Query("SELECT p FROM Product p WHERE p.warehouse.id IN :warehouseIds AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> searchProductsByWarehouses(@Param("warehouseIds") List<Long> warehouseIds,
                                             @Param("search") String search);

    // Legacy methods - kept for backward compatibility but should use warehouse-specific versions
    @Deprecated
    Optional<Product> findBySku(String sku);

    @Deprecated
    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategory(ProductCategory category);

    List<Product> findByCategoryAndStatus(ProductCategory category, ProductStatus status);

    @Deprecated
    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.name ASC")
    List<Product> findActiveProductsOrderByName(@Param("status") ProductStatus status);

    @Deprecated
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Product> searchProducts(@Param("search") String search);

    @Deprecated
    boolean existsBySku(String sku);

    @Deprecated
    boolean existsBySkuAndIdNot(String sku, Long id);
}
