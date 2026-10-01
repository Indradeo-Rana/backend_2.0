package in.infosys.backend.repository;

import in.infosys.backend.entity.Notification;
import in.infosys.backend.entity.NotificationType;
import in.infosys.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findAllByUserOrderByCreatedAtDesc(User user);

    List<Notification>
    findAllByUserAndReadFalseOrderByCreatedAtDesc(User user);

    long countByUserAndReadFalse(User user);

    boolean existsByUserAndTypeAndCreatedAtAfter(
            User user,
            NotificationType type,
            LocalDateTime createdAt
    );
}