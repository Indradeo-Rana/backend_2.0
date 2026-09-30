package in.infosys.backend.service;

import in.infosys.backend.dto.CredentialCreateRequestDto;
import in.infosys.backend.dto.CredentialResponseDto;
import in.infosys.backend.dto.CredentialUpdateRequestDto;
import in.infosys.backend.entity.Credential;
import in.infosys.backend.entity.User;
import in.infosys.backend.repository.CredentialRepository;
import in.infosys.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CredentialShareService credentialShareService;
    private final AuditLogService auditLogService;

    public CredentialService(
            CredentialRepository credentialRepository,
            UserRepository userRepository,
            EncryptionService encryptionService,
            CredentialShareService credentialShareService,
            AuditLogService auditLogService
    ) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.credentialShareService = credentialShareService;
        this.auditLogService = auditLogService;
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    private User getCurrentUser() {

        String username = Objects.requireNonNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).getName();

        return userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );
    }

    // =========================================================
    // CREATE
    // =========================================================

    public CredentialResponseDto createCredential(
            CredentialCreateRequestDto request
    ) {

        if (request == null) {
            throw new RuntimeException(
                    "Credential data is required"
            );
        }

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {

            throw new RuntimeException(
                    "Title is required"
            );
        }

        User user = getCurrentUser();

        String credentialType = request.getCredentialType();

        if (credentialType == null ||
                credentialType.isBlank()) {

            credentialType = "WEBSITE_LOGIN";
        }

        // Secure Note does not require username/password
        boolean secureNote =
                "SECURE_NOTE".equalsIgnoreCase(credentialType);

        if (!secureNote &&
                (request.getUsername() == null ||
                        request.getUsername().isBlank())) {

            throw new RuntimeException(
                    "Username is required"
            );
        }

        if (!secureNote &&
                (request.getPassword() == null ||
                        request.getPassword().isBlank())) {

            throw new RuntimeException(
                    "Password is required"
            );
        }

        Credential credential = new Credential();

        credential.setUser(user);

        credential.setTitle(
                request.getTitle()
        );

        credential.setUsername(
                request.getUsername()
        );

        // Password encryption
        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            credential.setPassword(
                    encryptionService.encrypt(
                            request.getPassword()
                    )
            );

        } else {

            // Secure Note does not need password
            credential.setPassword("");
        }

        credential.setWebsite(
                request.getWebsite()
        );

        // Notes encryption
        if (request.getNotes() != null &&
                !request.getNotes().isBlank()) {

            credential.setNotes(
                    encryptionService.encrypt(
                            request.getNotes()
                    )
            );

        } else {

            credential.setNotes("");
        }

        credential.setCategory(
                request.getCategory()
        );

        credential.setCredentialType(
                credentialType
        );

        credential.setFavorite(
                request.isFavorite()
        );

        credential.setDeleted(false);

        Credential savedCredential =
                credentialRepository.save(credential);

        auditLogService.log(
                "CREATE",
                "CREDENTIAL",
                savedCredential.getId(),
                "Created credential: "
                        + savedCredential.getTitle()
        );

        // IMPORTANT:
        // Return decrypted response, not encrypted DB value
        return toResponse(savedCredential);
    }

    // =========================================================
    // GET ONE
    // =========================================================

    public ResponseEntity<CredentialResponseDto>
    getCredentialById(Long id) {

        User currentUser = getCurrentUser();

        Credential credential =
                credentialRepository
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found"
                                )
                        );

        // Owner/shared-user authorization
        credentialShareService.canView(
                credential,
                currentUser
        );

        /*
         * Authorization is checked BEFORE decrypting
         * sensitive information.
         */
        CredentialResponseDto dto =
                toResponse(credential);

        auditLogService.log(
                "VIEW",
                "CREDENTIAL",
                credential.getId(),
                "Viewed credential: "
                        + credential.getTitle()
        );

        return ResponseEntity.ok(dto);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    public ResponseEntity<List<CredentialResponseDto>>
    getAllCredentials() {

        User user = getCurrentUser();

        List<Credential> credentials =
                credentialRepository
                        .findAllByUserAndDeletedFalse(user);

        return ResponseEntity.ok(
                credentials.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public ResponseEntity<List<CredentialResponseDto>>
    searchCredentials(String keyword) {

        User user = getCurrentUser();

        if (keyword == null ||
                keyword.isBlank()) {

            return getAllCredentials();
        }

        List<Credential> credentials =
                credentialRepository.searchCredentials(
                        user,
                        keyword.trim()
                );

        return ResponseEntity.ok(
                credentials.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    // =========================================================
    // FILTER
    // =========================================================

    public ResponseEntity<List<CredentialResponseDto>>
    filterCredentials(
            String category,
            String credentialType,
            Boolean favorite
    ) {

        User user = getCurrentUser();

        List<Credential> credentials;

        // Category + Type
        if (category != null &&
                !category.isBlank() &&
                credentialType != null &&
                !credentialType.isBlank()) {

            credentials =
                    credentialRepository
                            .findAllByUserAndCategoryIgnoreCaseAndCredentialTypeIgnoreCaseAndDeletedFalse(
                                    user,
                                    category,
                                    credentialType
                            );

        }

        // Category only
        else if (category != null &&
                !category.isBlank()) {

            credentials =
                    credentialRepository
                            .findAllByUserAndCategoryIgnoreCaseAndDeletedFalse(
                                    user,
                                    category
                            );

        }

        // Type only
        else if (credentialType != null &&
                !credentialType.isBlank()) {

            credentials =
                    credentialRepository
                            .findAllByUserAndCredentialTypeIgnoreCaseAndDeletedFalse(
                                    user,
                                    credentialType
                            );

        }

        // Favorite only
        else if (favorite != null) {

            credentials =
                    credentialRepository
                            .findAllByUserAndFavoriteAndDeletedFalse(
                                    user,
                                    favorite
                            );

        }

        // No filter
        else {

            credentials =
                    credentialRepository
                            .findAllByUserAndDeletedFalse(
                                    user
                            );
        }

        // Favorite + Category
        if (favorite != null &&
                category != null &&
                !category.isBlank()) {

            credentials =
                    credentials.stream()
                            .filter(c ->
                                    c.isFavorite() == favorite
                            )
                            .toList();
        }

        // Favorite + Type
        if (favorite != null &&
                credentialType != null &&
                !credentialType.isBlank() &&
                (category == null ||
                        category.isBlank())) {

            credentials =
                    credentials.stream()
                            .filter(c ->
                                    c.isFavorite() == favorite
                            )
                            .toList();
        }

        return ResponseEntity.ok(
                credentials.stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    // =========================================================
    // FAVORITE TOGGLE
    // =========================================================

    public ResponseEntity<CredentialResponseDto>
    toggleFavorite(Long id) {

        User user = getCurrentUser();

        // Favorite belongs to owner's vault.
        // Shared users cannot change owner's favorite state.
        Credential credential =
                credentialRepository
                        .findByIdAndUserAndDeletedFalse(
                                id,
                                user
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found or access denied"
                                )
                        );

        credential.setFavorite(
                !credential.isFavorite()
        );

        Credential saved =
                credentialRepository.save(
                        credential
                );

        auditLogService.log(
                "UPDATE",
                "CREDENTIAL",
                saved.getId(),
                "Favorite status changed for credential: "
                        + saved.getTitle()
        );

        return ResponseEntity.ok(
                toResponse(saved)
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public ResponseEntity<CredentialResponseDto>
    updateCredential(
            Long id,
            CredentialUpdateRequestDto request
    ) {

        User currentUser = getCurrentUser();

        Credential existing =
                credentialRepository
                        .findByIdAndDeletedFalse(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found or access denied"
                                )
                        );

        // Existing sharing permission logic preserved
        credentialShareService.canEdit(
                existing,
                currentUser
        );

        if (request.getTitle() != null &&
                !request.getTitle().isBlank()) {

            existing.setTitle(
                    request.getTitle()
            );
        }

        if (request.getUsername() != null) {

            existing.setUsername(
                    request.getUsername()
            );
        }

        // Only encrypt when a new password is supplied
        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            existing.setPassword(
                    encryptionService.encrypt(
                            request.getPassword()
                    )
            );
        }

        existing.setWebsite(
                request.getWebsite()
        );

        // Notes encryption
        if (request.getNotes() != null &&
                !request.getNotes().isBlank()) {

            existing.setNotes(
                    encryptionService.encrypt(
                            request.getNotes()
                    )
            );

        } else {

            existing.setNotes("");
        }

        if (request.getCategory() != null) {

            existing.setCategory(
                    request.getCategory()
            );
        }

        if (request.getCredentialType() != null &&
                !request.getCredentialType().isBlank()) {

            existing.setCredentialType(
                    request.getCredentialType()
            );
        }

        existing.setFavorite(
                request.isFavorite()
        );

        Credential updated =
                credentialRepository.save(
                        existing
                );

        auditLogService.log(
                "UPDATE",
                "CREDENTIAL",
                updated.getId(),
                "Updated credential: "
                        + updated.getTitle()
        );

        return ResponseEntity.ok(
                toResponse(updated)
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    public ResponseEntity<String>
    deleteCredential(Long id) {

        User user = getCurrentUser();

        Credential credential =
                credentialRepository
                        .findByIdAndUserAndDeletedFalse(
                                id,
                                user
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found or access denied"
                                )
                        );

        credentialRepository.delete(
                credential
        );

        return ResponseEntity.ok(
                "Credential deleted successfully"
        );
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    public ResponseEntity<String>
    softDeleteCredential(Long id) {

        User user = getCurrentUser();

        Credential credential =
                credentialRepository
                        .findByIdAndUserAndDeletedFalse(
                                id,
                                user
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Credential not found or access denied"
                                )
                        );

        credential.setDeleted(true);

        credentialRepository.save(
                credential
        );

        auditLogService.log(
                "DELETE",
                "CREDENTIAL",
                credential.getId(),
                "Deleted credential: "
                        + credential.getTitle()
        );

        return ResponseEntity.ok(
                "Credential moved to trash successfully"
        );
    }

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================

    private CredentialResponseDto toResponse(
            Credential credential
    ) {

        CredentialResponseDto dto =
                CredentialResponseDto.fromEntity(
                        credential
                );

        // -----------------------------------------------------
        // PASSWORD
        // -----------------------------------------------------

        if (credential.getPassword() != null &&
                !credential.getPassword().isBlank()) {

            dto.setPassword(
                    encryptionService.decrypt(
                            credential.getPassword()
                    )
            );
        }

        // -----------------------------------------------------
        // NOTES
        // -----------------------------------------------------

        if (credential.getNotes() != null &&
                !credential.getNotes().isBlank()) {

            dto.setNotes(
                    encryptionService.decrypt(
                            credential.getNotes()
                    )
            );
        }

        return dto;
    }
}