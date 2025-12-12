package com.warehouse.wms.service;

import com.warehouse.wms.dto.CustomerDTO;
import com.warehouse.wms.dto.CustomerLedgerDTO;
import com.warehouse.wms.entity.Customer;
import com.warehouse.wms.entity.CustomerLedger;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.enums.AuditAction;
import com.warehouse.wms.repository.AuditLogRepository;
import com.warehouse.wms.repository.CustomerLedgerRepository;
import com.warehouse.wms.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final UserService userService;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllActiveCustomers() {
        return customerRepository.findAllActiveCustomers().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return convertToDTO(customer);
    }

    @Transactional
    public CustomerDTO createCustomer(CustomerDTO dto, Long currentUserId) {
        // Generate customer code
        String customerCode = generateCustomerCode();

        Customer customer = Customer.builder()
                .code(customerCode)
                .name(dto.getName())
                .businessType(dto.getBusinessType())
                .contactPerson(dto.getContactPerson())
                .designation(dto.getDesignation())
                .phone(dto.getPhone())
                .alternatePhone(dto.getAlternatePhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .city(dto.getCity())
                .ntn(dto.getNtn())
                .strn(dto.getStrn())
                .paymentTerms(dto.getPaymentTerms())
                .creditLimit(dto.getCreditLimit())
                .openingBalance(dto.getOpeningBalance() != null ? dto.getOpeningBalance() : BigDecimal.ZERO)
                .openingBalanceType(dto.getOpeningBalanceType())
                .openingDate(dto.getOpeningDate() != null ? dto.getOpeningDate() : LocalDate.now())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .build();

        customer = customerRepository.save(customer);

        // Create opening balance entry in ledger if opening balance > 0
        if (customer.getOpeningBalance().compareTo(BigDecimal.ZERO) > 0) {
            createOpeningBalanceEntry(customer, currentUserId);
        }

        log.info("Customer created: {} - {} by user: {}", customer.getCode(), customer.getName(), currentUserId);

        return convertToDTO(customer);
    }

    @Transactional
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto, Long currentUserId) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setName(dto.getName());
        customer.setBusinessType(dto.getBusinessType());
        customer.setContactPerson(dto.getContactPerson());
        customer.setDesignation(dto.getDesignation());
        customer.setPhone(dto.getPhone());
        customer.setAlternatePhone(dto.getAlternatePhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setCity(dto.getCity());
        customer.setNtn(dto.getNtn());
        customer.setStrn(dto.getStrn());
        customer.setPaymentTerms(dto.getPaymentTerms());
        customer.setCreditLimit(dto.getCreditLimit());
        customer.setStatus(dto.getStatus());

        customer = customerRepository.save(customer);

        log.info("Customer updated: {} - {} by user: {}", customer.getCode(), customer.getName(), currentUserId);

        return convertToDTO(customer);
    }

    @Transactional
    public void deleteCustomer(Long id, Long currentUserId) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customerRepository.delete(customer);

        log.info("Customer deleted: {} - {} by user: {}", customer.getCode(), customer.getName(), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<CustomerLedgerDTO> getCustomerLedger(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return customerLedgerRepository.findByCustomerOrderByDate(customer).stream()
                .map(this::convertLedgerToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerLedgerDTO> getCustomerLedgerByDateRange(Long customerId, LocalDate startDate, LocalDate endDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return customerLedgerRepository.findByCustomerAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                customer, startDate, endDate).stream()
                .map(this::convertLedgerToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateCustomerLedger(Customer customer, BigDecimal amount, String type,
                                    String referenceType, Long referenceId, Long currentUserId) {
        // Get last balance
        CustomerLedger lastEntry = customerLedgerRepository.findLatestByCustomer(customer).orElse(null);

        BigDecimal previousBalance = BigDecimal.ZERO;
        String previousBalanceType = "DEBIT";

        if (lastEntry != null) {
            previousBalance = lastEntry.getBalance();
            previousBalanceType = lastEntry.getBalanceType();
        }

        // Calculate new balance
        BigDecimal newBalance;
        String newBalanceType;

        if ("DEBIT".equals(type)) {
            // Customer owes us (sale) - increases receivable
            if ("DEBIT".equals(previousBalanceType)) {
                newBalance = previousBalance.add(amount);
                newBalanceType = "DEBIT";
            } else {
                if (amount.compareTo(previousBalance) >= 0) {
                    newBalance = amount.subtract(previousBalance);
                    newBalanceType = "DEBIT";
                } else {
                    newBalance = previousBalance.subtract(amount);
                    newBalanceType = "CREDIT";
                }
            }
        } else {
            // CREDIT - payment received, decreases receivable
            if ("DEBIT".equals(previousBalanceType)) {
                if (amount.compareTo(previousBalance) >= 0) {
                    newBalance = amount.subtract(previousBalance);
                    newBalanceType = "CREDIT";
                } else {
                    newBalance = previousBalance.subtract(amount);
                    newBalanceType = "DEBIT";
                }
            } else {
                newBalance = previousBalance.add(amount);
                newBalanceType = "CREDIT";
            }
        }

        // Create ledger entry
        CustomerLedger ledger = CustomerLedger.builder()
                .customer(customer)
                .transactionDate(LocalDate.now())
                .description(generateLedgerDescription(referenceType, referenceId))
                .referenceType(referenceType)
                .referenceId(referenceId)
                .debit("DEBIT".equals(type) ? amount : BigDecimal.ZERO)
                .credit("CREDIT".equals(type) ? amount : BigDecimal.ZERO)
                .balance(newBalance)
                .balanceType(newBalanceType)
                .createdBy(currentUserId)
                .build();

        customerLedgerRepository.save(ledger);

        log.info("Customer ledger updated: {} - {} {} - New balance: {} {}",
                customer.getCode(), amount, type, newBalance, newBalanceType);
    }

    private void createOpeningBalanceEntry(Customer customer, Long currentUserId) {
        CustomerLedger openingEntry = CustomerLedger.builder()
                .customer(customer)
                .transactionDate(customer.getOpeningDate())
                .description("Opening Balance")
                .referenceType("opening_balance")
                .debit("DEBIT".equals(customer.getOpeningBalanceType()) ? customer.getOpeningBalance() : BigDecimal.ZERO)
                .credit("CREDIT".equals(customer.getOpeningBalanceType()) ? customer.getOpeningBalance() : BigDecimal.ZERO)
                .balance(customer.getOpeningBalance())
                .balanceType(customer.getOpeningBalanceType())
                .createdBy(currentUserId)
                .build();

        customerLedgerRepository.save(openingEntry);

        log.info("Opening balance created for customer: {} - {} {}",
                customer.getCode(), customer.getOpeningBalance(), customer.getOpeningBalanceType());
    }

    private String generateCustomerCode() {
        Long maxNumber = customerRepository.findMaxCustomerCodeNumber();
        Long nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("CUS-%04d", nextNumber);
    }

    private String generateLedgerDescription(String referenceType, Long referenceId) {
        return switch (referenceType) {
            case "sale" -> "Sale #" + referenceId;
            case "payment" -> "Payment Received #" + referenceId;
            case "opening_balance" -> "Opening Balance";
            default -> referenceType;
        };
    }

    private CustomerDTO convertToDTO(Customer customer) {
        CustomerDTO dto = CustomerDTO.builder()
                .id(customer.getId())
                .code(customer.getCode())
                .name(customer.getName())
                .businessType(customer.getBusinessType())
                .contactPerson(customer.getContactPerson())
                .designation(customer.getDesignation())
                .phone(customer.getPhone())
                .alternatePhone(customer.getAlternatePhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .city(customer.getCity())
                .ntn(customer.getNtn())
                .strn(customer.getStrn())
                .paymentTerms(customer.getPaymentTerms())
                .creditLimit(customer.getCreditLimit())
                .openingBalance(customer.getOpeningBalance())
                .openingBalanceType(customer.getOpeningBalanceType())
                .openingDate(customer.getOpeningDate())
                .status(customer.getStatus())
                .build();

        // Get current balance from latest ledger entry
        CustomerLedger latestEntry = customerLedgerRepository.findLatestByCustomer(customer).orElse(null);
        if (latestEntry != null) {
            dto.setCurrentBalance(latestEntry.getBalance());
            dto.setCurrentBalanceType(latestEntry.getBalanceType());
        } else {
            dto.setCurrentBalance(BigDecimal.ZERO);
            dto.setCurrentBalanceType("DEBIT");
        }

        return dto;
    }

    private CustomerLedgerDTO convertLedgerToDTO(CustomerLedger ledger) {
        return CustomerLedgerDTO.builder()
                .id(ledger.getId())
                .customerId(ledger.getCustomer().getId())
                .customerName(ledger.getCustomer().getName())
                .customerCode(ledger.getCustomer().getCode())
                .transactionDate(ledger.getTransactionDate())
                .description(ledger.getDescription())
                .referenceType(ledger.getReferenceType())
                .referenceId(ledger.getReferenceId())
                .debit(ledger.getDebit())
                .credit(ledger.getCredit())
                .balance(ledger.getBalance())
                .balanceType(ledger.getBalanceType())
                .createdBy(ledger.getCreatedBy())
                .createdAt(ledger.getCreatedAt())
                .build();
    }
}
