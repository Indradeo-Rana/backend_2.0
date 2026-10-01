package in.infosys.backend.dto;

import in.infosys.backend.entity.Notification;
import in.infosys.backend.entity.SharePermission;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
public class CredentialShareRequestDto {

    private Long credentialId;

    private String username;

    private SharePermission permission;

    private LocalDateTime expiresAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationResponseDto {

        private Long id;

        private String type;

        private String title;

        private String message;

        private boolean read;

        private LocalDateTime createdAt;

        public static NotificationResponseDto
        fromEntity(Notification notification) {

            NotificationResponseDto dto =
                    new NotificationResponseDto();

            dto.setId(notification.getId());

            dto.setType(
                    notification.getType().name()
            );

            dto.setTitle(
                    notification.getTitle()
            );

            dto.setMessage(
                    notification.getMessage()
            );

            dto.setRead(
                    notification.isRead()
            );

            dto.setCreatedAt(
                    notification.getCreatedAt()
            );

            return dto;
        }
    }
}
