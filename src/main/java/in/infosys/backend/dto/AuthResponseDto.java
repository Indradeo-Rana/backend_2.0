package in.infosys.backend.dto;

import in.infosys.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String username;
    private String email;
    private String message;
    private boolean mfaRequired;
    private String mfaChallengeId;
}
