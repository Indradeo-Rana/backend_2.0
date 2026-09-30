package in.infosys.backend.service;

import in.infosys.backend.dto.ForgotPasswordRequestDto;
import in.infosys.backend.dto.ResetPasswordRequestDto;
import in.infosys.backend.entity.PasswordRecoveryToken;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.PasswordRecoveryTokenRepository;
import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.util.TokenHashUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordRecoveryService {

    private static final int TOKEN_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRecoveryDeliveryService deliveryService;

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordRecoveryTokenRepository tokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   PasswordRecoveryDeliveryService deliveryService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.deliveryService = deliveryService;
    }

    @Transactional
    public void initiate(ForgotPasswordRequestDto request) {
        // Deliberately use the same generic outcome for valid/invalid accounts.
        if (request == null || request.getIdentifier() == null || request.getIdentifier().isBlank()) {
            return;
        }

        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElse(null);

        if (user == null) return;

        String rawToken = generateToken();
        PasswordRecoveryToken entity = new PasswordRecoveryToken(
                user,
                TokenHashUtil.sha256(rawToken),
                LocalDateTime.now().plusMinutes(TOKEN_MINUTES)
        );
        tokenRepository.save(entity);

        deliveryService.deliver(user.getEmail(), rawToken);
    }

    @Transactional
    public void reset(ResetPasswordRequestDto request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new IllegalArgumentException("Invalid recovery token");
        }
        validateNewPassword(request.getNewPassword());

        PasswordRecoveryToken token = tokenRepository
                .findByTokenHashAndUsedFalse(TokenHashUtil.sha256(request.getToken()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired recovery token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired recovery token");
        }

        User user = token.getUser();
        // Never read/decrypt the old password. Only replace its BCrypt hash.
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private void validateNewPassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8) {
            throw new IllegalArgumentException("New password must contain at least 8 characters");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
