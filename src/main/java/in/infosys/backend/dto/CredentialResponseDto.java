package in.infosys.backend.dto;

import in.infosys.backend.entity.Credential;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialResponseDto {

 private Long id;

 private String title;

 private String username;

 private String password;

 private String website;

 private String notes;

 private Boolean deleted;

 private String category;

 private String credentialType;

 private boolean favorite;

 public static CredentialResponseDto fromEntity(Credential c) {

  CredentialResponseDto dto = new CredentialResponseDto();

  dto.setId(c.getId());
  dto.setTitle(c.getTitle());
  dto.setUsername(c.getUsername());
  dto.setPassword(c.getPassword());
  dto.setWebsite(c.getWebsite());
  dto.setNotes(c.getNotes());
  dto.setDeleted(c.isDeleted());
  dto.setCategory(c.getCategory());
  dto.setCredentialType(c.getCredentialType());
  dto.setFavorite(c.isFavorite());

  return dto;
 }
}