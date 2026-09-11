package assembly.general.api.dto.user;

import assembly.general.api.entity.MembershipStatus;
import assembly.general.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private MembershipStatus membershipStatus;
    private Instant memberSince;
    private long activeReservations;
    private long borrowingHistory;
}
