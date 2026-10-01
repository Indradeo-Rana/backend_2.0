package in.infosys.backend.dto;

import in.infosys.backend.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;

    private String type;

    private String title;

    private String message;

    private boolean read;

    private LocalDateTime createdAt;

    public static NotificationResponseDto fromEntity(Notification notification) {

        return new NotificationResponseDto(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}