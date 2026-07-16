package top.sharpcaterpillar.campusecho.user.application;

import java.nio.file.Path;
import org.springframework.http.MediaType;

public record AvatarContent(Path path, MediaType mediaType) {
}
