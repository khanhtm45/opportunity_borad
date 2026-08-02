package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.service.MediaLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaLinkService mediaLinkService;

    /** Redirect tới S3 presign — chỉ khi chữ ký HMAC còn hạn (ảnh board không public bucket). */
    @GetMapping("/view")
    public ResponseEntity<Void> view(@RequestParam String ref,
                                     @RequestParam long exp,
                                     @RequestParam String sig) {
        mediaLinkService.verifyViewSignature(ref, exp, sig);
        String presigned = mediaLinkService.presignGet(ref, java.time.Duration.ofMinutes(10));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(presigned)).build();
    }

    /** User đăng nhập lấy viewUrl mới từ ref ob-s3:// */
    @GetMapping("/access")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> access(@RequestParam String ref) {
        String viewUrl = mediaLinkService.signedViewUrl(ref, java.time.Duration.ofDays(1));
        return ResponseEntity.ok(Map.of(
                "ref", ref,
                "viewUrl", viewUrl,
                "fetchUrl", mediaLinkService.presignGet(ref, java.time.Duration.ofMinutes(15))
        ));
    }
}
