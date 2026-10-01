package in.infosys.backend.service;

import in.infosys.backend.dto.NotificationResponseDto;
import in.infosys.backend.dto.PushTokenRequestDto;
import in.infosys.backend.entity.Notification;
import in.infosys.backend.entity.User;
import in.infosys.backend.entity.NotificationType;
import in.infosys.backend.repository.NotificationRepository;
import in.infosys.backend.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailNotificationService emailNotificationService;
    private final PushNotificationService pushNotificationService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailNotificationService emailNotificationService,
            PushNotificationService pushNotificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailNotificationService = emailNotificationService;
        this.pushNotificationService = pushNotificationService;
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    private User getCurrentUser() {

        String username =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Logged-in user not found"
                        )
                );
    }

    // =========================================================
    // MAIN NOTIFICATION CREATION
    // =========================================================

    @Transactional
    public Notification createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            boolean sendEmail,
            boolean sendPush
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Notification recipient cannot be null"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification title is required"
            );
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification message is required"
            );
        }

        Notification notification = new Notification();

        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setEmailSent(false);
        notification.setPushSent(false);
        notification.setCreatedAt(LocalDateTime.now());

        // Save first.
        // This means notification is not lost even if
        // email/push delivery fails.
        Notification saved =
                notificationRepository.save(notification);

        // =====================================================
        // EMAIL
        // =====================================================

        if (sendEmail) {

            try {

                boolean emailSent =
                        emailNotificationService.send(
                                user.getEmail(),
                                title,
                                message
                        );

                saved.setEmailSent(emailSent);

            } catch (Exception e) {

                saved.setEmailSent(false);
                saved.setEmailError(
                        safeError(e.getMessage())
                );

                System.err.println(
                        "Notification email failed: "
                                + e.getMessage()
                );
            }
        }

        // =====================================================
        // PUSH
        // =====================================================

        if (sendPush) {

            try {

                boolean pushSent =
                        pushNotificationService.send(
                                user,
                                title,
                                message
                        );

                saved.setPushSent(pushSent);

            } catch (Exception e) {

                saved.setPushSent(false);
                saved.setPushError(
                        safeError(e.getMessage())
                );

                System.err.println(
                        "Notification push failed: "
                                + e.getMessage()
                );
            }
        }

        return notificationRepository.save(saved);
    }

    // =========================================================
    // BACKWARD COMPATIBILITY
    // =========================================================

    public Notification create(
            User user,
            String type,
            String title,
            String message
    ) {

        NotificationType notificationType;

        try {

            notificationType =
                    NotificationType.valueOf(type);

        } catch (Exception e) {

            notificationType =
                    NotificationType.SECURITY_NOTIFICATION;
        }

        return createNotification(
                user,
                notificationType,
                title,
                message,
                true,
                true
        );
    }

    // =========================================================
    // LOGIN SUCCESS
    // =========================================================

    public Notification loginSuccess(
            User user,
            String ipAddress
    ) {

        String message =
                "A successful login to your SecureVault account "
                        + "was detected from IP address "
                        + ipAddress
                        + ".";

        return createNotification(
                user,
                NotificationType.LOGIN_ALERT,
                "Successful login",
                message,
                true,
                true
        );
    }

    // =========================================================
    // LOGIN FAILURE
    // =========================================================

    public Notification loginFailure(
            User user,
            String ipAddress
    ) {

        String message =
                "A failed login attempt was detected for "
                        + "your SecureVault account from IP address "
                        + ipAddress
                        + ".";

        return createNotification(
                user,
                NotificationType.LOGIN_ALERT,
                "Failed login attempt",
                message,
                true,
                true
        );
    }

    // =========================================================
    // SECURITY NOTIFICATION
    // =========================================================

    public Notification securityNotification(
            User user,
            String title,
            String message
    ) {

        return createNotification(
                user,
                NotificationType.SECURITY_NOTIFICATION,
                title,
                message,
                true,
                true
        );
    }

    // =========================================================
    // SHARING NOTIFICATION
    // =========================================================

    public Notification sharingNotification(
            User user,
            String credentialTitle,
            String permission
    ) {

        String message =
                "A credential named '"
                        + credentialTitle
                        + "' was shared with you with "
                        + permission
                        + " permission.";

        return createNotification(
                user,
                NotificationType.SHARING_NOTIFICATION,
                "Credential shared with you",
                message,
                true,
                true
        );
    }

    // =========================================================
    // PASSWORD EXPIRATION
    // =========================================================

    public Notification passwordExpirationAlert(
            User user,
            long daysRemaining
    ) {

        String message =
                "Your SecureVault password will expire in "
                        + daysRemaining
                        + " day(s). Please update your password.";

        return createNotification(
                user,
                NotificationType.PASSWORD_EXPIRATION_ALERT,
                "Password expiration reminder",
                message,
                true,
                true
        );
    }

    // =========================================================
    // RISK ALERT
    // =========================================================

    public Notification riskAlert(
            User user,
            int failedAttempts
    ) {

        String message =
                "Multiple failed login attempts were detected "
                        + "for your SecureVault account. "
                        + failedAttempts
                        + " failed attempts were detected "
                        + "within the security monitoring window.";

        return createNotification(
                user,
                NotificationType.RISK_ALERT,
                "Security risk detected",
                message,
                true,
                true
        );
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUserNotifications() {

        User user = getCurrentUser();

        return notificationRepository
                .findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponseDto::fromEntity)
                .toList();
    }

    // =========================================================
    // GET UNREAD
    // =========================================================

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getUnreadNotifications() {

        User user = getCurrentUser();

        return notificationRepository
                .findAllByUserAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponseDto::fromEntity)
                .toList();
    }

    // =========================================================
    // UNREAD COUNT
    // =========================================================

    @Transactional(readOnly = true)
    public long getUnreadCount() {

        User user = getCurrentUser();

        return notificationRepository
                .countByUserAndReadFalse(user);
    }

    // =========================================================
    // MARK ONE AS READ
    // =========================================================

    @Transactional
    public void markAsRead(Long notificationId) {

        User user = getCurrentUser();

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Notification not found"
                                )
                        );

        if (!notification.getUser().getId()
                .equals(user.getId())) {

            throw new RuntimeException(
                    "You are not authorized to update this notification"
            );
        }

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    @Transactional
    public void markAllAsRead() {

        User user = getCurrentUser();

        List<Notification> notifications =
                notificationRepository
                        .findAllByUserAndReadFalseOrderByCreatedAtDesc(
                                user
                        );

        notifications.forEach(
                notification -> notification.setRead(true)
        );

        notificationRepository.saveAll(notifications);
    }

    // =========================================================
    // REGISTER PUSH TOKEN
    // =========================================================

    @Transactional
    public void registerPushToken(
            PushTokenRequestDto request
    ) {

        if (request == null
                || request.getToken() == null
                || request.getToken().isBlank()) {

            throw new IllegalArgumentException(
                    "Push token is required"
            );
        }

        User user = getCurrentUser();

        user.setPushToken(
                request.getToken().trim()
        );

        userRepository.save(user);
    }

    // =========================================================
    // ERROR CLEANING
    // =========================================================

    private String safeError(String error) {

        if (error == null) {
            return "Unknown delivery error";
        }

        if (error.length() > 1000) {
            return error.substring(0, 1000);
        }

        return error;
    }
}