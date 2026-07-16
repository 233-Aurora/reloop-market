package top.sharpcaterpillar.campusecho.item.application;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.storage.item-images")
public record ItemImageStorageProperties(
        Path directory,
        DataSize maxFileSize,
        int maxFilesPerItem) {
}
