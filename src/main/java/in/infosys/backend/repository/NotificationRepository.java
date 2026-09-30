package in.infosys.backend.repository;

import in.infosys.backend.entity.Notification;
import in.infosys.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserOrderByCreatedAtDesc(
            User user);

    List<Notification> findAllByUserAndReadFalseOrderByCreatedAtDesc(
            User user);

    Optional<Notification> findByIdAndUser(
            Long id,
            User user);
}
