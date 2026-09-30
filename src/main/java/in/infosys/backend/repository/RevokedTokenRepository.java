package in.infosys.backend.repository;

import in.infosys.backend.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RevokedTokenRepository
        extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    Optional<RevokedToken> findByTokenHash(String tokenHash);
}