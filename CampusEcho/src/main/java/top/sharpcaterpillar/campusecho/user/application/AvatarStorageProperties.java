package top.sharpcaterpillar.campusecho.user.application;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.storage.avatars")
public record AvatarStorageProperties(Path directory, DataSize maxFileSize) {
}
