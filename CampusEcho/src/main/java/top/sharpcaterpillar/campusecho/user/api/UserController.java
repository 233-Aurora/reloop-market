package top.sharpcaterpillar.campusecho.user.api;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.user.application.UserProfileService;
import top.sharpcaterpillar.campusecho.user.application.UserRegistrationService;
import top.sharpcaterpillar.campusecho.user.application.AuthenticationService;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserRegistrationService userRegistrationService;
    private final UserProfileService userProfileService;
    private final AuthenticationService authenticationService;

    public UserController(
            UserRegistrationService userRegistrationService,
            UserProfileService userProfileService,
            AuthenticationService authenticationService) {
        this.userRegistrationService = userRegistrationService;
        this.userProfileService = userProfileService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserProfileResponse response = userRegistrationService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authenticationService.login(request));
    }

    @GetMapping("/users/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return ApiResponse.success(userProfileService.getCurrentUser(authenticatedUser));
    }

    @PutMapping("/users/me")
    public ApiResponse<CurrentUserResponse> updateCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userProfileService.updateCurrentUser(authenticatedUser, request));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserProfileResponse> getPublicProfile(@PathVariable Long id) {
        return ApiResponse.success(userProfileService.getPublicProfile(id));
    }
}
