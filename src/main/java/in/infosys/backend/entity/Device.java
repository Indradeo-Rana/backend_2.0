package in.infosys.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="devices")
@Getter
@Setter
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String deviceName;

    private String deviceType;

    private String browser;

    private String operatingSystem;

    private String ipAddress;

    @Column(length = 1000)
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime firstSeen;

    @Column(nullable = false)
    private LocalDateTime lastSeen;

    @Column(nullable = false)
    private boolean revoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
