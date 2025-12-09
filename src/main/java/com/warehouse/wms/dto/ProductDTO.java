package com.warehouse.wms.dto;

import com.warehouse.wms.enums.ProductStatus;
import com.warehouse.wms.enums.ProductUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private String sku;
    private String name;
    private Long categoryId;
    private String categoryName;
    private ProductUnit unit;
    private String description;
    private BigDecimal minimumStock;
    private ProductStatus status;
}
