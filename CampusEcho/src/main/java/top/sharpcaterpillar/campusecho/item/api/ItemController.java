package top.sharpcaterpillar.campusecho.item.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.item.application.ItemService;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ItemCreateRequest request) {
        ItemResponse response = itemService.create(currentUser, request);
        return ResponseEntity.created(URI.create("/api/v1/items/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping
    public ApiResponse<PageResponse<ItemResponse>> listPublic(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(itemService.listPublic(categoryId, keyword, page, size));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ItemResponse>> getMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(itemService.getMine(currentUser, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemResponse> getPublicDetail(@PathVariable Long id) {
        return ApiResponse.success(itemService.getPublicDetail(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ItemResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody ItemUpdateRequest request) {
        return ApiResponse.success(itemService.update(id, currentUser, request));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<ItemResponse> offShelf(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(itemService.offShelf(id, currentUser));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ItemResponse> publish(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(itemService.publish(id, currentUser));
    }
}
