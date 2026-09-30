package in.infosys.backend.controller;

import in.infosys.backend.dto.CredentialCreateRequestDto;
import in.infosys.backend.dto.CredentialResponseDto;
import in.infosys.backend.dto.CredentialUpdateRequestDto;
import in.infosys.backend.service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credential")

public class CredentialController {

    private final CredentialService credentialService;

    public CredentialController(
            CredentialService credentialService
    ) {
        this.credentialService = credentialService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<CredentialResponseDto>
    createCredential(
            @RequestBody CredentialCreateRequestDto request
    ) {

        return ResponseEntity.ok(
                credentialService.createCredential(request)
        );
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @GetMapping
    public ResponseEntity<List<CredentialResponseDto>>
    getAllCredentials() {

        return credentialService.getAllCredentials();
    }

    // =========================================================
    // GET ONE
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<CredentialResponseDto>
    getCredentialById(
            @PathVariable Long id
    ) {

        return credentialService.getCredentialById(id);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<CredentialResponseDto>>
    searchCredentials(
            @RequestParam String keyword
    ) {

        return credentialService.searchCredentials(
                keyword
        );
    }

    // =========================================================
    // FILTER
    // =========================================================

    @GetMapping("/filter")
    public ResponseEntity<List<CredentialResponseDto>>
    filterCredentials(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String credentialType,
            @RequestParam(required = false) Boolean favorite
    ) {

        return credentialService.filterCredentials(
                category,
                credentialType,
                favorite
        );
    }

    // =========================================================
    // FAVORITE
    // =========================================================

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<CredentialResponseDto>
    toggleFavorite(
            @PathVariable Long id
    ) {

        return credentialService.toggleFavorite(id);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<CredentialResponseDto>
    updateCredential(
            @PathVariable Long id,
            @RequestBody CredentialUpdateRequestDto request
    ) {

        return credentialService.updateCredential(
                id,
                request
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String>
    deleteCredential(
            @PathVariable Long id
    ) {

        return credentialService.deleteCredential(id);
    }

    // =========================================================
    // SOFT DELETE
    // =========================================================

    @PatchMapping("/{id}/trash")
    public ResponseEntity<String>
    softDeleteCredential(
            @PathVariable Long id
    ) {

        return credentialService.softDeleteCredential(id);
    }
}