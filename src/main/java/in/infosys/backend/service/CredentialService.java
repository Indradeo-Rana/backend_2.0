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
import java.util.stream.Collectors;

@Service
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CredentialShareService credentialShareService;
    private final AuditLogService auditLogService;

    public CredentialService(CredentialRepository credentialRepository, UserRepository userRepository, EncryptionService encryptionService, CredentialShareService credentialShareService, AuditLogService auditLogService) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.credentialShareService = credentialShareService;
        this.auditLogService = auditLogService;
    }

    // create a new credential
    public CredentialResponseDto createCredential(
            CredentialCreateRequestDto credentialReq) {

        // get currently logged-in username from JWT/SecurityContext
        String username = Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getName();

//        System.out.println("Creating credential for logged-in user: " + username);

        // find the logged-in user from the database
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

       // System.out.println("Creating credential for user ID: " + user.getId());

        // create a new credential object and set its properties
        Credential credential = new Credential();

        credential.setTitle(credentialReq.getTitle());
        credential.setUsername(credentialReq.getUsername());
//        credential.setPassword(credentialReq.getPassword());
        // encrypt the password before saving it to the database
        credential.setPassword(
                encryptionService.encrypt(credentialReq.getPassword())
        );
        credential.setWebsite(credentialReq.getWebsite());
        credential.setNotes(encryptNote(credentialReq.getNotes()));
        credential.setCategory(credentialReq.getCategory());
        credential.setCredentialType(credentialReq.getCredentialType());
        credential.setFavorite(Boolean.TRUE.equals(credentialReq.getFavorite()));

        // set owner
        credential.setUser(user);

        // new credential is active * soft delete*
        credential.setDeleted(false);
        Credential savedCredential = credentialRepository.save(credential);

        // Create an audit log entry
        auditLogService.log(
                "CREATE",
                "CREDENTIAL",
                credential.getId(),
                "Created credential: " + credential.getTitle()
        );

        return CredentialResponseDto.fromEntity(savedCredential);
    }

    // get a credential by ID
    public ResponseEntity<CredentialResponseDto> getCredentialById(Long id) {

        // Get logged-in user from SecurityContext
        String username = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User currentUser = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // Find credential by ID
        Credential credential = credentialRepository
//                .findByIdAndUserAndDeletedFalse(id, user) // update it
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new RuntimeException("Credential not found")
                );

        // Check whether user is owner OR has active share permission
        credentialShareService.canView(credential, currentUser);

        // Convert entity to DTO
        CredentialResponseDto dto =
                CredentialResponseDto.fromEntity(credential);

        // Decrypt only after access is authorized
        String decryptedPassword =
                encryptionService.decrypt(credential.getPassword());

        dto.setPassword(decryptedPassword);
        dto.setNotes(decryptNote(credential.getNotes()));

        auditLogService.log(
                "VIEW",
                "CREDENTIAL",
                credential.getId(),
                "Viewed credential: "
                        + credential.getTitle()
        );

        return ResponseEntity.ok(dto);
    }

