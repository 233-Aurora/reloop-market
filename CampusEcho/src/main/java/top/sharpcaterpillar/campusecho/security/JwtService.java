package top.sharpcaterpillar.campusecho.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserRole;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final byte[] HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8);

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public IssuedToken issue(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        claims.put("iss", properties.issuer());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = URL_ENCODER.encodeToString(HEADER);
        String encodedPayload = URL_ENCODER.encodeToString(writeJson(claims));
        String content = encodedHeader + "." + encodedPayload;
        return new IssuedToken(content + "." + sign(content), expiresAt);
    }

    public AuthenticatedUser parse(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new InvalidJwtException();
        }

        String content = parts[0] + "." + parts[1];
        byte[] receivedSignature;
        try {
            receivedSignature = URL_DECODER.decode(parts[2]);
        } catch (IllegalArgumentException exception) {
            throw new InvalidJwtException();
        }
        byte[] expectedSignature = URL_DECODER.decode(sign(content));
        if (!MessageDigest.isEqual(expectedSignature, receivedSignature)) {
            throw new InvalidJwtException();
        }

        JsonNode claims = readJson(parts[1]);
        long id = claims.path("sub").asLong(-1);
        long expiresAt = claims.path("exp").asLong(-1);
        String username = claims.path("username").asText(null);
        String issuer = claims.path("iss").asText(null);
        String roleName = claims.path("role").asText(null);
        if (id <= 0 || expiresAt <= Instant.now().getEpochSecond() || username == null || !properties.issuer().equals(issuer)) {
            throw new InvalidJwtException();
        }

        try {
            return new AuthenticatedUser(id, username, UserRole.valueOf(roleName));
        } catch (IllegalArgumentException exception) {
            throw new InvalidJwtException();
        }
    }

    private byte[] writeJson(Map<String, Object> claims) {
        try {
            return objectMapper.writeValueAsBytes(claims);
        } catch (JacksonException exception) {
            throw new IllegalStateException("无法创建访问令牌", exception);
        }
    }

    private JsonNode readJson(String encodedPayload) {
        try {
            return objectMapper.readTree(URL_DECODER.decode(encodedPayload));
        } catch (IllegalArgumentException | JacksonException exception) {
            throw new InvalidJwtException();
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("无法签名访问令牌", exception);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public static class InvalidJwtException extends RuntimeException {
    }
}
