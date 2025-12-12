package com.warehouse.wms.service;

import com.warehouse.wms.dto.SaleDTO;
import com.warehouse.wms.dto.SaleItemDTO;
import com.warehouse.wms.dto.StockMovementDTO;
import com.warehouse.wms.entity.*;
import com.warehouse.wms.enums.MovementType;
import com.warehouse.wms.enums.PaymentStatus;
import com.warehouse.wms.enums.SaleStatus;
import com.warehouse.wms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final StockService stockService;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public Page<SaleDTO> getAllSales(Pageable pageable) {
        return saleRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SaleDTO> getSalesByWarehouse(Long warehouseId, Pageable pageable) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        return saleRepository.findByWarehouseOrderBySaleDateDesc(warehouse, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<SaleDTO> getSalesByCustomer(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return saleRepository.findByCustomerOrderBySaleDateDesc(customer, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public SaleDTO getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));
        return convertToDTO(sale);
    }

    @Transactional
    public SaleDTO createSale(SaleDTO dto, Long currentUserId) {
        // Validate customer and warehouse
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Warehouse warehouse = warehouseRepository.findById(dto.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        User user = userService.getUserEntityById(currentUserId);

        // Check stock availability for all items BEFORE processing
        for (SaleItemDTO itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            BigDecimal availableStock = stockService.getAvailableStock(warehouse.getId(), product.getId());

            if (itemDto.getQuantity().compareTo(availableStock) > 0) {
                throw new RuntimeException(String.format(
                        "Insufficient stock for product: %s. Available: %s, Required: %s",
                        product.getName(),
                        availableStock,
                        itemDto.getQuantity()
                ));
            }
        }

        // Generate sale and invoice numbers
        String saleNumber = generateSaleNumber();
        String invoiceNumber = generateInvoiceNumber();

        // Calculate totals
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleItemDTO itemDto : dto.getItems()) {
            BigDecimal itemTotal = itemDto.getQuantity().multiply(itemDto.getRate());
            BigDecimal discountAmount = itemDto.getDiscountAmount() != null ? itemDto.getDiscountAmount() : BigDecimal.ZERO;
            itemTotal = itemTotal.subtract(discountAmount);
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal totalAmount = subtotal.subtract(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO);

        // Determine payment status
        PaymentStatus paymentStatus;
        BigDecimal receivedAmount = dto.getReceivedAmount() != null ? dto.getReceivedAmount() : BigDecimal.ZERO;

        if (receivedAmount.compareTo(BigDecimal.ZERO) == 0) {
            paymentStatus = PaymentStatus.PENDING;
        } else if (receivedAmount.compareTo(totalAmount) >= 0) {
            paymentStatus = PaymentStatus.PAID;
        } else {
            paymentStatus = PaymentStatus.PARTIAL;
        }

        // Create sale
        Sale sale = Sale.builder()
                .saleNumber(saleNumber)
                .invoiceNumber(invoiceNumber)
                .saleDate(dto.getSaleDate() != null ? dto.getSaleDate() : LocalDate.now())
                .customer(customer)
                .warehouse(warehouse)
                .subtotal(subtotal)
                .discount(dto.getDiscount() != null ? dto.getDiscount() : BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .receivedAmount(receivedAmount)
                .paymentStatus(paymentStatus)
                .paymentMethod(dto.getPaymentMethod())
                .notes(dto.getNotes())
                .status(SaleStatus.COMPLETED)
                .createdBy(user)
                .build();

        sale = saleRepository.save(sale);

        // Create sale items and update stock
        for (SaleItemDTO itemDto : dto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            SaleItem item = SaleItem.builder()
                    .sale(sale)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .rate(itemDto.getRate())
                    .discountPercent(itemDto.getDiscountPercent() != null ? itemDto.getDiscountPercent() : BigDecimal.ZERO)
                    .discountAmount(itemDto.getDiscountAmount() != null ? itemDto.getDiscountAmount() : BigDecimal.ZERO)
                    .amount(itemDto.getAmount())
                    .build();

            saleItemRepository.save(item);

            // Update stock - SALE movement (negative quantity)
            StockMovementDTO stockMovement = StockMovementDTO.builder()
                    .warehouseId(warehouse.getId())
                    .productId(product.getId())
                    .movementType(MovementType.SALE)
                    .quantity(itemDto.getQuantity().negate()) // Negative for stock OUT
                    .rate(itemDto.getRate())
                    .referenceType("sale")
                    .referenceId(sale.getId())
                    .notes("Sale #" + sale.getSaleNumber() + " - Invoice: " + sale.getInvoiceNumber())
                    .build();

            stockService.recordMovement(stockMovement, currentUserId);
        }

        // Update customer ledger (Debit entry - increases receivable)
        customerService.updateCustomerLedger(customer, totalAmount, "DEBIT",
                "sale", sale.getId(), currentUserId);

        // If payment received, create credit entry
        if (receivedAmount.compareTo(BigDecimal.ZERO) > 0) {
            customerService.updateCustomerLedger(customer, receivedAmount, "CREDIT",
                    "payment", sale.getId(), currentUserId);
        }

        log.info("Sale created: {} - Invoice: {} for warehouse: {} by user: {}",
                sale.getSaleNumber(), sale.getInvoiceNumber(), warehouse.getName(), currentUserId);

        return convertToDTO(sale);
    }

    @Transactional
    public SaleDTO updateSale(Long id, SaleDTO dto, Long currentUserId) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));

        // Only allow updating basic fields, not items
        sale.setNotes(dto.getNotes());

        sale = saleRepository.save(sale);

        log.info("Sale updated: {} by user: {}", sale.getSaleNumber(), currentUserId);
        return convertToDTO(sale);
    }

    @Transactional
    public void deleteSale(Long id, Long currentUserId) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));

        // Note: This will cascade delete sale items, stock movements, and ledger entries
        saleRepository.delete(sale);

        log.info("Sale deleted: {} by user: {}", sale.getSaleNumber(), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> getSalesByDateRange(Long warehouseId, LocalDate startDate, LocalDate endDate) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        return saleRepository.findByWarehouseAndDateRange(warehouse, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SaleDTO> getCustomerSalesByDateRange(Long customerId, LocalDate startDate, LocalDate endDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return saleRepository.findByCustomerAndDateRange(customer, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String generateSaleNumber() {
        String prefix = "SAL-";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = saleRepository.countSalesFromDate(LocalDate.now()) + 1;
        return String.format("%s%s-%04d", prefix, datePart, count);
    }

    private String generateInvoiceNumber() {
        String prefix = "INV-";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = saleRepository.countSalesFromDate(LocalDate.now()) + 1;
        return String.format("%s%s-%04d", prefix, datePart, count);
    }

    private SaleDTO convertToDTO(Sale sale) {
        List<SaleItemDTO> itemDTOs = saleItemRepository.findBySale(sale)
                .stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());

        return SaleDTO.builder()
                .id(sale.getId())
                .saleNumber(sale.getSaleNumber())
                .invoiceNumber(sale.getInvoiceNumber())
                .saleDate(sale.getSaleDate())
                .customerId(sale.getCustomer().getId())
                .customerName(sale.getCustomer().getName())
                .customerCode(sale.getCustomer().getCode())
                .warehouseId(sale.getWarehouse().getId())
                .warehouseName(sale.getWarehouse().getName())
                .subtotal(sale.getSubtotal())
                .discount(sale.getDiscount())
                .totalAmount(sale.getTotalAmount())
                .receivedAmount(sale.getReceivedAmount())
                .paymentStatus(sale.getPaymentStatus())
                .paymentMethod(sale.getPaymentMethod())
                .notes(sale.getNotes())
                .status(sale.getStatus())
                .createdBy(sale.getCreatedBy() != null ? sale.getCreatedBy().getId() : null)
                .createdByName(sale.getCreatedBy() != null ? sale.getCreatedBy().getName() : null)
                .items(itemDTOs)
                .build();
    }

    private SaleItemDTO convertItemToDTO(SaleItem item) {
        return SaleItemDTO.builder()
                .id(item.getId())
                .saleId(item.getSale().getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .productUnit(item.getProduct().getUnit().getDisplayName())
                .quantity(item.getQuantity())
                .rate(item.getRate())
                .discountPercent(item.getDiscountPercent())
                .discountAmount(item.getDiscountAmount())
                .amount(item.getAmount())
                .build();
    }
}
