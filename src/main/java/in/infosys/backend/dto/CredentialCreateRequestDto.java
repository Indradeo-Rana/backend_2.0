package in.infosys.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialCreateRequestDto {

 private String title;

 private String username;

 private String password;

 private String website;

 private String notes;

 private String category;

 private String credentialType;

 private boolean favorite;
}