package com.warehouse.wms.controller;

import com.warehouse.wms.dto.PurchaseAttachmentDTO;
import com.warehouse.wms.service.PurchaseAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@Controller
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseAttachmentController {

    private final PurchaseAttachmentService purchaseAttachmentService;

    @GetMapping("/{purchaseId}/attachments")
    @ResponseBody
    public ResponseEntity<List<PurchaseAttachmentDTO>> getAttachments(@PathVariable Long purchaseId) {
        List<PurchaseAttachmentDTO> attachments = purchaseAttachmentService.getAttachmentsByPurchaseId(purchaseId);
        return ResponseEntity.ok(attachments);
    }

    @PostMapping("/{purchaseId}/attachments")
    @ResponseBody
    public ResponseEntity<PurchaseAttachmentDTO> uploadAttachment(
            @PathVariable Long purchaseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("attachmentType") String attachmentType) throws IOException {

        PurchaseAttachmentDTO attachment = purchaseAttachmentService.uploadAttachment(purchaseId, file, attachmentType);
        return ResponseEntity.ok(attachment);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) throws MalformedURLException {
        Resource resource = purchaseAttachmentService.downloadAttachment(attachmentId);
        String filename = purchaseAttachmentService.getOriginalFileName(attachmentId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @ResponseBody
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) throws IOException {
        purchaseAttachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.ok().build();
    }
}
