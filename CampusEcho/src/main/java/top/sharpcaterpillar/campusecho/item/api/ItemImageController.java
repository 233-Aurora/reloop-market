package top.sharpcaterpillar.campusecho.item.api;

import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.item.application.ItemImageService;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/items")
public class ItemImageController {

    private final ItemImageService itemImageService;

    public ItemImageController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    @GetMapping("/{itemId}/images")
    public ApiResponse<List<ItemImageResponse>> list(@PathVariable Long itemId) {
        return ApiResponse.success(itemImageService.listPublic(itemId));
    }

    @PostMapping(value = "/{itemId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemImageResponse>> upload(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primary) {
        ItemImageResponse response = itemImageService.upload(itemId, currentUser, file, primary);
        return ResponseEntity.created(URI.create(response.url())).body(ApiResponse.success(response));
    }

    @PostMapping("/{itemId}/images/{imageId}/primary")
    public ApiResponse<ItemImageResponse> makePrimary(
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(itemImageService.makePrimary(itemId, imageId, currentUser));
    }

    @DeleteMapping("/{itemId}/images/{imageId}")
    public ApiResponse<Void> delete(
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        itemImageService.delete(itemId, imageId, currentUser);
        return ApiResponse.success(null);
    }

}
