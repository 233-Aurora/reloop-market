package top.sharpcaterpillar.campusecho.item.api;

import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sharpcaterpillar.campusecho.item.application.ItemImageContent;
import top.sharpcaterpillar.campusecho.item.application.ItemImageService;

@RestController
@RequestMapping("/api/v1/item-images")
public class ItemImageContentController {

    private final ItemImageService itemImageService;

    public ItemImageContentController(ItemImageService itemImageService) {
        this.itemImageService = itemImageService;
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long imageId) {
        ItemImageContent content = itemImageService.content(imageId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .contentType(content.mediaType())
                .body(new FileSystemResource(content.path()));
    }
}