// get all credentials
    public ResponseEntity<List<CredentialResponseDto>> getAllCredentials() {

        String username = Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getName();

//        System.out.println("Logged-in user: " + username);

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        List<Credential> credentials =
                credentialRepository
                        .findAllByUserAndDeletedFalse(user);

        List<CredentialResponseDto> response =
                credentials.stream()
                        .map(credential -> {

                            CredentialResponseDto dto =
                                    CredentialResponseDto.fromEntity(credential);

                            dto.setPassword(
                                    encryptionService.decrypt(
                                            credential.getPassword()
                                    )
                            );
                            dto.setNotes(decryptNote(credential.getNotes()));

                            return dto;

                        })
                        .toList();

        return ResponseEntity.ok(response);

    }

    public ResponseEntity<List<CredentialResponseDto>> searchCredentials(String search, String category, String type, Boolean favorite) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        String q = search == null ? null : search.trim();
        List<CredentialResponseDto> result = credentialRepository.searchVault(user, q, category, type, favorite)
                .stream().map(c -> { CredentialResponseDto d=CredentialResponseDto.fromEntity(c);
                    d.setPassword(encryptionService.decrypt(c.getPassword())); d.setNotes(decryptNote(c.getNotes())); return d; }).toList();
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<String> toggleFavorite(Long id) {
        String username=SecurityContextHolder.getContext().getAuthentication().getName();
        User user=userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("User not found"));
        Credential c=credentialRepository.findByIdAndUserAndDeletedFalse(id,user).orElseThrow(()->new RuntimeException("Credential not found"));
        c.setFavorite(!c.isFavorite()); credentialRepository.save(c);
        return ResponseEntity.ok(c.isFavorite() ? "Credential added to favorites" : "Credential removed from favorites");
    }

    // update a credential
    public ResponseEntity<CredentialResponseDto> updateCredential(
            Long id,
            CredentialUpdateRequestDto requestDto
    ) {

        String username = Objects.requireNonNull(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .getName();

       User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // finding credential and checking if it belongs to the logged-in user
        Credential existingCredential = credentialRepository
//                .findByIdAndUserAndDeletedFalse(id, user)/*findByIdAndUser(id, user)*/
                .findByIdAndDeletedFalse(id)  // updated so shared user can access credentials
                .orElseThrow(() ->
                        new RuntimeException("Credential not found or access denied")
                );

        // check OWNER or EDIT or MANAGE permission
        credentialShareService.canEdit(
                existingCredential, currentUser);

        // update the existing credential with new values
        existingCredential.setTitle(requestDto.getTitle());
        existingCredential.setUsername(requestDto.getUsername());
//        existingCredential.setPassword(requestDto.getPassword());
        existingCredential.setPassword(
                encryptionService.encrypt(requestDto.getPassword())
        );
        existingCredential.setWebsite(requestDto.getWebsite());
        existingCredential.setNotes(encryptNote(requestDto.getNotes()));
        existingCredential.setCategory(requestDto.getCategory());
        existingCredential.setCredentialType(requestDto.getCredentialType());
        if (requestDto.getFavorite() != null) existingCredential.setFavorite(requestDto.getFavorite());

        Credential updatedCredential = credentialRepository.save(existingCredential);

        // convert to dto
        CredentialResponseDto responseDto = CredentialResponseDto.fromEntity(updatedCredential);

        // Create an audit log entry
        auditLogService.log(
                "UPDATE",
                "CREDENTIAL",
                updatedCredential.getId(),
                "Updated credential: "
                        + updatedCredential.getTitle()
        );
        return ResponseEntity.ok(responseDto);
    }

// delete a credential
    public ResponseEntity<String> deleteCredential(Long id) {

       String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

       User user = userRepository.findByUsername(username)
               .orElseThrow(() ->
                       new RuntimeException("User not found ")
               );

      Credential credential = credentialRepository. /*findByIdAndUser(id, user)*/
                findByIdAndUserAndDeletedFalse(id, user)
               .orElseThrow(() ->
                       new RuntimeException("Credential not found or access denied")
               );

                credential.setDeleted(true);
      credentialRepository.save(credential);
      // create audit log entry
        auditLogService.log(
                "DELETE",
                "CREDENTIAL",
                credential.getId(),
                "Deleted credential: "
                        + credential.getTitle()
        );
      return ResponseEntity.ok("Credential deleted successfully");
    }

    // soft delete a credential
    public ResponseEntity<String> softDeleteCredential(Long id) {

        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found ")
                );

        // finding active credentials belonging to the logged-in user
        Credential credential = credentialRepository. /*findByIdAndUser(id, user)*/
                findByIdAndUserAndDeletedFalse(id, user)
                .orElseThrow(() ->
                        new RuntimeException("Credential not found or access denied")
                );

        // mark the credential as deleted --> soft-delete
        credential.setDeleted(true);
        credentialRepository.save(credential);

        // Create an audit log entry
        auditLogService.log(
                "DELETE",
                "CREDENTIAL",
                credential.getId(),
                "Deleted credential: "
                        + credential.getTitle()
        );

        return ResponseEntity.ok("Credential moved to trash successfully");
    }


    // get all soft deleted credentials
    public ResponseEntity<List<CredentialResponseDto>> getAllSoftDeletedCredentials() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found ")
                );

        List<Credential> softDeletedCredentials = credentialRepository
                .findAllByUserAndDeletedTrue(user);

        List<CredentialResponseDto> responseDtos = softDeletedCredentials
                .stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    private String encryptNote(String note) {
        if (note == null || note.isBlank()) return note;
        return note.startsWith("ENC:") ? note : "ENC:" + encryptionService.encrypt(note);
    }

    private String decryptNote(String note) {
        if (note == null || note.isBlank()) return note;
        if (!note.startsWith("ENC:")) return note;
        return encryptionService.decrypt(note.substring(4));
    }

    private CredentialResponseDto convertToResponseDto(Credential credential) {
        CredentialResponseDto dto = CredentialResponseDto.fromEntity(credential);
        dto.setNotes(decryptNote(credential.getNotes()));
        return dto;
    }
}
