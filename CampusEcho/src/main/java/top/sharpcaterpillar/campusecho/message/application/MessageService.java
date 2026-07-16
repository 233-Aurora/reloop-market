package top.sharpcaterpillar.campusecho.message.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sharpcaterpillar.campusecho.common.api.PageResponse;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.message.api.MessageResponse;
import top.sharpcaterpillar.campusecho.message.api.UnreadCountResponse;
import top.sharpcaterpillar.campusecho.message.domain.MessageType;
import top.sharpcaterpillar.campusecho.message.domain.SiteMessage;
import top.sharpcaterpillar.campusecho.message.domain.SiteMessageRepository;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class MessageService {

    private final SiteMessageRepository siteMessageRepository;
    private final UserAccountRepository userAccountRepository;

    public MessageService(
            SiteMessageRepository siteMessageRepository,
            UserAccountRepository userAccountRepository) {
        this.siteMessageRepository = siteMessageRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMine(
            AuthenticatedUser currentUser,
            Boolean unreadOnly,
            MessageType messageType,
            int page,
            int size) {
        requireActiveUser(currentUser.id());
        boolean onlyUnread = Boolean.TRUE.equals(unreadOnly);
        Page<SiteMessage> messages = siteMessageRepository.searchMine(
                currentUser.id(), onlyUnread, messageType, pageable(page, size));
        Map<Long, String> usernames = loadUsernames(messages.getContent());
        return PageResponse.from(messages.map(message -> toResponse(message, usernames)));
    }

    @Transactional(readOnly = true)
    public MessageResponse detail(Long messageId, AuthenticatedUser currentUser) {
        requireActiveUser(currentUser.id());
        SiteMessage message = ownedMessage(messageId, currentUser.id());
        return toResponse(message, loadUsernames(java.util.List.of(message)));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(AuthenticatedUser currentUser) {
        requireActiveUser(currentUser.id());
        return new UnreadCountResponse(siteMessageRepository.countByRecipientIdAndReadFalse(currentUser.id()));
    }

    @Transactional
    public MessageResponse markRead(Long messageId, AuthenticatedUser currentUser) {
        requireActiveUser(currentUser.id());
        SiteMessage message = ownedMessage(messageId, currentUser.id());
        if (!message.isRead()) {
            message.markRead();
        }
        return toResponse(message, loadUsernames(java.util.List.of(message)));
    }

    @Transactional
    public UnreadCountResponse markAllRead(AuthenticatedUser currentUser) {
        requireActiveUser(currentUser.id());
        siteMessageRepository.markAllRead(currentUser.id());
        return new UnreadCountResponse(0L);
    }

    private SiteMessage ownedMessage(Long messageId, Long recipientId) {
        return siteMessageRepository.findByIdAndRecipientId(messageId, recipientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "消息不存在"));
    }

    private MessageResponse toResponse(SiteMessage message, Map<Long, String> usernames) {
        String senderUsername = message.getSenderId() == null
                ? null
                : usernames.get(message.getSenderId());
        return MessageResponse.from(message, senderUsername);
    }

    private Map<Long, String> loadUsernames(java.util.List<SiteMessage> messages) {
        Set<Long> senderIds = new HashSet<>();
        for (SiteMessage message : messages) {
            if (message.getSenderId() != null) {
                senderIds.add(message.getSenderId());
            }
        }
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> usernames = new HashMap<>();
        for (UserAccount user : userAccountRepository.findAllById(senderIds)) {
            usernames.put(user.getId(), user.getUsername());
        }
        return usernames;
    }

    private void requireActiveUser(Long userId) {
        if (userAccountRepository.findByIdAndStatus(userId, UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用");
        }
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
