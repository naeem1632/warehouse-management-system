package com.warehouse.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseAttachmentDTO {
    private Long id;
    private Long purchaseId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String attachmentType; // GATE_PASS, INVOICE, QUALITY_CERT, DELIVERY_CHALLAN, OTHER
    private Long uploadedBy;
    private String uploadedByName;
    private LocalDateTime uploadedAt;
}
