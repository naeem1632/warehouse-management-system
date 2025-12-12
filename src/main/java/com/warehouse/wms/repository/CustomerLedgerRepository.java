package com.warehouse.wms.repository;

import com.warehouse.wms.entity.Customer;
import com.warehouse.wms.entity.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {

    List<CustomerLedger> findByCustomerOrderByTransactionDateDescCreatedAtDesc(Customer customer);

    List<CustomerLedger> findByCustomerAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Customer customer, LocalDate startDate, LocalDate endDate);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.customer = :customer " +
           "ORDER BY cl.transactionDate DESC, cl.createdAt DESC")
    List<CustomerLedger> findByCustomerOrderByDate(@Param("customer") Customer customer);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.customer = :customer " +
           "ORDER BY cl.transactionDate DESC, cl.createdAt DESC LIMIT 1")
    Optional<CustomerLedger> findLatestByCustomer(@Param("customer") Customer customer);
}
