package assembly.general.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserSummary user;
}
