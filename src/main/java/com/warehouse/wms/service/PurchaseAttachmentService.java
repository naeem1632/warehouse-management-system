package com.warehouse.wms.service;

import com.warehouse.wms.dto.PurchaseAttachmentDTO;
import com.warehouse.wms.entity.Purchase;
import com.warehouse.wms.entity.PurchaseAttachment;
import com.warehouse.wms.entity.User;
import com.warehouse.wms.repository.PurchaseAttachmentRepository;
import com.warehouse.wms.repository.PurchaseRepository;
import com.warehouse.wms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseAttachmentService {

    private final PurchaseAttachmentRepository purchaseAttachmentRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads/purchases}")
    private String uploadDir;

    public List<PurchaseAttachmentDTO> getAttachmentsByPurchaseId(Long purchaseId) {
        System.out.println("=== Getting attachments for purchase: " + purchaseId);
        List<PurchaseAttachment> attachments = purchaseAttachmentRepository.findByPurchaseIdOrderByUploadedAtDesc(purchaseId);
        System.out.println("Found " + attachments.size() + " attachments in database");

        List<PurchaseAttachmentDTO> dtos = attachments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        System.out.println("Returning " + dtos.size() + " attachment DTOs");
        return dtos;
    }

    public PurchaseAttachmentDTO uploadAttachment(Long purchaseId, MultipartFile file, String attachmentType) throws IOException {
        System.out.println("=== Starting file upload for purchase: " + purchaseId);
        System.out.println("File name: " + file.getOriginalFilename());
        System.out.println("File size: " + file.getSize());
        System.out.println("Attachment type: " + attachmentType);

        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + purchaseId));
        System.out.println("Purchase found: " + purchase.getId());

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        System.out.println("Current username: " + currentUsername);

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        System.out.println("Current user found: " + currentUser.getId());

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, purchaseId.toString());
        System.out.println("Upload path: " + uploadPath.toAbsolutePath());

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        System.out.println("Generated unique filename: " + uniqueFilename);

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("File saved to disk: " + filePath.toAbsolutePath());

        // Save attachment metadata to database
        PurchaseAttachment attachment = PurchaseAttachment.builder()
                .purchase(purchase)
                .fileName(uniqueFilename)
                .originalFileName(originalFilename)
                .filePath(filePath.toString())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .attachmentType(attachmentType)
                .uploadedBy(currentUser)
                .build();

        System.out.println("Saving attachment to database...");
        PurchaseAttachment saved = purchaseAttachmentRepository.save(attachment);
        System.out.println("Attachment saved with ID: " + saved.getId());

        PurchaseAttachmentDTO dto = convertToDTO(saved);
        System.out.println("=== Upload completed successfully, returning DTO");
        return dto;
    }

    public Resource downloadAttachment(Long attachmentId) throws MalformedURLException {
        PurchaseAttachment attachment = purchaseAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        Path filePath = Paths.get(attachment.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found or not readable: " + attachment.getOriginalFileName());
        }
    }

    public void deleteAttachment(Long attachmentId) throws IOException {
        PurchaseAttachment attachment = purchaseAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));

        // Delete file from disk
        Path filePath = Paths.get(attachment.getFilePath());
        Files.deleteIfExists(filePath);

        // Delete from database
        purchaseAttachmentRepository.delete(attachment);
    }

    public String getOriginalFileName(Long attachmentId) {
        PurchaseAttachment attachment = purchaseAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));
        return attachment.getOriginalFileName();
    }

    public String getContentType(Long attachmentId) {
        PurchaseAttachment attachment = purchaseAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + attachmentId));
        return attachment.getFileType();
    }

    private PurchaseAttachmentDTO convertToDTO(PurchaseAttachment attachment) {
        return PurchaseAttachmentDTO.builder()
                .id(attachment.getId())
                .purchaseId(attachment.getPurchase().getId())
                .fileName(attachment.getFileName())
                .originalFileName(attachment.getOriginalFileName())
                .filePath(attachment.getFilePath())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .attachmentType(attachment.getAttachmentType())
                .uploadedBy(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getId() : null)
                .uploadedByName(attachment.getUploadedBy() != null ? attachment.getUploadedBy().getName() : null)
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
