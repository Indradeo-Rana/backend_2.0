package in.infosys.backend.service;

import in.infosys.backend.dto.AuthResponseDto;
import in.infosys.backend.dto.MfaSetupResponseDto;
import in.infosys.backend.dto.MfaVerifyRequestDto;
import in.infosys.backend.entity.MfaChallenge;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.MfaChallengeRepository;
import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.security.CustomUserDetails;
import in.infosys.backend.security.JwtService;
import in.infosys.backend.util.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MfaService {

    private static final int CHALLENGE_MINUTES = 5;

    private final UserRepository userRepository;
    private final MfaChallengeRepository challengeRepository;
    private final JwtService jwtService;
    private final DeviceService deviceService;

    public MfaService(UserRepository userRepository,
                      MfaChallengeRepository challengeRepository,
                      JwtService jwtService, DeviceService deviceService) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.jwtService = jwtService;
        this.deviceService = deviceService;
    }

    public MfaSetupResponseDto setup() {

        User user = currentUser();

        // If MFA is already enabled, do not disable or regenerate anything
        if (user.isMfaEnabled()) {

            String secret = user.getMfaSecret();

            String label = user.getEmail() == null || user.getEmail().isBlank()
                    ? user.getUsername()
                    : user.getEmail();

            String issuer = "SecureVault";

            String uri = "otpauth://totp/" + issuer + ":" + label
                    + "?secret=" + secret
                    + "&issuer=" + issuer
                    + "&algorithm=SHA1"
                    + "&digits=6"
                    + "&period=30";

            return new MfaSetupResponseDto(
                    secret,
                    uri,
                    true
            );
        }

        // MFA is not enabled
        // Reuse existing secret if one already exists
        String secret = user.getMfaSecret();

        if (secret == null || secret.isBlank()) {
            secret = TotpUtil.generateSecret();
            user.setMfaSecret(secret);
            userRepository.save(user);
        }

        String label = user.getEmail() == null || user.getEmail().isBlank()
                ? user.getUsername()
                : user.getEmail();

        String issuer = "SecureVault";

        String uri = "otpauth://totp/" + issuer + ":" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";

        return new MfaSetupResponseDto(
                secret,
                uri,
                false
        );
    }
    public boolean enable(String code) {
        User user = currentUser();
        if (user.getMfaSecret() == null || !TotpUtil.verifyCode(
                user.getMfaSecret(), code)
        ) {
            throw new BadCredentialsException("Invalid MFA code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        return true;
    }

    public boolean disable(String code) {
        User user = currentUser();
        if (!user.isMfaEnabled() || !TotpUtil.verifyCode(user.getMfaSecret(), code)) {
            throw new BadCredentialsException("Invalid MFA code");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public AuthResponseDto createChallenge(User user) {
        String challengeId = UUID.randomUUID().toString();
        MfaChallenge challenge = new MfaChallenge(
                challengeId,
                user,
                LocalDateTime.now().plusMinutes(CHALLENGE_MINUTES)
        );
        challengeRepository.save(challenge);

        return new AuthResponseDto(
                null,
                user.getUsername(),
                user.getEmail(),
                "MFA verification required",
                true,
                challengeId
        );
    }

    @Transactional
    public AuthResponseDto verify(
            MfaVerifyRequestDto request,
            HttpServletRequest httpRequest
    ) {
        if (request == null ||
                request.getChallengeId() == null ||
                request.getCode() == null) {

            throw new BadCredentialsException("MFA verification is required");
        }

        MfaChallenge challenge = challengeRepository
                .findByChallengeIdAndUsedFalse(request.getChallengeId())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid or expired MFA challenge"
                        )
                );

        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException(
                    "Invalid or expired MFA challenge"
            );
        }

        User user = challenge.getUser();

        if (!user.isMfaEnabled() ||
                !TotpUtil.verifyCode(
                        user.getMfaSecret(),
                        request.getCode()
                )) {

            throw new BadCredentialsException("Invalid MFA code");
        }

        challenge.setUsed(true);
        challengeRepository.save(challenge);

        // Generate JWT only after successful MFA verification
        String deviceId = httpRequest.getHeader("X-Device-Id");
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }
        String token =
                jwtService.generateToken(
                        new CustomUserDetails(user),
                        deviceId
                );

        // Device information

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();

        // Register/update the successfully authenticated device
        deviceService.registerOrUpdateDevice(
                user,
                deviceId,
                userAgent,
                ipAddress
        );

        return new AuthResponseDto(
                token,
                user.getUsername(),
                user.getEmail(),
                "Login successful",
                false,
                null
        );
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
