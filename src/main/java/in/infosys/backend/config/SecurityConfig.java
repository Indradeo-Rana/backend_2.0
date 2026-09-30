package in.infosys.backend.config;

import in.infosys.backend.security.JwtAuthenticationFilter;
import in.infosys.backend.security.OAuth2SuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter.CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final PasswordConfig passwordConfig;

    public SecurityConfig(
            JwtAuthenticationFilter.CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            PasswordConfig passwordConfig
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.passwordConfig = passwordConfig;
    }

    // ---------------------------------------------------------
    // Authentication Provider
    // ---------------------------------------------------------

    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(
                        customUserDetailsService
                );

        authProvider.setPasswordEncoder(
                passwordConfig.passwordEncoder()
        );

        return authProvider;
    }

    // ---------------------------------------------------------
    // Security Filter Chain
    // ---------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // -------------------------------------------------
                // CSRF
                // -------------------------------------------------

                .csrf(AbstractHttpConfigurer::disable)

                // -------------------------------------------------
                // CORS
                // -------------------------------------------------

                .cors(Customizer.withDefaults())

                // -------------------------------------------------
                // Authorization
                // -------------------------------------------------

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public authentication APIs
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/mfa/verify",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()

                        // OAuth2 endpoints
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // -------------------------------------------------
                // Session
                // -------------------------------------------------

                /*
                 * OAuth2 authorization requires a session during
                 * the Google login flow.
                 *
                 * JWT protected API requests are still authenticated
                 * through JwtAuthenticationFilter.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // -------------------------------------------------
                // Authentication Provider
                // -------------------------------------------------

                .authenticationProvider(
                        authenticationProvider()
                )

                // -------------------------------------------------
                // OAuth2 Login
                // -------------------------------------------------

                .oauth2Login(oauth2 ->
                        oauth2.successHandler(
                                oAuth2SuccessHandler
                        )
                )

                // -------------------------------------------------
                // Important:
                // Do NOT redirect /api/** requests to Google login.
                // Return 401 instead.
                // -------------------------------------------------

                .exceptionHandling(exception ->
                        exception.defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                ),
                                request ->
                                        request.getRequestURI()
                                                .startsWith("/api/")
                        )
                )

                // -------------------------------------------------
                // JWT Filter
                // -------------------------------------------------

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
