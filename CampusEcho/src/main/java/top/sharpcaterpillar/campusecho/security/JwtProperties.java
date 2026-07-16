package top.sharpcaterpillar.campusecho.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT_SECRET 不能为空")
        @Size(min = 32, message = "JWT_SECRET 至少需要 32 个字符")
        String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl) {
}
