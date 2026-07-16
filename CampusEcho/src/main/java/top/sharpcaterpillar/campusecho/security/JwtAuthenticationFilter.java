package top.sharpcaterpillar.campusecho.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.sharpcaterpillar.campusecho.user.domain.UserAccount;
import top.sharpcaterpillar.campusecho.user.domain.UserAccountRepository;
import top.sharpcaterpillar.campusecho.user.domain.UserStatus;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository userAccountRepository) {
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return;
        }

        try {
            AuthenticatedUser tokenUser = jwtService.parse(authorization.substring(7));
            UserAccount account = userAccountRepository.findByIdAndStatus(tokenUser.id(), UserStatus.ACTIVE)
                    .filter(candidate -> candidate.getUsername().equals(tokenUser.username()))
                    .orElseThrow(JwtService.InvalidJwtException::new);
            AuthenticatedUser user = new AuthenticatedUser(account.getId(), account.getUsername(), account.getRole());
            var authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtService.InvalidJwtException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response);
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"https://campusecho.local/problems/unauthorized\",\"title\":\"认证失败\",\"status\":401,\"detail\":\"身份令牌无效或已过期\",\"code\":\"UNAUTHORIZED\"}");
    }
}
