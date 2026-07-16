package top.sharpcaterpillar.campusecho.user.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.common.storage.ImageFileValidator;
import top.sharpcaterpillar.campusecho.common.storage.ImageFileValidator.DetectedImage;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.api.CurrentUserResponse;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class AvatarService {

    private static final String URL_PREFIX = "/api/v1/avatars/";
    private static final Pattern STORAGE_KEY_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final UserAccountRepository userAccountRepository;
    private final AvatarStorageProperties properties;
    private final ImageFileValidator imageFileValidator;

    public AvatarService(
            UserAccountRepository userAccountRepository,
            AvatarStorageProperties properties,
            ImageFileValidator imageFileValidator) {
        this.userAccountRepository = userAccountRepository;
        this.properties = properties;
        this.imageFileValidator = imageFileValidator;
    }

    @Transactional
    public CurrentUserResponse upload(AuthenticatedUser currentUser, MultipartFile file) {
        UserAccount user = activeUserForUpdate(currentUser.id());
        DetectedImage image = imageFileValidator.validate(file, properties.maxFileSize());
        String previousAvatarUrl = user.getAvatarUrl();
        Path target = null;
        Path temporary = null;
        try {
            Path root = rootDirectory();
            Files.createDirectories(root);
            String storageKey = UUID.randomUUID() + image.extension();
            target = safePath(root, storageKey);
            temporary = safePath(root, "." + storageKey + ".uploading");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            user.updateAvatarUrl(URL_PREFIX + storageKey);
            userAccountRepository.saveAndFlush(user);
            registerRollbackCleanup(target);
            registerDeleteAfterCommit(storageKey(previousAvatarUrl));
            return CurrentUserResponse.from(user);
        } catch (IOException exception) {
            deleteQuietly(target);
            deleteQuietly(temporary);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "无法保存头像");
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            deleteQuietly(temporary);
            throw exception;
        }
    }

    @Transactional
    public CurrentUserResponse delete(AuthenticatedUser currentUser) {
        UserAccount user = activeUserForUpdate(currentUser.id());
        String previousStorageKey = storageKey(user.getAvatarUrl());
        user.updateAvatarUrl(null);
        userAccountRepository.saveAndFlush(user);
        registerDeleteAfterCommit(previousStorageKey);
        return CurrentUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AvatarContent content(String storageKey) {
        if (!STORAGE_KEY_PATTERN.matcher(storageKey).matches()
                || !userAccountRepository.existsByAvatarUrl(URL_PREFIX + storageKey)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "头像不存在");
        }
        Path path = safePath(rootDirectory(), storageKey);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "头像文件不存在");
        }
        return new AvatarContent(path, imageFileValidator.mediaType(storageKey));
    }

    private UserAccount activeUserForUpdate(Long userId) {
        return userAccountRepository.findByIdAndStatusForUpdate(userId, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "账户不可用"));
    }

    private Path rootDirectory() {
        return properties.directory().toAbsolutePath().normalize();
    }

    private Path safePath(Path root, String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "头像路径不合法");
        }
        return path;
    }

    private String storageKey(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(URL_PREFIX)) {
            return null;
        }
        String storageKey = avatarUrl.substring(URL_PREFIX.length());
        return STORAGE_KEY_PATTERN.matcher(storageKey).matches() ? storageKey : null;
    }

    private void registerRollbackCleanup(Path target) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteQuietly(target);
                    }
                }
            });
        }
    }

    private void registerDeleteAfterCommit(String storageKey) {
        if (storageKey != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(safePath(rootDirectory(), storageKey));
                }
            });
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Orphan cleanup can be retried by an operational job.
        }
    }
}
