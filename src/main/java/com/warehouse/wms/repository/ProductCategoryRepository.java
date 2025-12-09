package com.warehouse.wms.repository;

import com.warehouse.wms.entity.ProductCategory;
import com.warehouse.wms.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByStatus(CategoryStatus status);

    @Query("SELECT pc FROM ProductCategory pc ORDER BY pc.name ASC")
    List<ProductCategory> findAllOrderByName();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
