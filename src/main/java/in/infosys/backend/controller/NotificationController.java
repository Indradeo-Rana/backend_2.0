package in.infosys.backend.controller;

import in.infosys.backend.dto.NotificationResponseDto;
import in.infosys.backend.dto.PushTokenRequestDto;
import in.infosys.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>>
    getNotifications() {

        return ResponseEntity.ok(
                notificationService.getUserNotifications()
        );
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponseDto>>
    getUnreadNotifications() {

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications()
        );
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getUnreadCount() {

        return ResponseEntity.ok(
                notificationService.getUnreadCount()
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long id
    ) {

        notificationService.markAsRead(id);

        return ResponseEntity.ok(
                "Notification marked as read"
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllAsRead() {

        notificationService.markAllAsRead();

        return ResponseEntity.ok(
                "All notifications marked as read"
        );
    }

    @PostMapping("/device")
    public ResponseEntity<String> registerPushToken(
            @RequestBody PushTokenRequestDto request
    ) {

        notificationService.registerPushToken(request);

        return ResponseEntity.ok(
                "Push token registered successfully"
        );
    }
}