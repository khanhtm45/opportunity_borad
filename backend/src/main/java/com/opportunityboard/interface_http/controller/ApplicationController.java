package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.service.ApplicationService;
import com.opportunityboard.common.response.PagedResponse;
import com.opportunityboard.domain.enums.AppStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provider")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final com.opportunityboard.application.service.OpportunityService opportunityService;

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
                                          @RequestParam AppStatus to,
                                          @RequestParam(required = false) String note) {
        applicationService.changeStatusByProvider(appId, to, note);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/applications/export")
    public ResponseEntity<?> export(@RequestParam(required = false) UUID oppId) {
        // MVP: trả về tổng số; thực tế sinh CSV stream (P1)
        return ResponseEntity.ok().build();
    }
}
