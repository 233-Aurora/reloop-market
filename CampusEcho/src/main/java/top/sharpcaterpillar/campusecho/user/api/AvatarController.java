package top.sharpcaterpillar.campusecho.user.api;

import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.application.AvatarContent;
import top.sharpcaterpillar.campusecho.user.application.AvatarService;

@RestController
@RequestMapping("/api/v1")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CurrentUserResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(avatarService.upload(currentUser, file));
    }

    @DeleteMapping("/users/me/avatar")
    public ApiResponse<CurrentUserResponse> delete(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(avatarService.delete(currentUser));
    }

    @GetMapping("/avatars/{storageKey}")
    public ResponseEntity<Resource> content(@PathVariable String storageKey) {
        AvatarContent content = avatarService.content(storageKey);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic().immutable())
                .contentType(content.mediaType())
                .body(new FileSystemResource(content.path()));
    }
}
