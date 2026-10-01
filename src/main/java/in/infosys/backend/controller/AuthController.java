package in.infosys.backend.controller;

import in.infosys.backend.dto.*;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.security.JwtService;
import in.infosys.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final LoginActivityService loginActivityService;
    private final SecurityAlertService securityAlertService;
    private final MfaService mfaService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;
    private final DeviceService deviceService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AuthController(UserService userService, LoginActivityService loginActivityService, SecurityAlertService securityAlertService, MfaService mfaService, PasswordRecoveryService passwordRecoveryService, JwtService jwtService, TokenRevocationService tokenRevocationService, DeviceService deviceService, UserRepository userRepository, NotificationService notificationService) {
        this.userService = userService;
        this.loginActivityService = loginActivityService;
        this.securityAlertService = securityAlertService;
        this.mfaService = mfaService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
        this.deviceService = deviceService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // POST --> /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<User> createUser(@RequestBody RegisterRequestDto request){
        User createdUser = userService.registerUser(request);
        return ResponseEntity
                .status(201)
                .body(createdUser);
    }

// updated login method to record login activity
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();

        try {

            String deviceId = httpRequest.getHeader("X-Device-Id");

            AuthResponseDto response = userService.login( request, deviceId, httpRequest );

            if (!response.isMfaRequired()) {

                userRepository.findByUsername(response.getUsername()).ifPresent(user ->
                        notificationService.loginSuccess(user, ipAddress));

                loginActivityService.recordLogin(
                        response.getUsername(),
                        true,
                        ipAddress
                );
            }

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {

            loginActivityService.recordLogin(
                    request == null
                            ? null
                            : request.getUsername(),
                    false,
                    ipAddress
            );

           if(request != null && request.getUsername() != null) {

               String identifier = request.getUsername().trim();

               userRepository.findByUsername(identifier)
                       .or(() -> userRepository.findByEmail(identifier))
                       .ifPresent(user -> notificationService
                               .loginFailure(user, ipAddress)
                       );
           }

            securityAlertService.checkForSuspiciousActivity(
                    request == null
                            ? null
                            : request.getUsername(),
                    ipAddress
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            new AuthResponseDto(
                                    null,
                                    null,
                                    null,
                                    "Invalid username or password",
                                    false,
                                    null
                            )
                    );
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<AuthResponseDto> verifyMfa(
            @RequestBody MfaVerifyRequestDto request,
            HttpServletRequest httpRequest) {

        try {
            AuthResponseDto response = mfaService.verify(request, httpRequest);

            loginActivityService.recordLogin(
                    response.getUsername(),
                    true,
                    httpRequest.getRemoteAddr()
            );
            userRepository.findByUsername(response.getUsername()).ifPresent(user ->
                    notificationService.loginSuccess(user, httpRequest.getRemoteAddr()));

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponseDto> setupMfa() {
        return ResponseEntity.ok(mfaService.setup());
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<String> enableMfa(@RequestBody MfaCodeRequestDto request) {
        mfaService.enable(request == null ? null : request.getCode());

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        userRepository.findByUsername(username)
                .ifPresent(user ->
                        notificationService.securityNotification(
                                user,
                                "MFA enabled",
                                "MFA has been enabled on your account."
                        )
                        );
        return ResponseEntity.ok("MFA enabled");
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<String> disableMfa(@RequestBody MfaCodeRequestDto request) {
        mfaService.disable(request == null ? null : request.getCode());

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        userRepository.findByUsername(username)
                .ifPresent(user ->
                        notificationService.securityNotification(
                                user,
                                "MFA disabled",
                                "MFA has been disabled on your account."
                        )
                );
        return ResponseEntity.ok("MFA disabled");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        passwordRecoveryService.initiate(request);
        return ResponseEntity.ok("If the account exists, a recovery mechanism has been issued.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        try {
            passwordRecoveryService.reset(request);
            return ResponseEntity.ok("Password reset successful");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout") public ResponseEntity<?> logout(
            HttpServletRequest request ) {
        String authHeader = request.getHeader("Authorization");
        if ( authHeader == null || !authHeader.startsWith("Bearer ")
        ) {
            return ResponseEntity .badRequest() .body("No token provided");
        }
        String token = authHeader.substring(7);
        try {
            Date expiration = jwtService.extractExpiration(token);
            tokenRevocationService.revokeToken( token, expiration );
            return ResponseEntity.ok( "Logout successful" );
        } catch (Exception e) {
        return ResponseEntity .status(HttpStatus.UNAUTHORIZED)
                .body("Invalid or expired token");
        }
    }

}
