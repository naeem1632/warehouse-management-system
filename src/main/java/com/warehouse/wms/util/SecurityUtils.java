package com.warehouse.wms.util;

import com.warehouse.wms.entity.User;
import com.warehouse.wms.entity.Warehouse;
import com.warehouse.wms.enums.UserRole;
import com.warehouse.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SecurityUtils {

    private static UserRepository userRepository;

    @Autowired
    public SecurityUtils(UserRepository userRepository) {
        SecurityUtils.userRepository = userRepository;
    }

    /**
     * Get the current authenticated user ID
     * @return Current user ID or null
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Get the current authenticated user
     * @return Current user or null if not authenticated
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    /**
     * Get the current user's username
     * @return Current username or null
     */
    public static String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * Get the current user's assigned warehouse IDs
     * @return List of warehouse IDs, empty list if admin (has access to all)
     */
    public static List<Long> getCurrentUserWarehouseIds() {
        User user = getCurrentUser();
        if (user == null) {
            return List.of();
        }

        // Admins have access to all warehouses
        if (isAdmin()) {
            return List.of();
        }

        // Return assigned warehouse IDs
        return user.getWarehouses().stream()
                .map(Warehouse::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get the current user's assigned warehouses
     * @return Set of warehouses
     */
    public static Set<Warehouse> getCurrentUserWarehouses() {
        User user = getCurrentUser();
        if (user == null) {
            return Set.of();
        }
        return user.getWarehouses();
    }

    /**
     * Check if the current user is an admin
     * @return true if admin, false otherwise
     */
    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    /**
     * Check if the current user has access to a specific warehouse
     * @param warehouseId The warehouse ID to check
     * @return true if user has access, false otherwise
     */
    public static boolean hasAccessToWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return false;
        }

        User user = getCurrentUser();
        if (user == null) {
            return false;
        }

        // Admins have access to all warehouses
        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        // If user has no assigned warehouses, they have no access (unless admin)
        if (user.getWarehouses().isEmpty()) {
            return false;
        }

        // Check if the warehouse is in user's assigned warehouses
        return user.getWarehouses().stream()
                .anyMatch(w -> w.getId().equals(warehouseId));
    }

    /**
     * Check if the current user has access to all warehouses
     * @return true if admin or has no warehouse restrictions
     */
    public static boolean hasAccessToAllWarehouses() {
        return isAdmin();
    }

    /**
     * Validate that the current user has access to a warehouse, throw exception if not
     * @param warehouseId The warehouse ID to validate
     * @throws RuntimeException if user doesn't have access
     */
    public static void validateWarehouseAccess(Long warehouseId) {
        if (!hasAccessToWarehouse(warehouseId)) {
            throw new RuntimeException("Access denied: You don't have permission to access this warehouse");
        }
    }

    /**
     * Get warehouse IDs for filtering queries
     * Returns empty list for admins (no filtering needed)
     * Returns user's warehouse IDs for non-admins
     * @return List of warehouse IDs to filter by
     */
    public static List<Long> getWarehouseIdsForFiltering() {
        if (isAdmin()) {
            return List.of(); // Empty means no filtering (admin sees all)
        }
        return getCurrentUserWarehouseIds();
    }
}