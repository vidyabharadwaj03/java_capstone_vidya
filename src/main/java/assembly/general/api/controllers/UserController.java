package assembly.general.api.controllers;

import assembly.general.api.dto.user.ProfileResponse;
import assembly.general.api.entity.User;
import assembly.general.api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> profile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user));
    }
}
