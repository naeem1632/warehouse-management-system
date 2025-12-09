package com.warehouse.wms.dto;

import com.warehouse.wms.enums.CategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private CategoryStatus status;
}
