package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.dto.opportunity.FeatureRequest;
import com.opportunityboard.application.dto.opportunity.ModerateRequest;
import com.opportunityboard.application.service.AdminService;
import com.opportunityboard.application.service.OpportunityService;
import com.opportunityboard.common.response.PagedResponse;
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
}
