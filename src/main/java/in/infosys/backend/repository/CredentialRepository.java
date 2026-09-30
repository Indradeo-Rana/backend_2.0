package in.infosys.backend.repository;

import in.infosys.backend.entity.Credential;
import in.infosys.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository
        extends JpaRepository<Credential, Long> {

    List<Credential> findAllByUserAndDeletedFalse(User user);

    Optional<Credential> findByIdAndDeletedFalse(Long id);

    Optional<Credential> findByIdAndUserAndDeletedFalse(
            Long id,
            User user
    );

    // =========================
    // SEARCH
    // =========================

    @Query("""
            SELECT c
            FROM Credential c
            WHERE c.user = :user
            AND c.deleted = false
            AND (
                LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(c.website, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(c.credentialType, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY c.id DESC
            """)
    List<Credential> searchCredentials(
            @Param("user") User user,
            @Param("keyword") String keyword
    );

    // =========================
    // CATEGORY FILTER
    // =========================

    List<Credential> findAllByUserAndCategoryIgnoreCaseAndDeletedFalse(
            User user,
            String category
    );

    // =========================
    // TYPE FILTER
    // =========================

    List<Credential> findAllByUserAndCredentialTypeIgnoreCaseAndDeletedFalse(
            User user,
            String credentialType
    );

    // =========================
    // FAVORITE FILTER
    // =========================

    List<Credential> findAllByUserAndFavoriteAndDeletedFalse(
            User user,
            boolean favorite
    );

    // =========================
    // CATEGORY + TYPE
    // =========================

    List<Credential>
    findAllByUserAndCategoryIgnoreCaseAndCredentialTypeIgnoreCaseAndDeletedFalse(
            User user,
            String category,
            String credentialType
    );
}