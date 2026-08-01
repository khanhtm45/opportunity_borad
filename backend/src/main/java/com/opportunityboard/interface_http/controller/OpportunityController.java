package com.opportunityboard.interface_http.controller;

import com.opportunityboard.common.response.PagedResponse;
import com.opportunityboard.application.dto.opportunity.*;
import com.opportunityboard.application.service.OpportunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final com.opportunityboard.infrastructure.repository.CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        var r = opportunityService.listPublic(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        var r = opportunityService.listMine(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }


    @GetMapping("/featured")
    public ResponseEntity<?> featured(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        var r = opportunityService.listFeatured(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) List<String> categories,
                                    @RequestParam(required = false) String workType,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(defaultValue = "newest") String sort,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        UUID categoryId = null;
        if (categories != null && !categories.isEmpty()) {
            var cat = categoryRepository.findByCode(categories.get(0));
            categoryId = cat.map(c -> c.getCategoryId()).orElse(null);
        }
        var r = opportunityService.search(q, categoryId, workType, location, sort, page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> detail(@PathVariable String slug) {
        return ResponseEntity.ok(opportunityService.detail(slug));
    }

    // ----- Provider -----
    @PostMapping
    public ResponseEntity<?> create(@RequestBody OpportunityRequest req) {
        return ResponseEntity.ok(opportunityService.create(req));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable UUID id) {
        opportunityService.submit(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody OpportunityRequest req) {
        opportunityService.update(id, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<?> hide(@PathVariable UUID id) {
        opportunityService.setHidden(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/show")
    public ResponseEntity<?> show(@PathVariable UUID id) {
        opportunityService.setHidden(id, false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable UUID id) {
        opportunityService.close(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<?> extend(@PathVariable UUID id, @RequestParam long newDeadlineEpoch) {
        opportunityService.extend(id, java.time.Instant.ofEpochSecond(newDeadlineEpoch));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/feature-request")
    public ResponseEntity<?> featureRequest(@PathVariable UUID id) {
        // MVP: Provider đề xuất -> Admin duyệt (feature set bởi admin)
        return ResponseEntity.accepted().build();
    }
}
