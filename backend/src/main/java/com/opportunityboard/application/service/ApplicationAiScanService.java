package com.opportunityboard.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opportunityboard.application.dto.ai.ApplicationAiScanResponse;
import com.opportunityboard.application.dto.ai.ApplicationBatchScanResponse;
import com.opportunityboard.common.exception.BadRequestException;
import com.opportunityboard.common.exception.NotFoundException;
import com.opportunityboard.domain.entity.Application;
import com.opportunityboard.domain.entity.Opportunity;
import com.opportunityboard.domain.entity.StudentProfile;
import com.opportunityboard.domain.entity.User;
import com.opportunityboard.domain.enums.AppStatus;
import com.opportunityboard.infrastructure.ai.OpenRouterClient;
import com.opportunityboard.infrastructure.ai.OpenRouterProperties;
import com.opportunityboard.infrastructure.repository.ApplicationRepository;
import com.opportunityboard.infrastructure.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
 * AI quét CV / hồ sơ sinh viên theo tiêu chuẩn screening do nhà đăng nhập.
 * Không tự ACCEPT/REJECT — provider xem lý do rồi gửi yêu cầu cập nhật hoặc quyết định.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationAiScanService {

    private static final int BATCH_LIMIT = 25;
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private final ApplicationRepository applicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ApplicationService applicationService;
    private final OpenRouterClient openRouterClient;
    private final OpenRouterProperties openRouterProperties;
    private final ObjectMapper objectMapper;
    private final MediaLinkService mediaLinkService;

    @Transactional
    public ApplicationAiScanResponse scanOne(UUID appId, String criteria, boolean apply) {
        String c = requireCriteria(criteria);
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new NotFoundException("Đơn ứng tuyển không tồn tại"));
        applicationService.requireOwnerOfOpp(app.getOpportunity());
        return scanApplication(app, c, apply);
    }

    @Transactional
    public ApplicationBatchScanResponse scanOpportunity(UUID oppId, String criteria, boolean apply) {
        String c = requireCriteria(criteria);
        Opportunity opp = applicationService.requireOwnerOfOppId(oppId);

        // Sau khi đã check quyền: lấy theo oppId (Admin không phải org owner).
        var page = applicationRepository.findByOpportunityOppId(oppId, PageRequest.of(0, BATCH_LIMIT));
        List<Application> apps = page.getContent().stream()
                .filter(a -> a.getStatus() == AppStatus.SUBMITTED || a.getStatus() == AppStatus.REVIEWING)
                .collect(Collectors.toList());

        List<ApplicationAiScanResponse> results = new ArrayList<>();
        for (Application app : apps) {
            try {
                results.add(scanApplication(app, c, apply));
            } catch (Exception ex) {
                log.warn("AI app-scan failed app={}: {}", app.getAppId(), ex.getMessage());
                results.add(fallback(app, c, "Lỗi quét: " + ex.getMessage()));
            }
        }

        return group(oppId, opp.getTitle(), c, results);
    }

    private ApplicationAiScanResponse scanApplication(Application app, String criteria, boolean apply) {
        if (app.getCvFile() == null || app.getCvFile().isBlank()) {
            throw new BadRequestException("Ứng viên chưa có CV để quét");
        }
        Opportunity opp = app.getOpportunity();
        User student = app.getStudent();
        StudentProfile profile = studentProfileRepository.findByUserUserId(student.getUserId()).orElse(null);

        String prompt = buildPrompt(opp, app, student, profile, criteria);
        List<String> media = List.of();
        if (looksLikeMedia(app.getCvFile())) {
            media = List.of(mediaLinkService.resolveFetchableUrl(app.getCvFile()));
        }

        String raw = openRouterClient.chat(SYSTEM_PROMPT, OpenRouterClient.textAndImages(prompt, media));
        ApplicationAiScanResponse parsed = parse(app, student, criteria, raw);

        if (!apply) {
            return withActions(parsed, "NONE", suggestNext(parsed));
        }
        applicationService.saveAiScanNote(app.getAppId(), parsed.moderationNote(), criteria);
        return withActions(parsed, "NOTE_SAVED", suggestNext(parsed));
    }

    private static String suggestNext(ApplicationAiScanResponse p) {
        if ("APPROVE".equals(p.verdict()) && p.confidence() >= 0.75) return "ACCEPT";
        if ("REJECT".equals(p.verdict())) return "REJECT";
        return "REQUEST_UPDATE";
    }

    public static String buildModerationNote(ApplicationAiScanResponse p) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(p.verdict()).append(" · ")
                .append(Math.round(p.confidence() * 100)).append("%] ");
        if (p.summary() != null && !p.summary().isBlank()) {
            sb.append(p.summary().trim()).append(' ');
        }
        if (p.gaps() != null && !p.gaps().isEmpty()) {
            sb.append("| Thiếu: ").append(String.join("; ", p.gaps())).append(' ');
        }
        if (p.risks() != null && !p.risks().isEmpty()) {
            sb.append("| Rủi ro: ").append(String.join("; ", p.risks())).append(' ');
        }
        if (p.recommendations() != null && !p.recommendations().isEmpty()) {
            sb.append("| Cần bổ sung: ").append(String.join("; ", p.recommendations()));
        }
        return sb.toString().trim();
    }

    private static ApplicationBatchScanResponse group(UUID oppId, String title, String criteria,
                                                      List<ApplicationAiScanResponse> results) {
        List<ApplicationAiScanResponse> approve = new ArrayList<>();
        List<ApplicationAiScanResponse> review = new ArrayList<>();
        List<ApplicationAiScanResponse> reject = new ArrayList<>();
        for (ApplicationAiScanResponse r : results) {
            switch (r.verdict() != null ? r.verdict() : "REVIEW") {
                case "APPROVE" -> approve.add(r);
                case "REJECT" -> reject.add(r);
                default -> review.add(r);
            }
        }
        return new ApplicationBatchScanResponse(
                oppId, title, criteria, results.size(), results, approve, review, reject);
    }

    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên viên sàng lọc hồ sơ ứng tuyển (CV sinh viên) trên Opportunity Board (VN).
            Nhà tuyển dụng đã nhập TIÊU CHUẨN screening — đánh giá CV/hồ sơ có khớp không.
            Trả lời CHỈ JSON:
            {
              "verdict": "APPROVE" | "REVIEW" | "REJECT",
              "confidence": 0.0-1.0,
              "summary": "tóm tắt tiếng Việt",
              "strengths": ["điểm mạnh khớp tiêu chuẩn"],
              "gaps": ["thiếu so với tiêu chuẩn"],
              "risks": ["rủi ro / nghi ngờ (CV giả, không liên quan…)"],
              "recommendations": ["việc sinh viên cần bổ sung / cập nhật"]
            }
            Quy tắc:
            - APPROVE: khớp rõ tiêu chuẩn chính, CV đọc được, không dấu hiệu giả mạo.
            - REVIEW: thiếu một số tiêu chí, CV khó đọc, hoặc cần bổ sung chứng chỉ/kinh nghiệm.
            - REJECT: lệch hoàn toàn tiêu chuẩn, spam, CV không liên quan ngành/vị trí.
            - Không bịa nội dung CV; không đọc được → REVIEW + confidence thấp + ghi gaps.
            """;

    private String buildPrompt(Opportunity opp, Application app, User student,
                               StudentProfile profile, String criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Tiêu chuẩn screening (nhà đăng nhập)\n");
        sb.append(criteria.trim()).append("\n\n");
        sb.append("## Tin tuyển\n");
        sb.append("- Tiêu đề: ").append(nullToDash(opp.getTitle())).append('\n');
        sb.append("- Yêu cầu tin: ").append(truncate(opp.getRequirements(), 600)).append('\n');
        sb.append("- Mô tả (rút gọn): ").append(truncate(opp.getDescription(), 500)).append('\n');
        sb.append("\n## Ứng viên\n");
        sb.append("- Tên: ").append(nullToDash(student.getFullName())).append('\n');
        sb.append("- Email: ").append(nullToDash(student.getEmail())).append('\n');
        if (profile != null) {
            sb.append("- Trường: ").append(nullToDash(profile.getUniversity()))
                    .append(" · ngành ").append(nullToDash(profile.getMajor()))
                    .append(" · năm ").append(profile.getUniversityYear() != null ? profile.getUniversityYear() : "—")
                    .append('\n');
            sb.append("- Skills: ").append(nullToDash(profile.getSkills())).append('\n');
            sb.append("- Bio: ").append(truncate(profile.getBio(), 300)).append('\n');
        }
        sb.append("- Cover letter: ").append(truncate(app.getCoverLetter(), 400)).append('\n');
        sb.append("- CV URL: ").append(app.getCvFile()).append('\n');
        sb.append("\nĐánh giá CV/hồ sơ theo TIÊU CHUẨN screening (ưu tiên hơn yêu cầu tin nếu khác nhau).");
        return sb.toString();
    }

    private ApplicationAiScanResponse parse(Application app, User student, String criteria, String raw) {
        try {
            JsonNode n = objectMapper.readTree(extractJson(raw));
            String verdict = n.path("verdict").asText("REVIEW").toUpperCase(Locale.ROOT);
            if (!List.of("APPROVE", "REVIEW", "REJECT").contains(verdict)) verdict = "REVIEW";
            double confidence = Math.max(0, Math.min(1, n.path("confidence").asDouble(0.5)));
            var draft = new ApplicationAiScanResponse(
                    app.getAppId(), app.getOpportunity().getOppId(),
                    student.getFullName(), student.getEmail(), app.getStatus().name(),
                    verdict, confidence,
                    n.path("summary").asText(""),
                    readStringList(n.path("strengths")),
                    readStringList(n.path("gaps")),
                    readStringList(n.path("risks")),
                    readStringList(n.path("recommendations")),
                    criteria, openRouterProperties.model(), Instant.now(),
                    "NONE", "PROVIDER_REVIEW", null, raw
            );
            return withActions(draft, "NONE", suggestNext(draft));
        } catch (Exception ex) {
            log.warn("App AI JSON parse fallback: {}", ex.getMessage());
            return fallback(app, criteria, "AI trả lời không đúng JSON — xem tay.");
        }
    }

    private ApplicationAiScanResponse fallback(Application app, String criteria, String summary) {
        User student = app.getStudent();
        var draft = new ApplicationAiScanResponse(
                app.getAppId(), app.getOpportunity().getOppId(),
                student.getFullName(), student.getEmail(), app.getStatus().name(),
                "REVIEW", 0.3, summary,
                List.of(), List.of("Không parse / quét được đầy đủ"),
                List.of(), List.of("Kiểm tra lại CV thủ công"),
                criteria, openRouterProperties.model(), Instant.now(),
                "NONE", "PROVIDER_REVIEW", null, null
        );
        return withActions(draft, "NONE", "REQUEST_UPDATE");
    }

    private static ApplicationAiScanResponse withActions(ApplicationAiScanResponse base,
                                                         String appliedAction,
                                                         String nextAction) {
        String note = base.moderationNote() != null ? base.moderationNote() : buildModerationNote(base);
        return new ApplicationAiScanResponse(
                base.appId(), base.oppId(), base.studentName(), base.studentEmail(), base.appStatus(),
                base.verdict(), base.confidence(), base.summary(),
                base.strengths(), base.gaps(), base.risks(), base.recommendations(),
                base.criteriaUsed(), base.model(), base.scannedAt(),
                appliedAction, nextAction, note, base.rawModelText()
        );
    }

    private static String requireCriteria(String criteria) {
        if (criteria == null || criteria.isBlank()) {
            throw new BadRequestException("Nhập tiêu chuẩn screening trước khi quét AI");
        }
        String c = criteria.trim();
        if (c.length() < 10) {
            throw new BadRequestException("Tiêu chuẩn screening quá ngắn (≥10 ký tự)");
        }
        return c;
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
