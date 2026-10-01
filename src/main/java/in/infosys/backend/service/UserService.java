package in.infosys.backend.service;

import in.infosys.backend.dto.AuthResponseDto;
import in.infosys.backend.dto.LoginRequestDto;
import in.infosys.backend.dto.RegisterRequestDto;
import in.infosys.backend.entity.User;
import in.infosys.backend.entity.ApplicationRole;
import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.security.CustomUserDetails;
import in.infosys.backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final DeviceService deviceService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MfaService mfaService,
            DeviceService deviceService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mfaService = mfaService;
        this.deviceService = deviceService;
    }

    public User registerUser(RegisterRequestDto request) {

        if (request == null
                || request.getPassword() == null
                || request.getPassword().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setRole(ApplicationRole.USER);

        return userRepository.save(user);
    }

    public AuthResponseDto login(
            LoginRequestDto request,
            String deviceId,
            HttpServletRequest httpRequest
    ) {

        if (request == null ||
                request.getUsername() == null ||
                request.getPassword() == null) {

            throw new BadCredentialsException(
                    "Username and password are required"
            );
        }

        String identifier =
                request.getUsername().trim();

        User user = userRepository
                .findByUsername(identifier)
                .or(() ->
                        userRepository.findByEmail(identifier)
                )
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid username or password"
                        )
                );

        String storedPassword =
                user.getPassword();

        if (storedPassword == null ||
                storedPassword.isBlank()) {

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        if (!isBcryptHash(storedPassword)) {

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                storedPassword
        )) {

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        // -------------------------------------------------
        // MFA
        // -------------------------------------------------

        if (user.isMfaEnabled()) {

            return mfaService.createChallenge(user);
        }

        // -------------------------------------------------
        // Device Information
        // -------------------------------------------------

        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
        }

        String userAgent =
                httpRequest.getHeader("User-Agent");

        String ipAddress =
                httpRequest.getRemoteAddr();

        // -------------------------------------------------
        // Register / Update Device
        // -------------------------------------------------

        deviceService.registerOrUpdateDevice(
                user,
                deviceId,
                userAgent,
                ipAddress
        );

        // -------------------------------------------------
        // Generate JWT
        // -------------------------------------------------

        String token =
                jwtService.generateToken(
                        new CustomUserDetails(user),
                        deviceId
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

    private boolean isBcryptHash(String value) {

        return value != null &&
                value.matches(
                        "\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}"
                );
    }
}

