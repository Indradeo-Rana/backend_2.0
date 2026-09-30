package in.infosys.backend.service;

import in.infosys.backend.entity.Notification;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Create notification
    public Notification create(
            User user,
            String type,
            String title,
            String message) {

        Notification notification = new Notification();

        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    // Get all notifications for a user
    public List<Notification> getUserNotifications(User user) {

        return notificationRepository
                .findAllByUserOrderByCreatedAtDesc(user);
    }

    // Get unread notifications
    public List<Notification> getUnreadNotifications(User user) {

        return notificationRepository
                .findAllByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    // Mark notification as read
    public Notification markAsRead(
            Long notificationId,
            User user) {

        Notification notification =
                notificationRepository
                        .findByIdAndUser(notificationId, user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"));

        notification.setRead(true);

        return notificationRepository.save(notification);
    }

    // Delete notification
    public void delete(
            Long notificationId,
            User user) {

        Notification notification =
                notificationRepository
                        .findByIdAndUser(notificationId, user)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"));

        notificationRepository.delete(notification);
    }

    public void loginSuccess(User user, String ipAddress) {
    }

    public void loginFailure(User user, String ipAddress) {

    }
}