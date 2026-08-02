package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.dto.application.ApplicationScanRequest;
import com.opportunityboard.application.dto.opportunity.FeatureRequest;
import com.opportunityboard.application.dto.opportunity.ModerateRequest;
import com.opportunityboard.application.service.AdminService;
import com.opportunityboard.application.service.ApplicationAiScanService;
import com.opportunityboard.application.service.ApplicationService;
import com.opportunityboard.application.service.OpportunityDocumentScanService;
import com.opportunityboard.application.service.OpportunityService;
import com.opportunityboard.application.service.OrgDocumentService;
import com.opportunityboard.application.service.ProviderDocumentScanService;
import com.opportunityboard.common.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OpportunityService opportunityService;
    private final AdminService adminService;
    private final OrgDocumentService orgDocumentService;
    private final ProviderDocumentScanService providerDocumentScanService;
    private final OpportunityDocumentScanService opportunityDocumentScanService;
    private final ApplicationAiScanService applicationAiScanService;
    private final ApplicationService applicationService;

    // F06: hàng đợi kiểm duyệt
    @GetMapping("/moderation-queue")
    public ResponseEntity<?> queue(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        var r = opportunityService.listPending(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @PostMapping("/opportunities/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        opportunityService.approve(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/opportunities/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id, @RequestBody ModerateRequest req) {
        opportunityService.reject(id, req);
        return ResponseEntity.ok().build();
    }

    /** Gửi yêu cầu provider cập nhật tin (kèm lý do AI/Admin) — tin về DRAFT. */
    @PostMapping("/opportunities/{id}/request-update")
    public ResponseEntity<?> requestUpdate(@PathVariable UUID id, @RequestBody ModerateRequest req) {
        opportunityService.requestUpdate(id, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/opportunities/{id}/feature")
    public ResponseEntity<?> feature(@PathVariable UUID id, @RequestBody FeatureRequest req) {
        opportunityService.feature(id, req, true);
        return ResponseEntity.ok().build();
    }

    // F06: thống kê
    @GetMapping("/analytics")
    public ResponseEntity<?> analytics() {
        return ResponseEntity.ok(adminService.analytics());
    }

    // F06: danh sách người dùng
    @GetMapping("/users")
    public ResponseEntity<?> users(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(adminService.listUsers(page, size));
    }

    // F06: duyệt tổ chức nhà tuyển dụng
    @PostMapping("/users/{id}/verify-org")
    public ResponseEntity<?> verifyOrg(@PathVariable UUID id) {
        adminService.verifyOrg(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orgs/{orgId}/documents")
    public ResponseEntity<?> orgDocuments(@PathVariable UUID orgId) {
        return ResponseEntity.ok(orgDocumentService.listByOrg(orgId));
    }

    /**
     * Lớp 1 — AI quét hồ sơ TỔ CHỨC (thuế/pháp nhân). Không quét tin đăng.
     * apply=true: APPROVE+tax PASS → VERIFIED; còn lại → NEEDS_UPDATE.
     */
    @PostMapping("/orgs/{orgId}/ai-scan")
    public ResponseEntity<?> aiScanOrg(@PathVariable UUID orgId,
                                       @RequestParam(defaultValue = "true") boolean apply) {
        return ResponseEntity.ok(providerDocumentScanService.scanOrg(orgId, apply));
    }

    /** Lớp 1 — AI quét org theo owner userId. */
    @PostMapping("/users/{id}/ai-scan-org")
    public ResponseEntity<?> aiScanOrgByUser(@PathVariable UUID id,
                                             @RequestParam(defaultValue = "true") boolean apply) {
        return ResponseEntity.ok(providerDocumentScanService.scanByOwnerUserId(id, apply));
    }

    /** Lớp 1 — chỉ check MST (format + checksum), không gọi AI. */
    @GetMapping("/orgs/{orgId}/tax-check")
    public ResponseEntity<?> taxCheckOrg(@PathVariable UUID orgId) {
        return ResponseEntity.ok(providerDocumentScanService.taxCheckOrg(orgId));
    }

    @GetMapping("/users/{id}/tax-check-org")
    public ResponseEntity<?> taxCheckOrgByUser(@PathVariable UUID id) {
        return ResponseEntity.ok(providerDocumentScanService.taxCheckByOwnerUserId(id));
    }

    /**
     * Lớp 2 — AI quét hồ sơ TIN ĐĂNG (PROGRAM_PROOF / PARTNERSHIP_LETTER). Không check thuế.
     * apply=true: chỉ tự REJECT khi verdict=REJECT ≥0.75; không tự APPROVE tin.
     */
    @PostMapping("/opportunities/{id}/ai-scan")
    public ResponseEntity<?> aiScanOpportunity(@PathVariable UUID id,
                                               @RequestParam(defaultValue = "true") boolean apply) {
        return ResponseEntity.ok(opportunityDocumentScanService.scanOpportunity(id, apply));
    }

    @GetMapping("/opportunities/{id}/documents")
    public ResponseEntity<?> opportunityDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(opportunityService.listDocumentsForOwnerOrAdmin(id));
    }

    /** Danh sách ứng tuyển (SUBMITTED/REVIEWING) — Admin giám sát / AI scan. */
    @GetMapping("/applications")
    public ResponseEntity<?> applications(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size,
                                          @RequestParam(required = false) UUID oppId) {
        return ResponseEntity.ok(adminService.listApplications(page, size, oppId));
    }

    /** Lớp 3 — AI quét CV/hồ sơ SV theo tiêu chuẩn (Admin, giống Provider). */
    @PostMapping("/applications/{appId}/ai-scan")
    public ResponseEntity<?> aiScanApplication(@PathVariable UUID appId,
                                               @RequestParam(defaultValue = "true") boolean apply,
                                               @Valid @RequestBody ApplicationScanRequest body) {
        return ResponseEntity.ok(applicationAiScanService.scanOne(appId, body.criteria(), apply));
    }

    @PostMapping("/opportunities/{id}/applications/ai-scan")
    public ResponseEntity<?> aiScanApplications(@PathVariable UUID id,
                                                @RequestParam(defaultValue = "true") boolean apply,
                                                @Valid @RequestBody ApplicationScanRequest body) {
        return ResponseEntity.ok(applicationAiScanService.scanOpportunity(id, body.criteria(), apply));
    }

    @PostMapping("/applications/{appId}/request-update")
    public ResponseEntity<?> requestAppUpdate(@PathVariable UUID appId,
                                              @Valid @RequestBody ModerateRequest body) {
        applicationService.requestStudentUpdate(appId, body.reason());
        return ResponseEntity.ok().build();
    }
}
