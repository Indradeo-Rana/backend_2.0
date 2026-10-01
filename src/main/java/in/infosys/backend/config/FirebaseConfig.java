package in.infosys.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${notification.push.credentials:}")
    private String credentialsPath;

    @Bean
    @ConditionalOnProperty(
            name = "notification.push.enabled",
            havingValue = "true"
    )
    public FirebaseApp firebaseApp() throws IOException {

        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException(
                    "Firebase is enabled but notification.push.credentials is empty."
            );
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (FileInputStream serviceAccount =
                     new FileInputStream(credentialsPath)) {

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(
                                    GoogleCredentials.fromStream(serviceAccount)
                            )
                            .build();

            return FirebaseApp.initializeApp(options);
        }
    }
}