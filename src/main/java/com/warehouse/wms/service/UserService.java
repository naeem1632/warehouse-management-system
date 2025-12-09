package com.warehouse.wms.service;

import com.warehouse.wms.dto.UserDTO;
import com.warehouse.wms.entity.AuditLog;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.enums.AuditAction;
import com.warehouse.wms.repository.AuditLogRepository;
import com.warehouse.wms.repository.UserRepository;
import com.warehouse.wms.repository.UserWarehouseAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserWarehouseAccessRepository userWarehouseAccessRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public User getUserEntityById(Long id) {
        return getUserById(id);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional
    public User createUser(UserDTO userDTO) {
        // Check if email already exists
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists: " + userDTO.getEmail());
        }

        User user = new User();
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());
        user.setStatus(userDTO.getStatus());
        user.setPhone(userDTO.getPhone());

        User savedUser = userRepository.save(user);

        // Create audit log
        createAuditLog(savedUser.getId(), "users", savedUser.getId(), AuditAction.INSERT, null, mapUserToAudit(savedUser));

        return savedUser;
    }

    @Transactional
    public User updateUser(Long id, UserDTO userDTO) {
        User user = getUserById(id);

        // Store old values for audit
        Map<String, Object> oldValues = mapUserToAudit(user);

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equals(userDTO.getEmail()) && userRepository.existsByEmail(userDTO.getEmail())) {
            throw new RuntimeException("Email already exists: " + userDTO.getEmail());
        }

        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setStatus(userDTO.getStatus());
        user.setPhone(userDTO.getPhone());

        // Only update password if provided
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);

        // Create audit log
        createAuditLog(updatedUser.getId(), "users", updatedUser.getId(), AuditAction.UPDATE, oldValues, mapUserToAudit(updatedUser));

        return updatedUser;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);

        // Store old values for audit
        Map<String, Object> oldValues = mapUserToAudit(user);

        // Delete user warehouse access
        userWarehouseAccessRepository.deleteByUserId(id);

        // Delete user
        userRepository.delete(user);

        // Create audit log
        createAuditLog(user.getId(), "users", id, AuditAction.DELETE, oldValues, null);
    }

    private void createAuditLog(Long userId, String tableName, Long recordId, AuditAction action, Map<String, Object> oldValue, Map<String, Object> newValue) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);

        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(currentUser != null ? currentUser.getId() : userId);
        auditLog.setTableName(tableName);
        auditLog.setRecordId(recordId);
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);

        auditLogRepository.save(auditLog);
    }

    private Map<String, Object> mapUserToAudit(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("role", user.getRole().name());
        map.put("status", user.getStatus().name());
        map.put("phone", user.getPhone());
        return map;
    }
}