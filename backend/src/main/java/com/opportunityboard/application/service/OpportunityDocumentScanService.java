package com.opportunityboard.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunityboard.application.dto.ai.OpportunityDocScanResponse;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.common.exception.NotFoundException;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.OpportunityDocument;
import com.opportunityboard.domain.entity.Organization;
import com.opportunityboard.domain.enums.OrgVerified;
import com.opportunityboard.infrastructure.ai.OpenRouterClient;
import com.opportunityboard.infrastructure.ai.OpenRouterProperties;
import com.opportunityboard.infrastructure.repository.OpportunityDocumentRepository;
import com.opportunityboard.infrastructure.repository.OpportunityRepository;
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
 * Lớp 2: AI quét hồ sơ tin đăng (PROGRAM_PROOF / PARTNERSHIP_LETTER).
 * Không check MST/thuế — đó là {@link ProviderDocumentScanService} + {@link TaxCodeCheckService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityDocumentScanService {

    public static final double APPLY_THRESHOLD = 0.75;
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private final OpportunityRepository opportunityRepository;
    private final OpportunityDocumentRepository opportunityDocumentRepository;
    private final OpenRouterClient openRouterClient;
    private final OpenRouterProperties openRouterProperties;
    private final ObjectMapper objectMapper;
    private final OpportunityService opportunityService;
    private final MediaLinkService mediaLinkService;

    @Transactional
    public OpportunityDocScanResponse scanOpportunity(UUID oppId, boolean apply) {
        Opportunity opp = opportunityRepository.findById(oppId)
                .orElseThrow(() -> new NotFoundException("Tin đăng không tồn tại"));
        List<OpportunityDocument> docs = opportunityDocumentRepository.findByOpportunityOppId(oppId);
        if (docs.isEmpty()) {
            throw new BadRequestException("Tin chưa có hồ sơ chương trình / ủy quyền để quét");
        }

        Organization org = opp.getOrg();
        String previous = opp.getStatus().name();
        String prompt = buildPrompt(opp, org, docs);
        List<String> mediaUrls = docs.stream()
                .map(OpportunityDocument::getFileUrl)
                .map(mediaLinkService::resolveFetchableUrlOrNull)
                .filter(u -> u != null && !u.isBlank())
                .limit(8)
                .collect(Collectors.toList());

        String raw = openRouterClient.chat(SYSTEM_PROMPT, OpenRouterClient.textAndImages(prompt, mediaUrls));
        OpportunityDocScanResponse parsed = harden(parse(opp, org, docs.size(), raw, previous));

        if (!apply) {
            return withActions(parsed, "NONE", suggestNext(parsed));
        }
        return applyVerdict(opp, parsed);
    }

    private OpportunityDocScanResponse applyVerdict(Opportunity opp, OpportunityDocScanResponse parsed) {
        String note = buildModerationNote(parsed);
        opportunityService.saveAiScanNote(opp.getOppId(), note);

        // Không tự APPROVE / REJECT — Admin xem lý do rồi quyết định
        if ("APPROVE".equals(parsed.verdict()) && parsed.confidence() >= APPLY_THRESHOLD) {
            log.info("AI opp-scan APPROVE opp={} → chờ Admin xác nhận", opp.getOppId());
            return withActions(parsed, "NOTE_SAVED", "ADMIN_REVIEW");
        }
        if ("REJECT".equals(parsed.verdict()) || "REVIEW".equals(parsed.verdict())) {
            log.info("AI opp-scan {} opp={} → NOTE_SAVED (Admin có thể gửi yêu cầu cập nhật)",
                    parsed.verdict(), opp.getOppId());
            return withActions(parsed, "NOTE_SAVED", "REQUEST_UPDATE");
        }
        return withActions(parsed, "NOTE_SAVED", "ADMIN_REVIEW");
    }

    private static String suggestNext(OpportunityDocScanResponse p) {
        if ("APPROVE".equals(p.verdict()) && p.confidence() >= APPLY_THRESHOLD) return "ADMIN_REVIEW";
        if ("REJECT".equals(p.verdict()) || "REVIEW".equals(p.verdict())) return "REQUEST_UPDATE";
        return "ADMIN_REVIEW";
    }

    /** Lý do đầy đủ để Admin gửi cho provider / ghi nhận từ chối. */
    public static String buildModerationNote(OpportunityDocScanResponse p) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(p.verdict()).append(" · ")
                .append(Math.round(p.confidence() * 100)).append("%] ");
        if (p.summary() != null && !p.summary().isBlank()) {
            sb.append(p.summary().trim()).append(' ');
        }
        if (p.risks() != null && !p.risks().isEmpty()) {
            sb.append("| Vấn đề: ").append(String.join("; ", p.risks())).append(' ');
        }
        if (p.contentMismatches() != null && !p.contentMismatches().isEmpty()) {
            sb.append("| Không khớp: ").append(String.join("; ", p.contentMismatches())).append(' ');
        }
        if (p.recommendations() != null && !p.recommendations().isEmpty()) {
            sb.append("| Cần làm: ").append(String.join("; ", p.recommendations()));
        }
        return sb.toString().trim();
    }

    private static OpportunityDocScanResponse harden(OpportunityDocScanResponse p) {
        String verdict = p.verdict();
        double confidence = p.confidence();
        List<String> risks = new ArrayList<>(p.risks() != null ? p.risks() : List.of());

        // Org chưa VERIFIED → không gợi ý APPROVE tin
        if (!"VERIFIED".equals(p.orgVerifiedStatus()) && "APPROVE".equals(verdict)) {
            verdict = "REVIEW";
            confidence = Math.min(confidence, 0.55);
            risks.add("Tổ chức chưa VERIFIED (chưa qua lớp thuế/pháp nhân) — duyệt tin sau khi org đạt");
        }
        if (p.contentMismatches() != null && !p.contentMismatches().isEmpty() && "APPROVE".equals(verdict)) {
            verdict = "REVIEW";
            confidence = Math.min(confidence, 0.6);
        }
        String note = buildModerationNote(new OpportunityDocScanResponse(
                p.oppId(), p.title(), p.orgName(), p.orgVerifiedStatus(),
                verdict, confidence, p.summary(), p.findings(), risks, p.recommendations(),
                p.contentMismatches(), p.documentCount(), p.model(), p.scannedAt(),
                p.previousOppStatus(), p.appliedAction(), p.nextAction(), null, p.rawModelText()));
        return new OpportunityDocScanResponse(
                p.oppId(), p.title(), p.orgName(), p.orgVerifiedStatus(),
                verdict, confidence, p.summary(), p.findings(), risks, p.recommendations(),
                p.contentMismatches(), p.documentCount(), p.model(), p.scannedAt(),
                p.previousOppStatus(), p.appliedAction(), p.nextAction(), note, p.rawModelText()
        );
    }

    private static OpportunityDocScanResponse withActions(OpportunityDocScanResponse base,
                                                          String appliedAction,
                                                          String nextAction) {
        String note = base.moderationNote() != null ? base.moderationNote() : buildModerationNote(base);
        return new OpportunityDocScanResponse(
                base.oppId(), base.title(), base.orgName(), base.orgVerifiedStatus(),
                base.verdict(), base.confidence(), base.summary(), base.findings(),
                base.risks(), base.recommendations(), base.contentMismatches(),
                base.documentCount(), base.model(), base.scannedAt(),
                base.previousOppStatus(), appliedAction, nextAction, note, base.rawModelText()
        );
    }

    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên viên kiểm duyệt TIN TUYỂN / chương trình thực tập-học bổng trên Opportunity Board (VN).
            Đây là lớp HỒ SƠ TIN ĐĂNG — KHÔNG kiểm tra MST/thuế (đã tách sang lớp tổ chức).
            Nhiệm vụ: đánh giá giấy tờ PROGRAM_PROOF / PARTNERSHIP_LETTER / OTHER có chứng minh được tin đăng không.
            Trả lời CHỈ JSON:
            {
              "verdict": "APPROVE" | "REVIEW" | "REJECT",
              "confidence": 0.0-1.0,
              "summary": "tóm tắt tiếng Việt",
              "findings": ["nội dung đọc được từ hồ sơ"],
              "risks": ["spam, lừa đảo, thiếu thẩm quyền, giấy tờ không liên quan"],
              "recommendations": ["việc provider cần bổ sung cho tin này"],
              "contentMismatches": ["chỗ hồ sơ ≠ tiêu đề/mô tả/org của tin"]
            }
            Quy tắc:
            - APPROVE: hồ sơ liên quan rõ ràng tới tin (đúng công ty/chương trình), không dấu hiệu lừa đảo.
            - REVIEW: thiếu chứng minh, URL không đọc được, hoặc lệch nhẹ nội dung.
            - REJECT: spam, lừa đảo thu phí mờ ám, giấy tờ không liên quan, giả mạo thư hợp tác.
            - Không đánh giá MST/GPKD ở đây; nếu thấy giấy tờ pháp nhân thì ghi findings nhưng verdict dựa trên tính hợp lệ của TIN.
            - Không bịa OCR; không đọc được → REVIEW + confidence thấp.
            """;

    private String buildPrompt(Opportunity opp, Organization org, List<OpportunityDocument> docs) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Tin đăng (đối chiếu nội dung)\n");
        sb.append("- Tiêu đề: ").append(nullToDash(opp.getTitle())).append('\n');
        sb.append("- Org: ").append(nullToDash(org.getOrgName()))
                .append(" (verified=").append(org.getVerifiedStatus()).append(")\n");
        sb.append("- Category: ").append(opp.getCategory() != null ? opp.getCategory().getCode() : "—").append('\n');
        sb.append("- Deadline: ").append(opp.getDeadline()).append('\n');
        sb.append("- WorkType/Location: ").append(opp.getWorkType()).append(" / ").append(opp.getLocation()).append('\n');
        sb.append("- ApplyMode: ").append(opp.getApplyMode())
                .append(opp.getExternalLink() != null ? " link=" + opp.getExternalLink() : "").append('\n');
        sb.append("- Mô tả (rút gọn): ").append(truncate(opp.getDescription(), 800)).append('\n');
        sb.append("- Yêu cầu (rút gọn): ").append(truncate(opp.getRequirements(), 400)).append('\n');
        if (org.getVerifiedStatus() != OrgVerified.VERIFIED) {
            sb.append("\n⚠ Org chưa VERIFIED — không nên APPROVE tin cho đến khi lớp thuế/org đạt.\n");
        }
        sb.append("\n## Hồ sơ tin đăng (").append(docs.size()).append(") — KHÔNG phải hồ sơ thuế\n");
        int i = 1;
        for (OpportunityDocument d : docs) {
            sb.append(i++).append(". type=").append(d.getDocType())
                    .append("; title=").append(d.getTitle())
                    .append("; url=").append(d.getFileUrl()).append('\n');
        }
        sb.append("\nĐánh giá tính hợp lệ của tin + hồ sơ chương trình/ủy quyền (không check thuế).");
        return sb.toString();
    }

    private OpportunityDocScanResponse parse(Opportunity opp, Organization org, int docCount,
                                             String raw, String previous) {
        try {
            JsonNode n = objectMapper.readTree(extractJson(raw));
            String verdict = n.path("verdict").asText("REVIEW").toUpperCase(Locale.ROOT);
            if (!List.of("APPROVE", "REVIEW", "REJECT").contains(verdict)) verdict = "REVIEW";
            double confidence = Math.max(0, Math.min(1, n.path("confidence").asDouble(0.5)));
            var findings = readStringList(n.path("findings"));
            var risks = readStringList(n.path("risks"));
            var recs = readStringList(n.path("recommendations"));
            var mismatches = readStringList(n.path("contentMismatches"));
            var draft = new OpportunityDocScanResponse(
                    opp.getOppId(), opp.getTitle(), org.getOrgName(),
                    org.getVerifiedStatus().name(),
                    verdict, confidence,
                    n.path("summary").asText(""),
                    findings, risks, recs, mismatches,
                    docCount, openRouterProperties.model(), Instant.now(),
                    previous, "NONE", "ADMIN_REVIEW", null, raw
            );
            return new OpportunityDocScanResponse(
                    draft.oppId(), draft.title(), draft.orgName(), draft.orgVerifiedStatus(),
                    draft.verdict(), draft.confidence(), draft.summary(), draft.findings(),
                    draft.risks(), draft.recommendations(), draft.contentMismatches(),
                    draft.documentCount(), draft.model(), draft.scannedAt(),
                    draft.previousOppStatus(), draft.appliedAction(), draft.nextAction(),
                    buildModerationNote(draft), draft.rawModelText()
            );
        } catch (Exception ex) {
            log.warn("Opp AI JSON parse fallback: {}", ex.getMessage());
            var fallback = new OpportunityDocScanResponse(
                    opp.getOppId(), opp.getTitle(), org.getOrgName(),
                    org.getVerifiedStatus().name(),
                    "REVIEW", 0.3,
                    "AI trả lời không đúng JSON — Admin xem tay.",
                    List.of(), List.of("Không parse được JSON từ model"),
                    List.of("Kiểm tra lại hồ sơ tin đăng"),
                    List.of(), docCount, openRouterProperties.model(), Instant.now(),
                    previous, "NONE", "ADMIN_REVIEW", null, raw
            );
            return withActions(fallback, "NONE", "ADMIN_REVIEW");
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

    private static String truncate(String s, int max) {
        if (s == null || s.isBlank()) return "—";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
