package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.dto.student.StudentProfileUpdateRequest;
import com.opportunityboard.application.service.ApplicationService;
import com.opportunityboard.application.service.SavedOpportunityService;
import com.opportunityboard.application.service.StudentProfileService;
import com.opportunityboard.common.response.PagedResponse;
import com.opportunityboard.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentController {

    private final ApplicationService applicationService;
    private final SavedOpportunityService savedService;
    private final StudentProfileService studentProfileService;
    private final CurrentUser currentUser;

    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> myProfile() {
        return ResponseEntity.ok(studentProfileService.getMine());
    }

    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody StudentProfileUpdateRequest body) {
        return ResponseEntity.ok(studentProfileService.updateMine(body));
    }

    // F04.1 apply internal
    @PostMapping("/opportunities/{id}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> apply(@PathVariable UUID id,
                                   @RequestParam(required = false) String cvFile,
                                   @RequestParam(required = false) String coverLetter) {
        return ResponseEntity.ok(applicationService.applyInternal(id, cvFile, coverLetter));
    }

    // F04.2 bookmark
    @PostMapping("/opportunities/{id}/save")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> save(@PathVariable UUID id, @RequestParam(defaultValue = "48") short notifyBeforeHours) {
        savedService.save(id, notifyBeforeHours);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/opportunities/{id}/save")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> unsave(@PathVariable UUID id) {
        savedService.unsave(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/bookmarks")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> bookmarks(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        var r = savedService.list(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @GetMapping("/me/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> myApps(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        var r = applicationService.appsByStudent(currentUser.getId(), page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @PostMapping("/me/applications/{appId}/withdraw")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> withdraw(@PathVariable UUID appId) {
        applicationService.withdraw(appId);
        return ResponseEntity.ok().build();
    }
}
