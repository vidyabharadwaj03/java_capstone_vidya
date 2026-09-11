package assembly.general.api.dto.auth;

import assembly.general.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class UserSummary {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
}
