package com.opportunityboard.interface_http.controller;

import com.opportunityboard.application.service.NotificationQueryService;
import com.opportunityboard.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/notifications")
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        var r = notificationQueryService.list(page, size);
        return ResponseEntity.ok(PagedResponse.of(r.items(), null, r.total()));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> read(@PathVariable java.util.UUID id) {
        notificationQueryService.markRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notification-preferences")
    public ResponseEntity<?> prefs() {
        return ResponseEntity.ok(notificationQueryService.getPreferences());
    }
}
