package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    /**
     * Upload private + AES256 lên S3.
     * purpose: cv | image | avatar | logo | banner | org-doc | opp-doc
     * Trả {@code url=ob-s3://…} (lưu DB) + {@code viewUrl} ký tạm.
     */
    @PostMapping(value = "/api/v1/me/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "image") String purpose) {
        return ResponseEntity.ok(fileUploadService.upload(file, purpose));
    }

    /** Đăng ký NTD — upload hồ sơ org trước khi có JWT. */
    @PostMapping(value = "/api/v1/uploads/guest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadGuest(@RequestPart("file") MultipartFile file,
                                         @RequestParam(defaultValue = "org-doc") String purpose) {
        return ResponseEntity.ok(fileUploadService.uploadGuest(file, purpose));
    }
}
