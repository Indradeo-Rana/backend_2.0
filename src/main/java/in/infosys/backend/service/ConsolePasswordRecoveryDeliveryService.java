package in.infosys.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "password.recovery.delivery", havingValue = "console", matchIfMissing = true)
public class ConsolePasswordRecoveryDeliveryService implements PasswordRecoveryDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(ConsolePasswordRecoveryDeliveryService.class);

    @Override
    public void deliver(String destination, String token) {
        log.warn("DEV password recovery token for {}: {}", destination, token);
    }
}
