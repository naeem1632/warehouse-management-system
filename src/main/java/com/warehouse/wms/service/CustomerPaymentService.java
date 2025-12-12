package com.warehouse.wms.service;

import com.warehouse.wms.dto.CustomerPaymentDTO;
import com.warehouse.wms.entity.Customer;
import com.warehouse.wms.entity.CustomerPayment;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.repository.CustomerPaymentRepository;
import com.warehouse.wms.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentService {

    private final CustomerPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final UserService userService;

    @Transactional
    public CustomerPaymentDTO createPayment(CustomerPaymentDTO dto, Long currentUserId) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        User user = userService.getUserEntityById(currentUserId);

        // Generate receipt number
        String receiptNumber = generateReceiptNumber();

        CustomerPayment payment = CustomerPayment.builder()
                .receiptNumber(receiptNumber)
                .paymentDate(dto.getPaymentDate())
                .customer(customer)
                .amount(dto.getAmount())
                .paymentMethod(dto.getPaymentMethod())
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber())
                .transactionReference(dto.getTransactionReference())
                .chequeNumber(dto.getChequeNumber())
                .chequeDate(dto.getChequeDate())
                .notes(dto.getNotes())
                .receivedBy(user)
                .build();

        payment = paymentRepository.save(payment);

        // Update customer ledger - CREDIT entry (payment received reduces receivable)
        customerService.updateCustomerLedger(customer, dto.getAmount(), "CREDIT",
                "payment", payment.getId(), currentUserId);

        log.info("Customer payment created: {} for customer: {} by user: {}",
                receiptNumber, customer.getCode(), currentUserId);

        return convertToDTO(payment);
    }

    @Transactional(readOnly = true)
    public CustomerPaymentDTO getPaymentById(Long id) {
        CustomerPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return convertToDTO(payment);
    }

    @Transactional(readOnly = true)
    public Page<CustomerPaymentDTO> getAllPayments(Pageable pageable) {
        return paymentRepository.findAllByOrderByPaymentDateDesc(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<CustomerPaymentDTO> getPaymentsWithFilters(Long customerId, LocalDate startDate,
                                                           LocalDate endDate, Pageable pageable) {
        return paymentRepository.findWithFilters(customerId, startDate, endDate, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<CustomerPaymentDTO> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByPaymentDateDesc(customerId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String generateReceiptNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "RCP-" + today + "-";

        CustomerPayment latestPayment = paymentRepository.findLatestByReceiptNumberPrefix(prefix)
                .orElse(null);

        int nextSequence = 1;
        if (latestPayment != null) {
            String lastNumber = latestPayment.getReceiptNumber();
            String sequencePart = lastNumber.substring(lastNumber.lastIndexOf('-') + 1);
            nextSequence = Integer.parseInt(sequencePart) + 1;
        }

        return prefix + String.format("%04d", nextSequence);
    }

    private CustomerPaymentDTO convertToDTO(CustomerPayment payment) {
        return CustomerPaymentDTO.builder()
                .id(payment.getId())
                .receiptNumber(payment.getReceiptNumber())
                .paymentDate(payment.getPaymentDate())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getName())
                .customerCode(payment.getCustomer().getCode())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .bankName(payment.getBankName())
                .accountNumber(payment.getAccountNumber())
                .transactionReference(payment.getTransactionReference())
                .chequeNumber(payment.getChequeNumber())
                .chequeDate(payment.getChequeDate())
                .notes(payment.getNotes())
                .receivedBy(payment.getReceivedBy() != null ? payment.getReceivedBy().getId() : null)
                .receivedByName(payment.getReceivedBy() != null ? payment.getReceivedBy().getName() : null)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
