package in.infosys.backend.repository;

import in.infosys.backend.entity.User;
import in.infosys.backend.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository
        extends JpaRepository<UserDeviceToken, Long> {

    List<UserDeviceToken>
    findAllByUser(User user);

    Optional<UserDeviceToken>
    findByToken(String token);

    void deleteByToken(String token);
}