package com.warehouse.wms.service;

import com.warehouse.wms.dto.WarehouseDTO;
import com.warehouse.wms.entity.AuditLog;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.AuditAction;
import com.warehouse.wms.enums.WarehouseStatus;
import com.warehouse.wms.repository.AuditLogRepository;
import com.warehouse.wms.repository.UserRepository;
import com.warehouse.wms.repository.UserWarehouseAccessRepository;
import com.warehouse.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserWarehouseAccessRepository userWarehouseAccessRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public Page<Warehouse> getAllWarehouses(Pageable pageable) {
        return warehouseRepository.findAll(pageable);
    }

    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    public List<Warehouse> getActiveWarehouses() {
        return warehouseRepository.findByStatus(WarehouseStatus.ACTIVE);
    }

    public Warehouse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with id: " + id));
    }

    public Warehouse getWarehouseByCode(String code) {
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with code: " + code));
    }

    @Transactional
    public Warehouse createWarehouse(WarehouseDTO warehouseDTO) {
        // Check if code already exists
        if (warehouseRepository.existsByCode(warehouseDTO.getCode())) {
            throw new RuntimeException("Warehouse code already exists: " + warehouseDTO.getCode());
        }

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(warehouseDTO.getCode().toUpperCase());
        warehouse.setName(warehouseDTO.getName());
        warehouse.setLocation(warehouseDTO.getLocation());
        warehouse.setCity(warehouseDTO.getCity());
        warehouse.setContactPerson(warehouseDTO.getContactPerson());
        warehouse.setPhone(warehouseDTO.getPhone());
        warehouse.setEmail(warehouseDTO.getEmail());
        warehouse.setAddress(warehouseDTO.getAddress());
        warehouse.setStatus(warehouseDTO.getStatus());
        warehouse.setCreatedBy(currentUser.getId());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        // Create audit log
        createAuditLog(savedWarehouse.getId(), "warehouses", savedWarehouse.getId(), AuditAction.INSERT, null, mapWarehouseToAudit(savedWarehouse));

        return savedWarehouse;
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, WarehouseDTO warehouseDTO) {
        Warehouse warehouse = getWarehouseById(id);

        // Store old values for audit
        Map<String, Object> oldValues = mapWarehouseToAudit(warehouse);

        // Check if code is being changed and if new code already exists
        if (!warehouse.getCode().equals(warehouseDTO.getCode().toUpperCase()) &&
                warehouseRepository.existsByCode(warehouseDTO.getCode())) {
            throw new RuntimeException("Warehouse code already exists: " + warehouseDTO.getCode());
        }

        warehouse.setCode(warehouseDTO.getCode().toUpperCase());
        warehouse.setName(warehouseDTO.getName());
        warehouse.setLocation(warehouseDTO.getLocation());
        warehouse.setCity(warehouseDTO.getCity());
        warehouse.setContactPerson(warehouseDTO.getContactPerson());
        warehouse.setPhone(warehouseDTO.getPhone());
        warehouse.setEmail(warehouseDTO.getEmail());
        warehouse.setAddress(warehouseDTO.getAddress());
        warehouse.setStatus(warehouseDTO.getStatus());

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);

        // Create audit log
        createAuditLog(updatedWarehouse.getCreatedBy(), "warehouses", updatedWarehouse.getId(), AuditAction.UPDATE, oldValues, mapWarehouseToAudit(updatedWarehouse));

        return updatedWarehouse;
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = getWarehouseById(id);

        // Store old values for audit
        Map<String, Object> oldValues = mapWarehouseToAudit(warehouse);

        // Delete warehouse access
        userWarehouseAccessRepository.deleteByWarehouseId(id);

        // Delete warehouse
        warehouseRepository.delete(warehouse);

        // Create audit log
        createAuditLog(warehouse.getCreatedBy(), "warehouses", id, AuditAction.DELETE, oldValues, null);
    }

    private void createAuditLog(Long warehouseCreatedBy, String tableName, Long recordId, AuditAction action, Map<String, Object> oldValue, Map<String, Object> newValue) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(currentUser != null ? currentUser.getId() : warehouseCreatedBy);
        auditLog.setWarehouseId(action == AuditAction.DELETE ? recordId : null);
        auditLog.setTableName(tableName);
        auditLog.setRecordId(recordId);
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);

        auditLogRepository.save(auditLog);
    }

    private Map<String, Object> mapWarehouseToAudit(Warehouse warehouse) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", warehouse.getId());
        map.put("code", warehouse.getCode());
        map.put("name", warehouse.getName());
        map.put("location", warehouse.getLocation());
        map.put("city", warehouse.getCity());
        map.put("contactPerson", warehouse.getContactPerson());
        map.put("phone", warehouse.getPhone());
        map.put("email", warehouse.getEmail());
        map.put("address", warehouse.getAddress());
        map.put("status", warehouse.getStatus().name());
        map.put("createdBy", warehouse.getCreatedBy());
        return map;
    }
}