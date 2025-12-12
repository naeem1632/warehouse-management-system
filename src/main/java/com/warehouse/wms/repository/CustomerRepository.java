package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCode(String code);

    List<Customer> findByStatus(String status);

    List<Customer> findByStatusOrderByNameAsc(String status);

    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' ORDER BY c.name ASC")
    List<Customer> findAllActiveCustomers();

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.code, 5) AS integer)), 0) FROM Customer c WHERE c.code LIKE 'CUS-%'")
    Long findMaxCustomerCodeNumber();
}
