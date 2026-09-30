package in.infosys.backend.service;

import in.infosys.backend.entity.Device;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.DeviceRepository;
import in.infosys.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public DeviceService(
            DeviceRepository deviceRepository,
            UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;

    }

    public Device registerOrUpdateDevice(
            User user,
            String deviceId,
            String userAgent,
            String ipAddress
    ) {

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                    "Device ID is required"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        Device device =
                deviceRepository
                        .findByDeviceIdAndUser(deviceId, user)
                        .orElse(null);

        if (device == null) {

            device = new Device();

            device.setDeviceId(deviceId);
            device.setUser(user);
            device.setFirstSeen(now);
            device.setRevoked(false);

        } else {

            // A revoked device should not
            // automatically become active again.
            if (device.isRevoked()) {
                return device;
            }
        }

        device.setUserAgent(userAgent);
        device.setIpAddress(ipAddress);
        device.setLastSeen(now);

        detectDeviceInformation(device, userAgent);

        return deviceRepository.save(device);
    }

    public List<Device> getUserDevices(User user) {

        return deviceRepository
                .findByUserOrderByLastSeenDesc(user);
    }

    public void revokeDevice(
            Long deviceId,
            User user
    ) {

        Device device =
                deviceRepository
                        .findByIdAndUser(deviceId, user)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Device not found"
                                )
                        );

        device.setRevoked(true);

        deviceRepository.save(device);
    }

    public void restoreDevice(
            Long deviceId,
            User user
    ) {

        Device device =
                deviceRepository
                        .findByIdAndUser(deviceId, user)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Device not found"
                                )
                        );

        device.setRevoked(false);

        deviceRepository.save(device);
    }

    private void detectDeviceInformation(
            Device device,
            String userAgent
    ) {

        if (userAgent == null || userAgent.isBlank()) {

            device.setDeviceName("Unknown Device");
            device.setDeviceType("Unknown");
            device.setBrowser("Unknown");
            device.setOperatingSystem("Unknown");

            return;
        }

        device.setDeviceName(detectDeviceName(userAgent));
        device.setDeviceType(detectDeviceType(userAgent));
        device.setBrowser(detectBrowser(userAgent));
        device.setOperatingSystem(detectOperatingSystem(userAgent));
    }

    private String detectDeviceName(String userAgent) {

        if (userAgent.contains("Mobile")) {
            return "Mobile Device";
        }

        return "Desktop";
    }

    private String detectDeviceType(String userAgent) {

        if (userAgent.contains("Mobile")) {
            return "Mobile";
        }

        if (userAgent.contains("Tablet")) {
            return "Tablet";
        }

        return "Desktop";
    }

    private String detectBrowser(String userAgent) {

        if (userAgent.contains("Edg")) {
            return "Microsoft Edge";
        }

        if (userAgent.contains("Chrome")) {
            return "Google Chrome";
        }

        if (userAgent.contains("Firefox")) {
            return "Mozilla Firefox";
        }

        if (userAgent.contains("Safari")) {
            return "Safari";
        }

        return "Unknown";
    }

    private String detectOperatingSystem(
            String userAgent
    ) {

        if (userAgent.contains("Windows")) {
            return "Windows";
        }

        if (userAgent.contains("Mac OS")) {
            return "macOS";
        }

        if (userAgent.contains("Android")) {
            return "Android";
        }

        if (
                userAgent.contains("iPhone") ||
                        userAgent.contains("iPad")
        ) {
            return "iOS";
        }

        if (userAgent.contains("Linux")) {
            return "Linux";
        }

        return "Unknown";
    }

    public boolean isDeviceRevoked( String username, String deviceId ) {
        if (username == null || username.isBlank() || deviceId == null || deviceId.isBlank()
        ) {
            return true;
        }
        User user = userRepository
                .findByUsername(username)
                .orElse(null);
        if (user == null) {
            return true;
        }
        return deviceRepository .findByDeviceIdAndUser( deviceId, user)
                .map(Device::isRevoked)
            .orElse(true);
    }

}

