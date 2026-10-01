package in.infosys.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import in.infosys.backend.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PushNotificationService {

    @Value("${notification.push.enabled:false}")
    private boolean enabled;

    public boolean send(
            User user,
            String title,
            String message
    ) {

        if (!enabled) {
            System.out.println(
                    "[PUSH DISABLED] User: "
                            + user.getUsername()
                            + " | "
                            + title
                            + " | "
                            + message
            );

            return false;
        }

        String pushToken = user.getPushToken();

        if (pushToken == null || pushToken.isBlank()) {
            throw new IllegalStateException(
                    "User does not have a registered push token"
            );
        }

        try {

            Notification notification =
                    Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build();

            Message firebaseMessage =
                    Message.builder()
                            .setToken(pushToken)
                            .setNotification(notification)
                            .build();

            FirebaseMessaging
                    .getInstance()
                    .send(firebaseMessage);

            return true;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Push notification delivery failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}