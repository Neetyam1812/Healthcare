package Healthcare.HC.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * AccessDeniedHandler:
 * - If an admin opens /app/** redirect them to /admin/dashboard
 * - If a user opens /admin/** show templates/error/403.html (HTTP 403)
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Set<String> ADMIN_ROLES = Set.of(
            "ROLE_SUPER_ADMIN",
            "ROLE_SUPPORT_AGENT",
            "ROLE_ANALYST"
    );

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathWithinApp = uri.substring(contextPath.length());

        if (auth != null && auth.isAuthenticated()) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(ADMIN_ROLES::contains);

            if (isAdmin && pathWithinApp.startsWith("/app")) {
                response.sendRedirect(contextPath + "/admin/dashboard");
                return;
            }
        }

        // Show 403 error page
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute("jakarta.servlet.error.status_code", HttpServletResponse.SC_FORBIDDEN);
        request.getRequestDispatcher("/error/403").forward(request, response);
    }
}
