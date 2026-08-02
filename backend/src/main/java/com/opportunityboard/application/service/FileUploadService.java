package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.upload.UploadResponse;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.infrastructure.storage.S3Properties;
import com.opportunityboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private static final Set<String> CV_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final Set<String> DOC_TYPES = Set.of(
            "application/pdf",
            "image/jpeg", "image/png", "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final S3Properties s3Properties;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final CurrentUser currentUser;
    private final MediaLinkService mediaLinkService;

    public UploadResponse upload(MultipartFile file, String purpose) {
        User user = currentUser.get();
        return uploadInternal(file, purpose, user.getUserId().toString());
    }

    /** Upload khi đăng ký (chưa có JWT) — chỉ org-doc. */
    public UploadResponse uploadGuest(MultipartFile file, String purpose) {
        String p = normalizePurpose(purpose);
        if (!"org-doc".equals(p)) {
            throw new BadRequestException("Guest upload chỉ cho hồ sơ tổ chức (purpose=org-doc)");
        }
        return uploadInternal(file, p, "guest");
    }

    private UploadResponse uploadInternal(MultipartFile file, String purpose, String ownerSegment) {
        if (!s3Properties.isConfigured()) {
            throw new BadRequestException("Chưa cấu hình S3 (UPLOAD_STORAGE=s3 + bucket/keys trong backend/.env)");
        }
        S3Client s3 = s3ClientProvider.getIfAvailable();
        if (s3 == null) {
            throw new BadRequestException("S3Client chưa sẵn sàng — kiểm tra UPLOAD_STORAGE=s3");
        }
        if (file == null || file.isEmpty()) throw new BadRequestException("File trống");
        if (file.getSize() > MAX_BYTES) throw new BadRequestException("File tối đa 10MB");

        String purposeNorm = normalizePurpose(purpose);
        String contentType = detectContentType(file);
        validateType(purposeNorm, contentType, file.getOriginalFilename());

        String ext = extensionOf(file.getOriginalFilename(), contentType);
        String key = "uploads/" + purposeNorm + "/" + ownerSegment + "/"
                + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);

        try {
            byte[] bytes = file.getBytes();
            // Private object + AES256 server-side encryption — không public-read
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket().trim())
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) bytes.length)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();
            s3.putObject(put, RequestBody.fromBytes(bytes));

            String ref = MediaLinkService.toRef(key);
            String viewUrl = mediaLinkService.signedViewUrl(ref, Duration.ofDays(7));
            log.info("S3 private+SSE upload ok purpose={} key={}", purposeNorm, key);
            return new UploadResponse(ref, key, viewUrl, contentType, bytes.length, purposeNorm, true);
        } catch (IOException ex) {
            throw new BadRequestException("Không đọc được file upload: " + ex.getMessage());
        } catch (S3Exception ex) {
            log.error("S3 putObject failed: {}", ex.awsErrorDetails());
            throw new BadRequestException("Upload S3 thất bại: " + ex.awsErrorDetails().errorMessage());
        }
    }

    private static String normalizePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) return "misc";
        String p = purpose.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "cv", "resume" -> "cv";
            case "org-doc", "org_doc", "org" -> "org-doc";
            case "opp-doc", "opp_doc", "opp" -> "opp-doc";
            case "image", "logo", "banner", "avatar" -> "image";
            default -> p.replaceAll("[^a-z0-9-]", "-");
        };
    }

    private static void validateType(String purpose, String contentType, String filename) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        boolean ok = switch (purpose) {
            case "cv" -> CV_TYPES.contains(ct) || looksLike(filename, ".pdf", ".doc", ".docx");
            case "image" -> IMAGE_TYPES.contains(ct) || looksLike(filename, ".jpg", ".jpeg", ".png", ".webp", ".gif");
            case "org-doc", "opp-doc" -> DOC_TYPES.contains(ct)
                    || looksLike(filename, ".pdf", ".jpg", ".jpeg", ".png", ".webp", ".doc", ".docx");
            default -> CV_TYPES.contains(ct) || IMAGE_TYPES.contains(ct) || DOC_TYPES.contains(ct);
        };
        if (!ok) {
            throw new BadRequestException("Loại file không hỗ trợ cho purpose=" + purpose);
        }
    }

    private static String detectContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && !ct.isBlank() && !"application/octet-stream".equalsIgnoreCase(ct)) {
            return ct;
        }
        String name = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase(Locale.ROOT) : "";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".doc")) return "application/msword";
        if (name.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return "application/octet-stream";
    }

    private static String extensionOf(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (ext.matches("[a-z0-9]{1,8}")) return ext;
        }
        return switch (contentType == null ? "" : contentType) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            default -> "";
        };
    }

    private static boolean looksLike(String filename, String... exts) {
        if (filename == null) return false;
        String n = filename.toLowerCase(Locale.ROOT);
        for (String e : exts) if (n.endsWith(e)) return true;
        return false;
    }
}
