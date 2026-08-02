package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.dto.application.AppStatusUpdateRequest;
import com.opportunityboard.application.dto.application.ApplicationScanRequest;
import com.opportunityboard.application.dto.document.OrgDocumentInput;
import com.opportunityboard.application.dto.opportunity.ModerateRequest;
import com.opportunityboard.application.dto.org.OrgProfileUpdateRequest;
import com.opportunityboard.application.service.ApplicationAiScanService;
import com.opportunityboard.application.service.ApplicationService;
import com.opportunityboard.application.service.OrgDocumentService;
import com.opportunityboard.common.response.PagedResponse;
import com.opportunityboard.domain.enums.AppStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationAiScanService applicationAiScanService;
    private final com.opportunityboard.application.service.OpportunityService opportunityService;
    private final OrgDocumentService orgDocumentService;

    @GetMapping("/opportunities")
    public ResponseEntity<?> myOpportunities(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        var r = opportunityService.listMine(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @GetMapping("/opportunities/{oppId}/applications")
    public ResponseEntity<?> list(@PathVariable UUID oppId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        var r = applicationService.appsByOppOwner(oppId, page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @PutMapping("/applications/{appId}/status")
    public ResponseEntity<?> changeStatus(@PathVariable UUID appId,
                                          @RequestParam(required = false) AppStatus to,
                                          @RequestParam(required = false) String note,
                                          @RequestBody(required = false) AppStatusUpdateRequest body) {
        AppStatus status = to != null ? to : (body != null ? body.status() : null);
        String n = note != null ? note : (body != null ? body.note() : null);
        if (status == null) {
            return ResponseEntity.badRequest().body("Thiếu status (query ?to= hoặc body.status)");
        }
        applicationService.changeStatusByProvider(appId, status, n);
        return ResponseEntity.ok().build();
    }

    /** AI quét 1 hồ sơ SV theo tiêu chuẩn screening. */
    @PostMapping("/applications/{appId}/ai-scan")
    public ResponseEntity<?> aiScanOne(@PathVariable UUID appId,
                                       @RequestParam(defaultValue = "true") boolean apply,
                                       @Valid @RequestBody ApplicationScanRequest body) {
        return ResponseEntity.ok(applicationAiScanService.scanOne(appId, body.criteria(), apply));
    }

    /** AI quét hàng loạt hồ sơ SUBMITTED/REVIEWING của tin — nhóm APPROVE/REVIEW/REJECT. */
    @PostMapping("/opportunities/{oppId}/applications/ai-scan")
    public ResponseEntity<?> aiScanBatch(@PathVariable UUID oppId,
                                         @RequestParam(defaultValue = "true") boolean apply,
                                         @Valid @RequestBody ApplicationScanRequest body) {
        return ResponseEntity.ok(applicationAiScanService.scanOpportunity(oppId, body.criteria(), apply));
    }

    /** Gửi lý do yêu cầu sinh viên cập nhật hồ sơ (sau khi provider xem lại AI). */
    @PostMapping("/applications/{appId}/request-update")
    public ResponseEntity<?> requestUpdate(@PathVariable UUID appId,
                                           @Valid @RequestBody ModerateRequest body) {
        applicationService.requestStudentUpdate(appId, body.reason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/applications/export")
    public ResponseEntity<?> export(@RequestParam(required = false) UUID oppId) {
        // MVP: trả về tổng số; thực tế sinh CSV stream (P1)
        return ResponseEntity.ok().build();
    }

    @GetMapping("/org")
    public ResponseEntity<?> myOrgProfile() {
        return ResponseEntity.ok(orgDocumentService.getMyProfile());
    }

    @PutMapping("/org")
    public ResponseEntity<?> updateOrgProfile(@Valid @RequestBody OrgProfileUpdateRequest body) {
        return ResponseEntity.ok(orgDocumentService.updateMyProfile(body));
    }

    @GetMapping("/org/documents")
    public ResponseEntity<?> listOrgDocuments() {
        return ResponseEntity.ok(orgDocumentService.listMine());
    }

    @PostMapping("/org/documents")
    public ResponseEntity<?> addOrgDocument(@Valid @RequestBody OrgDocumentInput input) {
        return ResponseEntity.ok(orgDocumentService.add(input));
    }

    @DeleteMapping("/org/documents/{docId}")
    public ResponseEntity<?> deleteOrgDocument(@PathVariable UUID docId) {
        orgDocumentService.delete(docId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/opportunities/{oppId}/documents")
    public ResponseEntity<?> listOppDocuments(@PathVariable UUID oppId) {
        return ResponseEntity.ok(opportunityService.listDocumentsForOwnerOrAdmin(oppId));
    }
}
