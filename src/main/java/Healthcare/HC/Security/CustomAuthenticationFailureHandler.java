package Healthcare.HC.Security;

import Healthcare.HC.Service.AuditService;
import Healthcare.HC.Service.RateLimiterService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuditService auditService;
    private final RateLimiterService rateLimiterService;

    public CustomAuthenticationFailureHandler(AuditService auditService,
                                              RateLimiterService rateLimiterService) {
        this.auditService = auditService;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("email");
        String ip = getClientIp(request);

        if (email != null && !email.isBlank()) {
            rateLimiterService.recordFailedLogin(email, ip);
        }

        auditService.log(null, email, "LOGIN_FAILURE", "USER", null, "Failed login attempt", ip);

        response.sendRedirect(request.getContextPath() + "/login?error=true");
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
