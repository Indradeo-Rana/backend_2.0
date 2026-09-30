package in.infosys.backend.service;

import in.infosys.backend.entity.RevokedToken;
import in.infosys.backend.repository.RevokedTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationService(
            RevokedTokenRepository revokedTokenRepository
    ) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public void revokeToken(String token, Date expiration) {

        String tokenHash = hashToken(token);

        LocalDateTime expiresAt =
                expiration.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

        if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {

            RevokedToken revokedToken =
                    new RevokedToken(tokenHash, expiresAt);

            revokedTokenRepository.save(revokedToken);
        }
    }

    public boolean isRevoked(String token) {

        String tokenHash = hashToken(token);

        return revokedTokenRepository
                .existsByTokenHash(tokenHash);
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hash) {

                String hex =
                        Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to hash token",
                    e
            );
        }
    }
}