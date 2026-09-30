 package in.infosys.backend.controller;

import in.infosys.backend.entity.Device;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.UserRepository;
import in.infosys.backend.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final UserRepository userRepository;

    public DeviceController(
            DeviceService deviceService,
            UserRepository userRepository
    ) {
        this.deviceService = deviceService;
        this.userRepository = userRepository;
    }

    /*
     * Get only current user's devices.
     */
    @GetMapping
    public ResponseEntity<List<Device>> getDevices() {

        User user = getCurrentUser();

        return ResponseEntity.ok(
                deviceService.getUserDevices(user)
        );
    }

    /*
     * Revoke current user's device.
     */
    @PostMapping("/{deviceId}/revoke")
    public ResponseEntity<String> revokeDevice(
            @PathVariable Long deviceId
    ) {

        User user = getCurrentUser();

        deviceService.revokeDevice(
                deviceId,
                user
        );

        return ResponseEntity.ok(
                "Device revoked successfully"
        );
    }

    /*
     * Restore current user's device.
     */
    @PostMapping("/{deviceId}/restore")
    public ResponseEntity<String> restoreDevice(
            @PathVariable Long deviceId
    ) {

        User user = getCurrentUser();

        deviceService.restoreDevice(
                deviceId,
                user
        );

        return ResponseEntity.ok(
                "Device restored successfully"
        );
    }

    private User getCurrentUser() {

        String username =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "Authenticated user not found"
                                )
                );
    }
}
