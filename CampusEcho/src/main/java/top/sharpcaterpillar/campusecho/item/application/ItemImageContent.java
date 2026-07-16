package top.sharpcaterpillar.campusecho.item.application;

import java.nio.file.Path;
import org.springframework.http.MediaType;

public record ItemImageContent(Path path, MediaType mediaType) {
}
