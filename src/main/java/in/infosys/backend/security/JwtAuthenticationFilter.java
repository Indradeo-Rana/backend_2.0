package in.infosys.backend.security;

import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.service.DeviceService;
import in.infosys.backend.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenRevocationService tokenRevocationService;
    private final DeviceService deviceService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService,
            TokenRevocationService tokenRevocationService, DeviceService deviceService
    ) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.tokenRevocationService = tokenRevocationService;
        this.deviceService = deviceService;
    }

    @Override protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException { String authHeader = request.getHeader("Authorization"); /* * No JWT. * Let Spring Security handle authentication. */ if (authHeader == null || !authHeader.startsWith("Bearer ")) { filterChain.doFilter( request, response ); return; } try { String jwt = authHeader.substring(7); /* * 1. Check token revocation. */ if (tokenRevocationService.isRevoked(jwt)) { SecurityContextHolder.clearContext(); response.setStatus( HttpServletResponse.SC_UNAUTHORIZED ); response.getWriter().write( "Token has been revoked" ); return; } /* * 2. Extract username. */ String username = jwtService.extractUsername(jwt); /* * 3. Extract device ID from JWT. */ String deviceId = jwtService.extractDeviceId(jwt); /* * 4. Device must exist and must not be revoked. */ if (deviceId == null || deviceId.isBlank()) { SecurityContextHolder.clearContext(); response.setStatus( HttpServletResponse.SC_UNAUTHORIZED ); response.getWriter().write( "Device information missing" ); return; } boolean deviceRevoked = deviceService.isDeviceRevoked( username, deviceId ); if (deviceRevoked) { SecurityContextHolder.clearContext(); response.setStatus( HttpServletResponse.SC_UNAUTHORIZED ); response.getWriter().write( "Device has been revoked" ); return; } /* * 5. Load user only when not already authenticated. */ if (username != null && SecurityContextHolder .getContext() .getAuthentication() == null) { UserDetails userDetails = customUserDetailsService .loadUserByUsername( username ); /* * 6. Validate JWT. */ if (jwtService.validateToken( jwt, userDetails.getUsername() )) { UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken( userDetails, null, userDetails.getAuthorities() ); SecurityContextHolder .getContext() .setAuthentication( authentication ); } } } catch (Exception e) { SecurityContextHolder.clearContext(); response.setStatus( HttpServletResponse.SC_UNAUTHORIZED ); response.getWriter().write( "Invalid or expired token" ); return; } filterChain.doFilter( request, response ); } @Service public static class CustomUserDetailsService implements UserDetailsService { private final UserRepository userRepository; public CustomUserDetailsService( UserRepository userRepository ) { this.userRepository = userRepository; } @Override public UserDetails loadUserByUsername( String username ) throws UsernameNotFoundException { return userRepository .findByUsername(username) .map(CustomUserDetails::new) .orElseThrow( () -> new UsernameNotFoundException( "User not found" ) ); } } }