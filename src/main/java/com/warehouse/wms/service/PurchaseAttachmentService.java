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
        return purchaseAttachmentRepository.findByPurchaseIdOrderByUploadedAtDesc(purchaseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PurchaseAttachmentDTO uploadAttachment(Long purchaseId, MultipartFile file, String attachmentType) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }

        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new RuntimeException("Purchase not found with id: " + purchaseId));

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, purchaseId.toString());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

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

        PurchaseAttachment saved = purchaseAttachmentRepository.save(attachment);
        return convertToDTO(saved);
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
