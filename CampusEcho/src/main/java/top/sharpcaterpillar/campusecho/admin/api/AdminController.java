package top.sharpcaterpillar.campusecho.admin.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.admin.application.AdminService;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(adminService.dashboard(currentUser));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserResponse>> listUsers(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(adminService.listUsers(currentUser, keyword, role, status, page, size));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> userDetail(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(adminService.userDetail(userId, currentUser));
    }

    @PatchMapping("/users/{userId}/status")
    public ApiResponse<AdminUserResponse> changeStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        return ApiResponse.success(adminService.changeStatus(userId, request.status(), currentUser));
    }

    @PatchMapping("/users/{userId}/role")
    public ApiResponse<AdminUserResponse> changeRole(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return ApiResponse.success(adminService.changeRole(userId, request.role(), currentUser));
    }
}
