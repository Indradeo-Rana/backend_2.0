package in.infosys.backend.repository;

import in.infosys.backend.entity.PasswordRecoveryToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByTokenHashAndUsedFalse(String tokenHash);
}
