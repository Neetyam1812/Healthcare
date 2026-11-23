package Healthcare.HC.Security;

import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.Service.AuditService;
import Healthcare.HC.Service.RateLimiterService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * RoleBasedSuccessHandler:
 * - admin roles (SUPER_ADMIN, SUPPORT_AGENT, ANALYST) -> /admin/dashboard
 * - everyone else -> /app/dashboard
 * - update users.lastLoginAt
 * - write an audit entry (LOGIN_SUCCESS)
 * - reset failed login counter
 */
@Component
public class RoleBasedSuccessHandler implements AuthenticationSuccessHandler {

    private static final Set<String> ADMIN_ROLES = Set.of(
            "ROLE_SUPER_ADMIN",
            "ROLE_SUPPORT_AGENT",
            "ROLE_ANALYST"
    );

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public RoleBasedSuccessHandler(UserRepository userRepository,
                                   AuditService auditService,
                                   RateLimiterService rateLimiterService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        String ip = getClientIp(request);

        // Reset rate limiter for email+ip
        rateLimiterService.resetFailedLogin(email, ip);

        Long userId = null;
        if (authentication.getPrincipal() instanceof AppUserDetails userDetails) {
            userId = userDetails.getId();
        }

        // Update users.lastLoginAt
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            if (userId == null) {
                userId = user.getId();
            }
        }

        // Write audit entry: LOGIN_SUCCESS
        auditService.log(userId, email, "LOGIN_SUCCESS", "USER", userId, "User login successful", ip);

        // Determine destination based on role
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_ROLES::contains);

        String targetUrl = isAdmin ? "/admin/dashboard" : "/app/dashboard";
        response.sendRedirect(request.getContextPath() + targetUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
