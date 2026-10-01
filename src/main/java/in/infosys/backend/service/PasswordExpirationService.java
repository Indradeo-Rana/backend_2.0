package in.infosys.backend.service;

import in.infosys.backend.entity.User;
import in.infosys.backend.entity.NotificationType;
import in.infosys.backend.repository.NotificationRepository;
import in.infosys.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class PasswordExpirationService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Value("${security.password.expiration-days:90}")
    private long expirationDays;

    @Value("${security.password.expiration-warning-days:7}")
    private long warningDays;

    public PasswordExpirationService(
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(
            fixedDelayString = "${security.password.expiration-check-ms:86400000}"
    )
    public void checkPasswordExpiration() {

        LocalDateTime now = LocalDateTime.now();

        for (User user : userRepository.findAll()) {

            if (user.getPasswordChangedAt() == null) {
                continue;
            }

            LocalDateTime expirationDate =
                    user.getPasswordChangedAt()
                            .plusDays(expirationDays);

            long daysRemaining =
                    ChronoUnit.DAYS.between(
                            now.toLocalDate(),
                            expirationDate.toLocalDate()
                    );

            if (daysRemaining < 0) {
                continue;
            }

            if (daysRemaining <= warningDays) {

                LocalDateTime duplicateCheckTime =
                        now.minusHours(24);

                boolean alreadySent =
                        notificationRepository
                                .existsByUserAndTypeAndCreatedAtAfter(
                                        user,
                                        NotificationType.PASSWORD_EXPIRATION_ALERT,
                                        duplicateCheckTime
                                );

                if (!alreadySent) {

                    notificationService.passwordExpirationAlert(
                            user,
                            daysRemaining
                    );
                }
            }
        }
    }
}