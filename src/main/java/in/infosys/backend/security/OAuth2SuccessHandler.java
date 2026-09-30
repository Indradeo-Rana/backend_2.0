package in.infosys.backend.security;

import in.infosys.backend.entity.User;
import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.service.DeviceService;
import in.infosys.backend.service.MfaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DeviceService deviceService;
    private final MfaService mfaService;

    public OAuth2SuccessHandler(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            DeviceService deviceService,
            MfaService mfaService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.deviceService = deviceService;
        this.mfaService = mfaService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauth2User =
                (OAuth2User) authentication.getPrincipal();

        String email =
                oauth2User.getAttribute("email");

        String name =
                oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "OAuth2 account email not available"
            );
            return;
        }

        /*
         * Find existing SecureVault user by email.
         * If user does not exist, create a new SecureVault user.
         */
        User user = userRepository
                .findByEmail(email)
                .orElseGet(() ->
                        createOAuthUser(email, name)
                );

        /*
         * Device information
         *
         * OAuth2 callback does not receive the frontend's
         * X-Device-Id header directly.
         *
         * Therefore deviceId is read from the cookie
         * created by getDeviceId().
         */
        String deviceId =
                getCookieValue(
                        request,
                        "securevault_device_id"
                );
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie("securevault_device_id", deviceId);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        String userAgent =
                request.getHeader("User-Agent");

        String ipAddress =
                request.getRemoteAddr();

        /*
         * Register or update the device after successful
         * Google authentication.
         */
        if (deviceId != null && !deviceId.isBlank()) {

            deviceService.registerOrUpdateDevice(
                    user,
                    deviceId,
                    userAgent,
                    ipAddress
            );
        }

        /*
         * If SecureVault MFA is enabled, OAuth2 authentication is only the
         * primary factor. Require the same MFA challenge before issuing JWT.
         */
        if (user.isMfaEnabled()) {
            String challengeId = mfaService.createChallenge(user).getMfaChallengeId();
            String frontendMfa = frontendUrl(request) + "/mfa-verification?challengeId=" + encode(challengeId) + "&username=" + encode(user.getUsername());
            response.sendRedirect(frontendMfa);
            return;
        }

        /*
         * Convert SecureVault user into Spring UserDetails.
         */
        CustomUserDetails userDetails =
                new CustomUserDetails(user);

        /*
         * Generate normal SecureVault JWT.
         */
        String token =
                jwtService.generateToken(userDetails, deviceId);

        /*
         * Send JWT and basic user information to frontend.
         */
        String frontendUrl =
                "http://localhost:5173/oauth2/success";

        String redirectUrl =
                frontendUrl
                        + "?token="
                        + encode(token)
                        + "&username="
                        + encode(user.getUsername())
                        + "&email="
                        + encode(user.getEmail());

        response.sendRedirect(redirectUrl);
    }

    private User createOAuthUser(
            String email,
            String name
    ) {

        User user = new User();

        user.setEmail(email);

        /*
         * Username must be unique.
         * Prefer Google's name if available.
         */
        String baseUsername =
                createBaseUsername(email, name);

        String username = baseUsername;

        int counter = 1;

        while (
                userRepository
                        .findByUsername(username)
                        .isPresent()
        ) {
            username =
                    baseUsername + counter;

            counter++;
        }

        user.setUsername(username);

        /*
         * OAuth users do not authenticate using this password.
         *
         * A random BCrypt password keeps the existing
         * User model compatible without storing Google's
         * password.
         */
        String randomPassword =
                UUID.randomUUID().toString();

        user.setPassword(
                passwordEncoder.encode(randomPassword)
        );

        /*
         * MFA is disabled initially.
         * User can enable MFA later.
         */
        user.setMfaEnabled(false);

        return userRepository.save(user);
    }

    private String createBaseUsername(
            String email,
            String name
    ) {

        if (name != null && !name.isBlank()) {

            String cleaned =
                    name.toLowerCase()
                            .replaceAll(
                                    "[^a-z0-9]",
                                    ""
                            );

            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }

        String emailUsername =
                email.substring(
                        0,
                        email.indexOf("@")
                );

        String cleanedEmailUsername =
                emailUsername
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9]",
                                ""
                        );

        /*
         * Prevent an empty username.
         */
        if (cleanedEmailUsername.isBlank()) {
            return "user";
        }

        return cleanedEmailUsername;
    }

    private String getCookieValue(
            HttpServletRequest request,
            String cookieName
    ) {

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (cookieName.equals(cookie.getName())) {

                return cookie.getValue();
            }
        }

        return null;
    }

    private String frontendUrl(HttpServletRequest request) {
        return "http://localhost:5173";
    }

    private String encode(String value) {

        if (value == null) {
            return "";
        }

        try {
            return URLEncoder.encode(
                    value,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            return "";
        }
    }
}
