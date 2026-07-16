package top.sharpcaterpillar.campusecho.common.error;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(errorCode.status()), exception.getMessage());
        problem.setType(URI.create("https://campusecho.local/problems/" + errorCode.name().toLowerCase()));
        problem.setTitle(errorCode.message());
        problem.setProperty("code", errorCode.name());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> violations = exception.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.message());
        problem.setType(URI.create("https://campusecho.local/problems/validation-failed"));
        problem.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "上传文件超过服务器限制");
        problem.setType(URI.create("https://campusecho.local/problems/file-too-large"));
        problem.setTitle(ErrorCode.FILE_TOO_LARGE.message());
        problem.setProperty("code", ErrorCode.FILE_TOO_LARGE.name());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(Exception exception) {
        LOGGER.error("Unhandled request exception", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用");
        problem.setType(URI.create("https://campusecho.local/problems/internal-error"));
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }
}
