package top.sharpcaterpillar.campusecho.item.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;
import top.sharpcaterpillar.campusecho.common.storage.ImageFileValidator;
import top.sharpcaterpillar.campusecho.common.storage.ImageFileValidator.DetectedImage;
import top.sharpcaterpillar.campusecho.item.api.ItemImageResponse;
import top.sharpcaterpillar.campusecho.item.domain.ItemImage;
import top.sharpcaterpillar.campusecho.item.domain.ItemImageRepository;
import top.sharpcaterpillar.campusecho.item.domain.ItemStatus;
import top.sharpcaterpillar.campusecho.item.domain.ItemModerationStatus;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItem;
import top.sharpcaterpillar.campusecho.item.domain.MarketplaceItemRepository;
import top.sharpcaterpillar.campusecho.security.AuthenticatedUser;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Service
public class ItemImageService {

    private final ItemImageRepository itemImageRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final ItemImageStorageProperties properties;
    private final ImageFileValidator imageFileValidator;

    public ItemImageService(
            ItemImageRepository itemImageRepository,
            MarketplaceItemRepository marketplaceItemRepository,
            ItemImageStorageProperties properties,
            ImageFileValidator imageFileValidator) {
        this.itemImageRepository = itemImageRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.properties = properties;
        this.imageFileValidator = imageFileValidator;
    }

    @Transactional
    public ItemImageResponse upload(Long itemId, AuthenticatedUser currentUser, MultipartFile file, boolean primary) {
        MarketplaceItem item = editableOwnedItem(itemId, currentUser.id());
        if (itemImageRepository.countByItemId(itemId) >= properties.maxFilesPerItem()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "每件物品最多上传 " + properties.maxFilesPerItem() + " 张图片");
        }

        DetectedImage format = imageFileValidator.validate(file, properties.maxFileSize());
        Path target = null;
        Path temporary = null;
        try {
            Path root = rootDirectory();
            Files.createDirectories(root);
            String storageKey = UUID.randomUUID() + format.extension();
            target = safePath(root, storageKey);
            temporary = safePath(root, "." + storageKey + ".uploading");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);

            boolean isPrimary = primary || itemImageRepository.countByItemId(itemId) == 0;
            if (isPrimary) {
                clearPrimary(itemId);
            }
            int sortOrder = itemImageRepository.findMaxSortOrder(itemId) + 1;
            ItemImage image = itemImageRepository.save(ItemImage.create(itemId, storageKey, sortOrder, isPrimary));
            registerCleanup(target);
            return ItemImageResponse.from(image);
        } catch (IOException exception) {
            deleteQuietly(target);
            deleteQuietly(temporary);
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "无法保存图片");
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            deleteQuietly(temporary);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ItemImageResponse> list(Long itemId) {
        return itemImageRepository.findByItemIdOrderBySortOrderAscIdAsc(itemId).stream()
                .map(ItemImageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemImageResponse> listPublic(Long itemId) {
        marketplaceItemRepository.findPublicById(
                        itemId, ItemStatus.ON_SALE, ItemModerationStatus.NORMAL, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "物品不存在"));
        return list(itemId);
    }

    @Transactional
    public void delete(Long itemId, Long imageId, AuthenticatedUser currentUser) {
        editableOwnedItem(itemId, currentUser.id());
        ItemImage image = itemImageRepository.findByIdAndItemId(imageId, itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图片不存在"));
        boolean wasPrimary = image.isPrimary();
        itemImageRepository.delete(image);
        itemImageRepository.flush();
        if (wasPrimary) {
            itemImageRepository.findFirstByItemIdOrderBySortOrderAscIdAsc(itemId).ifPresent(next -> {
                next.makePrimary();
                itemImageRepository.save(next);
            });
        }
        registerDeleteAfterCommit(image.getStorageKey());
    }

    @Transactional
    public ItemImageResponse makePrimary(Long itemId, Long imageId, AuthenticatedUser currentUser) {
        editableOwnedItem(itemId, currentUser.id());
        ItemImage image = itemImageRepository.findByIdAndItemId(imageId, itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图片不存在"));
        if (!image.isPrimary()) {
            clearPrimary(itemId);
            image.makePrimary();
            itemImageRepository.save(image);
        }
        return ItemImageResponse.from(image);
    }

    @Transactional(readOnly = true)
    public ItemImageContent content(Long imageId) {
        ItemImage image = itemImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图片不存在"));
        marketplaceItemRepository.findPublicById(
                        image.getItemId(), ItemStatus.ON_SALE, ItemModerationStatus.NORMAL, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图片不存在"));
        Path path = safePath(rootDirectory(), image.getStorageKey());
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "图片文件不存在");
        }
        return new ItemImageContent(path, imageFileValidator.mediaType(image.getStorageKey()));
    }

    private MarketplaceItem editableOwnedItem(Long itemId, Long sellerId) {
        MarketplaceItem item = marketplaceItemRepository.findOwnedByIdForUpdate(itemId, sellerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "物品不存在或无权操作"));
        if (item.getStatus() == ItemStatus.SOLD || item.getStatus() == ItemStatus.RESERVED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前物品状态不允许修改图片");
        }
        if (item.getModerationStatus() != ItemModerationStatus.NORMAL) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "物品已被管理员屏蔽，不能修改图片");
        }
        return item;
    }

    private void clearPrimary(Long itemId) {
        itemImageRepository.findByItemIdAndPrimaryTrue(itemId).ifPresent(current -> {
            current.removePrimary();
            itemImageRepository.saveAndFlush(current);
        });
    }

    private Path rootDirectory() {
        return properties.directory().toAbsolutePath().normalize();
    }

    private Path safePath(Path root, String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_ERROR, "图片路径不合法");
        }
        return path;
    }

    private void registerCleanup(Path target) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Path savedTarget = target;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteQuietly(savedTarget);
                    }
                }
            });
        }
    }

    private void registerDeleteAfterCommit(String storageKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
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
