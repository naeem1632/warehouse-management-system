package com.warehouse.wms.repository;

import com.warehouse.wms.entity.PurchaseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseAttachmentRepository extends JpaRepository<PurchaseAttachment, Long> {

    List<PurchaseAttachment> findByPurchaseIdOrderByUploadedAtDesc(Long purchaseId);

    List<PurchaseAttachment> findByPurchaseIdAndAttachmentType(Long purchaseId, String attachmentType);
}
