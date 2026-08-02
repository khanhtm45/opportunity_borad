package com.opportunityboard.application.service;

import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.common.exception.ForbiddenException;
import com.opportunityboard.infrastructure.storage.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * File S3 private + SSE. DB lưu {@code ob-s3://key}.
 * Xem qua URL ký HMAC (view) hoặc presign ngắn hạn (AI / download).
 */
@Service
@RequiredArgsConstructor
public class MediaLinkService {

    public static final String REF_PREFIX = "ob-s3://";

    private final S3Properties s3Properties;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;

    @Value("${app.jwt.secret}")
    private String mediaHmacSecret;

    @Value("${app.media.public-base-url:}")
    private String mediaPublicBaseUrl;

    public static boolean isManagedRef(String value) {
        return value != null && value.startsWith(REF_PREFIX);
    }

    public static String toRef(String key) {
        return REF_PREFIX + key;
    }

    public static String keyOf(String refOrUrl) {
        if (refOrUrl == null) return null;
        if (refOrUrl.startsWith(REF_PREFIX)) return refOrUrl.substring(REF_PREFIX.length());
        return null;
    }

    /** Link xem có chữ ký (board/public). Mặc định 7 ngày. */
    public String signedViewUrl(String stored, Duration ttl) {
        if (stored == null || stored.isBlank()) return stored;
        if (!isManagedRef(stored)) return stored;
        long exp = Instant.now().plus(ttl != null ? ttl : Duration.ofDays(7)).getEpochSecond();
        String ref = stored;
        String sig = sign(ref, exp);
        String base = mediaPublicBaseUrl != null && !mediaPublicBaseUrl.isBlank()
                ? trimSlash(mediaPublicBaseUrl)
                : "/api/v1";
        return base + "/media/view?ref=" + enc(ref) + "&exp=" + exp + "&sig=" + sig;
    }

    public void verifyViewSignature(String ref, long exp, String sig) {
        if (!isManagedRef(ref)) throw new BadRequestException("ref không hợp lệ");
        if (Instant.now().getEpochSecond() > exp) throw new ForbiddenException("Link xem đã hết hạn");
        String expect = sign(ref, exp);
        if (sig == null || !expect.equalsIgnoreCase(sig)) {
            throw new ForbiddenException("Chữ ký media không hợp lệ");
        }
    }

    /** Presign S3 GET ngắn hạn — dùng AI scan / redirect view. */
    public String presignGet(String storedOrKey, Duration ttl) {
        if (!s3Properties.isConfigured()) {
            throw new BadRequestException("S3 chưa cấu hình");
        }
        S3Presigner presigner = s3PresignerProvider.getIfAvailable();
        if (presigner == null) throw new BadRequestException("S3Presigner chưa sẵn sàng");

        String key = isManagedRef(storedOrKey) ? keyOf(storedOrKey) : storedOrKey;
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new BadRequestException("key media không hợp lệ");
        }
        Duration d = ttl != null ? ttl : Duration.ofMinutes(15);
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3Properties.bucket().trim())
                .key(key)
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(d)
                        .getObjectRequest(get)
                        .build())
                .url()
                .toString();
    }

    /** URL OpenRouter/Gemini có thể tải được (http/https hoặc ob-s3 đã resolve). */
    public static boolean isHttpOrHttpsUrl(String value) {
        if (value == null || value.isBlank()) return false;
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        return v.startsWith("http://") || v.startsWith("https://");
    }

    /**
     * Cho AI / client: ob-s3 → presign; http(s) → giữ nguyên.
     * Tên file kiểu {@code cv.pdf} không phải URL — trả nguyên (caller phải lọc).
     */
    public String resolveFetchableUrl(String stored) {
        if (stored == null || stored.isBlank()) return stored;
        if (isManagedRef(stored)) return presignGet(stored, Duration.ofMinutes(20));
        return stored;
    }

    /**
     * Chỉ trả URL thật để multimodal AI đọc file.
     * Bỏ qua filename local ({@code cv.pdf}) — tránh OpenRouter 400 Invalid URL format.
     */
    public String resolveFetchableUrlOrNull(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (isManagedRef(stored)) {
            try {
                return presignGet(stored, Duration.ofMinutes(20));
            } catch (Exception ex) {
                return null;
            }
        }
        return isHttpOrHttpsUrl(stored) ? stored.trim() : null;
    }

    public String resolveForDisplay(String stored, boolean signManaged) {
        if (stored == null || stored.isBlank()) return stored;
        if (signManaged && isManagedRef(stored)) return signedViewUrl(stored, Duration.ofDays(7));
        return stored;
    }

    private String sign(String ref, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(mediaHmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((ref + "|" + exp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC media lỗi", ex);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String trimSlash(String base) {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
