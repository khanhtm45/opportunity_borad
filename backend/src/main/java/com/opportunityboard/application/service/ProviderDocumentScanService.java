package com.opportunityboard.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunityboard.application.dto.ai.ProviderDocScanResponse;
import com.opportunityboard.application.dto.ai.TaxCheckResult;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.common.exception.NotFoundException;
import com.opportunityboard.domain.entity.OrgDocument;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.infrastructure.ai.OpenRouterClient;
import com.opportunityboard.infrastructure.ai.OpenRouterProperties;
import com.opportunityboard.infrastructure.repository.OrgDocumentRepository;
import com.opportunityboard.infrastructure.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lớp 1: AI quét hồ sơ TỔ CHỨC (GPKD/MST/định danh) + {@link TaxCodeCheckService}.
 * Không quét hồ sơ tin đăng — xem {@link OpportunityDocumentScanService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderDocumentScanService {

    /** Confidence tối thiểu để tự áp dụng APPROVE/REJECT. */
    public static final double APPLY_THRESHOLD = 0.75;

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private final OrganizationRepository organizationRepository;
    private final OrgDocumentRepository orgDocumentRepository;
    private final OpenRouterClient openRouterClient;
    private final OpenRouterProperties openRouterProperties;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final TaxCodeCheckService taxCodeCheckService;
    private final MediaLinkService mediaLinkService;

    /** Chỉ check MST (không gọi AI). */
    public TaxCheckResult taxCheckOrg(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Tổ chức không tồn tại"));
        return taxCodeCheckService.check(org.getTaxCode());
    }

    public TaxCheckResult taxCheckByOwnerUserId(UUID ownerUserId) {
        Organization org = organizationRepository.findByOwnerUserUserId(ownerUserId).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tổ chức của user"));
        return taxCodeCheckService.check(org.getTaxCode());
    }

    public ProviderDocScanResponse scanByOwnerUserId(UUID ownerUserId, boolean apply) {
        Organization org = organizationRepository.findByOwnerUserUserId(ownerUserId).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tổ chức của user"));
        return scanOrg(org.getOrgId(), apply);
    }

    @Transactional
    public ProviderDocScanResponse scanOrg(UUID orgId, boolean apply) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new NotFoundException("Tổ chức không tồn tại"));
        List<OrgDocument> docs = orgDocumentRepository.findByOrgOrgId(orgId);
        if (docs.isEmpty()) {
            throw new BadRequestException("Tổ chức chưa có hồ sơ để quét AI");
        }

        String previous = org.getVerifiedStatus().name();
        TaxCheckResult taxCheck = taxCodeCheckService.check(org.getTaxCode());
        String prompt = buildPrompt(org, docs, taxCheck);
        // ob-s3:// → presign tạm; http giữ nguyên (Gemini đọc được URL tạm)
        List<String> mediaUrls = docs.stream()
                .map(OrgDocument::getFileUrl)
                .filter(this::looksLikeMedia)
                .map(mediaLinkService::resolveFetchableUrl)
                .limit(8)
                .collect(Collectors.toList());

        String raw = openRouterClient.chat(SYSTEM_PROMPT, OpenRouterClient.textAndImages(prompt, mediaUrls));
        ProviderDocScanResponse parsed = hardenTax(
                hardenAuthenticity(parseResponse(org, docs.size(), raw, previous, taxCheck)),
                taxCheck);

        if (!apply) {
            return withActions(parsed, "NONE", "MANUAL_REVIEW", parsed.verificationNote());
        }
        return applyVerdict(org, parsed);
    }

    /** MST FAIL/MISSING → không VERIFIED; ép REVIEW/REJECT. */
    private static ProviderDocScanResponse hardenTax(ProviderDocScanResponse p, TaxCheckResult tax) {
        if (tax == null || tax.passed()) {
            return withTax(p, tax != null ? tax : TaxCheckResult.missing());
        }
        String verdict = p.verdict();
        double confidence = p.confidence();
        List<String> risks = new ArrayList<>(p.risks() != null ? p.risks() : List.of());
        List<String> recommendations = new ArrayList<>(p.recommendations() != null ? p.recommendations() : List.of());
        risks.add("Check thuế: " + tax.message());
        recommendations.add("Cập nhật MST hợp lệ (10 hoặc 13 số) khớp GPKD");
        if ("APPROVE".equals(verdict)) {
            verdict = "MISSING".equals(tax.status()) ? "REVIEW" : "REJECT";
            confidence = Math.min(confidence, "MISSING".equals(tax.status()) ? 0.45 : 0.8);
        }
        return new ProviderDocScanResponse(
                p.orgId(), p.orgName(), verdict, confidence, p.summary(),
                p.findings(), risks, recommendations,
                p.documentCount(), p.model(), p.scannedAt(),
                p.previousVerifiedStatus(), p.appliedAction(), p.nextAction(),
                p.verificationNote(), p.forgeryRisk(), p.aiGeneratedSuspected(),
                p.authenticitySignals(), p.consistencyIssues(), tax, p.rawModelText()
        );
    }

    private static ProviderDocScanResponse withTax(ProviderDocScanResponse p, TaxCheckResult tax) {
        return new ProviderDocScanResponse(
                p.orgId(), p.orgName(), p.verdict(), p.confidence(), p.summary(),
                p.findings(), p.risks(), p.recommendations(),
                p.documentCount(), p.model(), p.scannedAt(),
                p.previousVerifiedStatus(), p.appliedAction(), p.nextAction(),
                p.verificationNote(), p.forgeryRisk(), p.aiGeneratedSuspected(),
                p.authenticitySignals(), p.consistencyIssues(), tax, p.rawModelText()
        );
    }

    /**
     * Hạ verdict nếu model tự báo dấu hiệu giả/AI-gen nhưng vẫn APPROVE nhầm.
     * VLM không phải forensic — khi nghi ngờ thì không tự VERIFIED.
     */
    private static ProviderDocScanResponse hardenAuthenticity(ProviderDocScanResponse p) {
        String forgeryRisk = normalizeRisk(p.forgeryRisk());
        boolean aiSuspected = p.aiGeneratedSuspected()
                || containsAny(p.authenticitySignals(), AI_SIGNAL_HINTS)
                || containsAny(p.risks(), AI_SIGNAL_HINTS)
                || containsAny(p.findings(), AI_SIGNAL_HINTS);
        boolean highForgery = "HIGH".equals(forgeryRisk)
                || containsAny(p.authenticitySignals(), FORGERY_HINTS)
                || containsAny(p.risks(), FORGERY_HINTS);
        boolean mediumForgery = "MEDIUM".equals(forgeryRisk)
                || !p.consistencyIssues().isEmpty();

        String verdict = p.verdict();
        double confidence = p.confidence();
        List<String> risks = new ArrayList<>(p.risks());
        List<String> recommendations = new ArrayList<>(p.recommendations());

        if (highForgery || aiSuspected) {
            if ("APPROVE".equals(verdict)) {
                verdict = highForgery ? "REJECT" : "REVIEW";
                confidence = Math.min(confidence, highForgery ? 0.85 : 0.55);
            }
            if (aiSuspected && risks.stream().noneMatch(r -> r.toLowerCase(Locale.ROOT).contains("ai"))) {
                risks.add("Nghi ngờ giấy tờ do AI/editor tạo hoặc chỉnh sửa kỹ thuật số");
            }
            if (highForgery && recommendations.stream().noneMatch(r -> r.toLowerCase(Locale.ROOT).contains("gốc"))) {
                recommendations.add("Chụp ảnh giấy tờ bản giấy (có dấu đỏ/chữ ký thật), kèm tờ ghi ngày hôm nay + tên công ty; hoặc nộp bản scan công chứng");
            }
            forgeryRisk = highForgery ? "HIGH" : (aiSuspected ? "MEDIUM" : forgeryRisk);
        } else if (mediumForgery && "APPROVE".equals(verdict)) {
            verdict = "REVIEW";
            confidence = Math.min(confidence, 0.6);
            forgeryRisk = forgeryRisk.equals("LOW") ? "MEDIUM" : forgeryRisk;
        }

        // Không đủ media thật để soi giả → không APPROVE tự động
        if ("APPROVE".equals(verdict) && p.documentCount() > 0
                && (p.authenticitySignals() == null || p.authenticitySignals().isEmpty())
                && (p.findings() == null || p.findings().isEmpty())) {
            verdict = "REVIEW";
            confidence = Math.min(confidence, 0.5);
            risks.add("Thiếu phân tích tính xác thực tài liệu — cần Admin xem tay");
        }

        return new ProviderDocScanResponse(
                p.orgId(), p.orgName(), verdict, confidence, p.summary(),
                p.findings(), risks, recommendations,
                p.documentCount(), p.model(), p.scannedAt(),
                p.previousVerifiedStatus(), p.appliedAction(), p.nextAction(),
                p.verificationNote(), forgeryRisk, aiSuspected,
                p.authenticitySignals(), p.consistencyIssues(), p.taxCheck(), p.rawModelText()
        );
    }

    private static final List<String> AI_SIGNAL_HINTS = List.of(
            "ai-generated", "ai generated", "generated by ai", "synthetic", "deepfake",
            "chatgpt", "midjourney", "dall-e", "gemini tạo", "do ai", "ai tạo",
            "ai gen", "fake document", "fabricated", "rendered", "too perfect",
            "quá sạch", "font giả", "font không đồng nhất", "artifact"
    );

    private static final List<String> FORGERY_HINTS = List.of(
            "giả mạo", "forgery", "forged", "photoshop", "edited", "chỉnh sửa",
            "dấu giả", "con dấu", "watermark giả", "mst không khớp", "sai khuôn",
            "template", "mẫu trống", "copy-paste", "in giả"
    );

    private static boolean containsAny(List<String> items, List<String> hints) {
        if (items == null || items.isEmpty()) return false;
        for (String item : items) {
            if (item == null) continue;
            String lower = item.toLowerCase(Locale.ROOT);
            for (String h : hints) {
                if (lower.contains(h)) return true;
            }
        }
        return false;
    }

    private static String normalizeRisk(String risk) {
        if (risk == null || risk.isBlank()) return "LOW";
        String r = risk.trim().toUpperCase(Locale.ROOT);
        if (r.contains("HIGH")) return "HIGH";
        if (r.contains("MED")) return "MEDIUM";
        return "LOW";
    }

    private ProviderDocScanResponse applyVerdict(Organization org, ProviderDocScanResponse parsed) {
        String verdict = parsed.verdict();
        double confidence = parsed.confidence();
        org.setAiScannedAt(Instant.now());

        if ("APPROVE".equals(verdict) && confidence >= APPLY_THRESHOLD
                && parsed.taxCheck() != null && parsed.taxCheck().passed()) {
            org.setVerifiedStatus(OrgVerified.VERIFIED);
            org.setVerifiedAt(Instant.now());
            org.setVerificationNote(null);
            organizationRepository.save(org);
            log.info("AI org-scan APPROVE org={} confidence={} tax=PASS → VERIFIED", org.getOrgId(), confidence);
            return withActions(parsed, "VERIFIED", "DONE", null);
        }

        // Sai / thiếu / không chắc → yêu cầu người đăng cập nhật lại hồ sơ + liên hệ
        String note = buildProviderUpdateNote(parsed);
        org.setVerifiedStatus(OrgVerified.NEEDS_UPDATE);
        org.setVerifiedAt(null);
        org.setVerificationNote(note);
        organizationRepository.save(org);
        notificationService.notifyOrgUpdateRequired(org, note);
        log.info("AI scan org={} verdict={} confidence={} → NEEDS_UPDATE (yêu cầu provider cập nhật)",
                org.getOrgId(), verdict, confidence);
        return withActions(parsed, "NEEDS_UPDATE", "PROVIDER_UPDATE", note);
    }

    private static String buildProviderUpdateNote(ProviderDocScanResponse parsed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hệ thống AI phát hiện hồ sơ/thông tin chưa đạt (")
                .append(parsed.verdict()).append(", độ tin cậy ")
                .append(Math.round(parsed.confidence() * 100)).append("%). ");
        if (parsed.summary() != null && !parsed.summary().isBlank()) {
            sb.append(parsed.summary().trim()).append(' ');
        }
        if (parsed.risks() != null && !parsed.risks().isEmpty()) {
            sb.append("Vấn đề: ").append(String.join("; ", parsed.risks())).append(". ");
        }
        if (parsed.taxCheck() != null && !parsed.taxCheck().passed()) {
            sb.append("Check thuế: ").append(parsed.taxCheck().message()).append(". ");
        }
        if (parsed.aiGeneratedSuspected() || "HIGH".equals(parsed.forgeryRisk()) || "MEDIUM".equals(parsed.forgeryRisk())) {
            sb.append("Có dấu hiệu nghi giấy tờ giả / chỉnh sửa / do AI tạo (forgeryRisk=")
                    .append(parsed.forgeryRisk()).append("). ");
        }
        if (parsed.recommendations() != null && !parsed.recommendations().isEmpty()) {
            sb.append("Cần làm: ").append(String.join("; ", parsed.recommendations())).append(". ");
        }
        sb.append("Vui lòng cập nhật hồ sơ tổ chức (MST, địa chỉ, giấy tờ bản giấy thật) hoặc liên hệ Admin để bổ sung, rồi chờ quét lại.");
        return sb.toString().trim();
    }

    private static ProviderDocScanResponse withActions(ProviderDocScanResponse base,
                                                       String appliedAction,
                                                       String nextAction,
                                                       String verificationNote) {
        return new ProviderDocScanResponse(
                base.orgId(), base.orgName(), base.verdict(), base.confidence(),
                base.summary(), base.findings(), base.risks(), base.recommendations(),
                base.documentCount(), base.model(), base.scannedAt(),
                base.previousVerifiedStatus(), appliedAction, nextAction,
                verificationNote, base.forgeryRisk(), base.aiGeneratedSuspected(),
                base.authenticitySignals(), base.consistencyIssues(), base.taxCheck(),
                base.rawModelText()
        );
    }

    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên viên KYB / xác minh PHÁP NHÂN tổ chức cho Opportunity Board (VN).
            Đây là lớp HỒ SƠ TỔ CHỨC (GPKD, MST, định danh) — KHÔNG đánh giá tin tuyển dụng hay thư hợp tác chương trình.
            Nhiệm vụ: (1) đọc giấy tờ pháp nhân từ ảnh/PDF, (2) đối chiếu tên/MST/địa chỉ với form,
            (3) phát hiện GIẢ MẠO / AI-gen / không khớp thuế.
            Trả lời CHỈ bằng JSON hợp lệ (không markdown), đúng schema:
            {
              "verdict": "APPROVE" | "REVIEW" | "REJECT",
              "confidence": 0.0-1.0,
              "summary": "tóm tắt ngắn tiếng Việt",
              "findings": ["nhận xét khách quan về nội dung đọc được"],
              "risks": ["rủi ro giả mạo / AI-gen / không khớp"],
              "recommendations": ["việc provider cần nộp thêm để chứng minh"],
              "forgeryRisk": "LOW" | "MEDIUM" | "HIGH",
              "aiGeneratedSuspected": true/false,
              "authenticitySignals": ["dấu hiệu visual/forensic quan sát được"],
              "consistencyIssues": ["chỗ không khớp giữa giấy tờ ↔ form org"]
            }

            Checklist giả mạo / AI-gen (ghi vào authenticitySignals khi thấy):
            - Layout/template generic, không giống mẫu GPKD/ĐKKD VN (thiếu mã số, cơ quan cấp, ngày cấp, dấu treo).
            - Chữ quá đều/sạch kiểu render AI; nhiễu nén kỳ lạ; méo viền; halo quanh chữ; font lẫn lộn; kerning lỗi.
            - Con dấu tròn/đỏ: màu phẳng không thấm giấy, không chồng lên chữ tự nhiên, hoặc dấu copy-paste lặp y hệt.
            - Chữ ký: quá mượt / vector / không có nét bút thật; hoặc chữ ký giống stock.
            - Ảnh màn hình / mockup / Canva / Word export thay vì scan/ảnh giấy thật (không có bóng, không có góc giấy, nền trắng tuyệt đối).
            - Watermark "sample/demo/draft", hoặc metadata/text kiểu lorem / placeholder.
            - MST/tên công ty/địa chỉ trên giấy ≠ form đăng ký; số MST sai định dạng (không 10 hoặc 13 chữ số).
            - URL giấy tờ trỏ blog/tin tuyển dụng/ảnh stock thay vì file hồ sơ.

            Quy tắc cứng:
            - APPROVE chỉ khi đọc được nội dung, khớp form, forgeryRisk=LOW, aiGeneratedSuspected=false, không có consistencyIssues nghiêm trọng.
            - REVIEW khi không đọc được file, thiếu bằng chứng vật lý, hoặc nghi AI-gen mức vừa (forgeryRisk=MEDIUM).
            - REJECT khi có dấu hiệu giả mạo rõ / fake AI / spam / giấy tờ không liên quan (forgeryRisk=HIGH).
            - Khi nghi giả hoặc AI-gen: yêu cầu provider chụp ảnh bản giấy thật (có dấu đỏ), kèm tờ giấy viết tay ngày hôm nay + tên công ty trong cùng khung hình.
            - Không bịa số liệu OCR; nếu không đọc được thì nói rõ và chọn REVIEW, confidence thấp hơn.
            - Không dùng APPROVE chỉ vì "trông chuyên nghiệp" — giấy AI thường trông sạch và đẹp.
            - Không đánh giá tin tuyển / thư hợp tác chương trình ở đây (lớp khác).
            """;

    private String buildPrompt(Organization org, List<OrgDocument> docs, TaxCheckResult taxCheck) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Lớp: HỒ SƠ TỔ CHỨC / THUẾ (không phải tin đăng)\n");
        sb.append("## Thông tin tổ chức (form đăng ký — dùng để đối chiếu)\n");
        sb.append("- Tên: ").append(nullToDash(org.getOrgName())).append('\n');
        sb.append("- Website: ").append(nullToDash(org.getWebsite())).append('\n');
        sb.append("- Email: ").append(nullToDash(org.getContactEmail())).append('\n');
        sb.append("- SĐT: ").append(nullToDash(org.getContactPhone())).append('\n');
        sb.append("- MST: ").append(nullToDash(org.getTaxCode())).append('\n');
        sb.append("- Check thuế máy (format+checksum): ").append(taxCheck.status())
                .append(" — ").append(taxCheck.message()).append('\n');
        sb.append("- Địa chỉ: ").append(nullToDash(org.getAddress())).append('\n');
        sb.append("- Lĩnh vực: ").append(nullToDash(org.getIndustry())).append('\n');
        sb.append("- Quy mô: ").append(org.getCompanySize() != null ? org.getCompanySize().name() : "—").append('\n');
        sb.append("- Trạng thái verify hiện tại: ").append(org.getVerifiedStatus()).append('\n');
        sb.append("- Mô tả: ").append(nullToDash(org.getDescription())).append('\n');
        sb.append("\n## Hồ sơ pháp nhân (").append(docs.size()).append(")\n");
        int i = 1;
        for (OrgDocument d : docs) {
            sb.append(i++).append(". type=").append(d.getDocType())
                    .append("; title=").append(d.getTitle())
                    .append("; url=").append(d.getFileUrl()).append('\n');
        }
        sb.append("\n## Việc cần làm\n");
        sb.append("1) OCR/đọc từng ảnh/PDF (đã gửi kèm nếu public).\n");
        sb.append("2) Đối chiếu tên/MST/địa chỉ trên giấy với form + kết quả check thuế máy.\n");
        sb.append("3) Đánh giá giả mạo / AI-gen trên giấy tờ pháp nhân.\n");
        sb.append("4) Điền forgeryRisk, aiGeneratedSuspected, authenticitySignals, consistencyIssues.\n");
        return sb.toString();
    }

    private ProviderDocScanResponse parseResponse(Organization org, int docCount, String raw,
                                                  String previous, TaxCheckResult taxCheck) {
        String json = extractJson(raw);
        try {
            JsonNode n = objectMapper.readTree(json);
            String verdict = n.path("verdict").asText("REVIEW").toUpperCase(Locale.ROOT);
            if (!List.of("APPROVE", "REVIEW", "REJECT").contains(verdict)) {
                verdict = "REVIEW";
            }
            double confidence = n.path("confidence").asDouble(0.5);
            confidence = Math.max(0, Math.min(1, confidence));
            boolean aiSuspected = n.path("aiGeneratedSuspected").asBoolean(false);
            return new ProviderDocScanResponse(
                    org.getOrgId(),
                    org.getOrgName(),
                    verdict,
                    confidence,
                    n.path("summary").asText(""),
                    readStringList(n.path("findings")),
                    readStringList(n.path("risks")),
                    readStringList(n.path("recommendations")),
                    docCount,
                    openRouterProperties.model(),
                    Instant.now(),
                    previous,
                    "NONE",
                    "MANUAL_REVIEW",
                    null,
                    normalizeRisk(n.path("forgeryRisk").asText("LOW")),
                    aiSuspected,
                    readStringList(n.path("authenticitySignals")),
                    readStringList(n.path("consistencyIssues")),
                    taxCheck,
                    raw
            );
        } catch (Exception ex) {
            log.warn("AI JSON parse fallback: {}", ex.getMessage());
            return new ProviderDocScanResponse(
                    org.getOrgId(), org.getOrgName(), "REVIEW", 0.3,
                    "AI trả lời không đúng JSON — cần Admin xem tay.",
                    List.of(raw != null && raw.length() > 500 ? raw.substring(0, 500) + "…" : String.valueOf(raw)),
                    List.of("Không parse được cấu trúc JSON từ model"),
                    List.of("Cập nhật lại hồ sơ tổ chức hoặc liên hệ Admin"),
                    docCount, openRouterProperties.model(), Instant.now(),
                    previous, "NONE", "PROVIDER_UPDATE", null,
                    "MEDIUM", false, List.of(), List.of(), taxCheck, raw
            );
        }
    }

    private static String extractJson(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) return trimmed.substring(start, end + 1);
        }
        Matcher m = JSON_BLOCK.matcher(trimmed);
        if (m.find()) return m.group();
        return trimmed;
    }

    private static List<String> readStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(x -> {
                if (!x.asText("").isBlank()) out.add(x.asText());
            });
        }
        return out;
    }

    private boolean looksLikeMedia(String url) {
        if (url == null) return false;
        if (MediaLinkService.isManagedRef(url)) return true;
        String u = url.toLowerCase(Locale.ROOT);
        return u.endsWith(".png") || u.endsWith(".jpg") || u.endsWith(".jpeg")
                || u.endsWith(".webp") || u.endsWith(".gif") || u.endsWith(".pdf")
                || u.contains(".png?") || u.contains(".jpg?") || u.contains(".jpeg?")
                || u.contains(".pdf?") || u.contains(".webp?");
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
