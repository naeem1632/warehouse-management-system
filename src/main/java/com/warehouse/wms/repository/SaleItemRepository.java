package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Sale;
import com.warehouse.wms.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    List<SaleItem> findBySale(Sale sale);

    List<SaleItem> findBySaleOrderByIdAsc(Sale sale);
}
