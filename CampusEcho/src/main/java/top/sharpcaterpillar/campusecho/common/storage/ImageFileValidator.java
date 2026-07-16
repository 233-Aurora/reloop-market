package top.sharpcaterpillar.campusecho.common.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import top.sharpcaterpillar.campusecho.common.error.BusinessException;
import top.sharpcaterpillar.campusecho.common.error.ErrorCode;

@Component
public class ImageFileValidator {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    public DetectedImage validate(MultipartFile file, DataSize maxFileSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "图片文件不能为空");
        }
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, "图片不能超过 " + maxFileSize.toMegabytes() + " MiB");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (startsWith(header, PNG_SIGNATURE)) {
                return new DetectedImage(".png", MediaType.IMAGE_PNG);
            }
            if (header.length >= 3
                    && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8
                    && (header[2] & 0xff) == 0xff) {
                return new DetectedImage(".jpg", MediaType.IMAGE_JPEG);
            }
            if (header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return new DetectedImage(".webp", MediaType.parseMediaType("image/webp"));
            }
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "无法读取图片文件");
        }
        throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "仅支持 JPEG、PNG 或 WebP 图片");
    }

    public MediaType mediaType(String storageKey) {
        if (storageKey.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (storageKey.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (storageKey.endsWith(".jpg")) {
            return MediaType.IMAGE_JPEG;
        }
        throw new BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "图片文件类型不受支持");
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    public record DetectedImage(String extension, MediaType mediaType) {
    }
}
