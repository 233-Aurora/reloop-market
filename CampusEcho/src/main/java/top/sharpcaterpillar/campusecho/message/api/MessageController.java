package top.sharpcaterpillar.campusecho.message.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.common.api.ApiResponse;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.message.application.MessageService;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;

@RestController
@Validated
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MessageResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) MessageType messageType,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0")
            @Max(value = 100, message = "每页数量不能超过 100") int size) {
        return ApiResponse.success(messageService.listMine(currentUser, unreadOnly, messageType, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(messageService.unreadCount(currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<MessageResponse> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(messageService.detail(id, currentUser));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<MessageResponse> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(messageService.markRead(id, currentUser));
    }

    @PostMapping("/read-all")
    public ApiResponse<UnreadCountResponse> markAllRead(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.success(messageService.markAllRead(currentUser));
    }
}
