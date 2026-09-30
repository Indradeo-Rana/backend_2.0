package in.infosys.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 2000)
    private String password;

    private String website;

    @Column(length = 5000)
    private String notes;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String credentialType;

    @Column(nullable = false)
    private boolean favorite = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void setTeam(Team team) {
    }
}