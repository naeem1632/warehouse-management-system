package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Customer;
import com.warehouse.wms.entity.Sale;
import com.warehouse.wms.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    Page<Sale> findByWarehouseOrderBySaleDateDesc(Warehouse warehouse, Pageable pageable);

    Page<Sale> findByCustomerOrderBySaleDateDesc(Customer customer, Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE s.warehouse = :warehouse " +
           "AND s.saleDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.saleDate DESC")
    List<Sale> findByWarehouseAndDateRange(@Param("warehouse") Warehouse warehouse,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Sale s WHERE s.customer = :customer " +
           "AND s.saleDate BETWEEN :startDate AND :endDate " +
           "ORDER BY s.saleDate DESC")
    List<Sale> findByCustomerAndDateRange(@Param("customer") Customer customer,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate = :date")
    Long countSalesFromDate(@Param("date") LocalDate date);
}
