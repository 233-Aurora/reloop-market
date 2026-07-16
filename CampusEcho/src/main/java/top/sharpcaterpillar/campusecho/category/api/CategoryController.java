package top.sharpcaterpillar.campusecho.category.api;

import java.util.List;
import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.category.application.CategoryQueryService;
import top.sharpcaterpillar.campusecho.category.application.CategoryAdminService;
import top.sharpcaterpillar.campusecho.category.api.CategoryAdminResponse;
import top.sharpcaterpillar.campusecho.category.api.CategoryCreateRequest;
import top.sharpcaterpillar.campusecho.category.api.CategoryUpdateRequest;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;
    private final CategoryAdminService categoryAdminService;

    public CategoryController(CategoryQueryService categoryQueryService, CategoryAdminService categoryAdminService) {
        this.categoryQueryService = categoryQueryService;
        this.categoryAdminService = categoryAdminService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeResponse>> getTree() {
        return ApiResponse.success(categoryQueryService.getTree());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryAdminResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryAdminResponse response = categoryAdminService.create(currentUser, request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryAdminResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return ApiResponse.success(categoryAdminService.update(id, currentUser, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        categoryAdminService.delete(id, currentUser);
        return ApiResponse.success(null);
    }
}
