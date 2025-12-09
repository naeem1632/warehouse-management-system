package com.warehouse.wms.repository;

import com.warehouse.wms.entity.UserWarehouseAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWarehouseAccessRepository extends JpaRepository<UserWarehouseAccess, Long> {

    List<UserWarehouseAccess> findByUserId(Long userId);

    List<UserWarehouseAccess> findByWarehouseId(Long warehouseId);

    boolean existsByUserIdAndWarehouseId(Long userId, Long warehouseId);

    void deleteByUserId(Long userId);

    void deleteByWarehouseId(Long warehouseId);

    void deleteByUserIdAndWarehouseId(Long userId, Long warehouseId);
}