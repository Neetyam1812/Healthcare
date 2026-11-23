package Healthcare.HC.Security;

import Healthcare.HC.Service.AuditService;
import Healthcare.HC.Service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter executed prior to UsernamePasswordAuthenticationFilter.
 * If the login rate limit (5 failed attempts per email+IP per 15 min) has been reached,
 * abort authentication immediately and redirect to /login?error=true with an audit log.
 */
@Component
public class RateLimitLoginFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final AuditService auditService;

    public RateLimitLoginFilter(RateLimiterService rateLimiterService, AuditService auditService) {
        this.rateLimiterService = rateLimiterService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath())) {
            String email = request.getParameter("email");
            String ip = getClientIp(request);

            if (email != null && rateLimiterService.isLoginBlocked(email, ip)) {
                auditService.log(null, email, "LOGIN_FAILURE", "USER", null, "Login blocked by rate limit", ip);
                response.sendRedirect(request.getContextPath() + "/login?error=true");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
