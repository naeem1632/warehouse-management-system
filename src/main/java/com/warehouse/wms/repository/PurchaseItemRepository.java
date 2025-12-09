package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Product;
import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

    List<PurchaseItem> findByPurchase(Purchase purchase);

    List<PurchaseItem> findByProduct(Product product);

    @Query("SELECT pi FROM PurchaseItem pi WHERE pi.product = :product " +
           "AND pi.purchase.purchaseDate BETWEEN :startDate AND :endDate")
    List<PurchaseItem> findByProductAndDateRange(@Param("product") Product product,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}