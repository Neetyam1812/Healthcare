package Healthcare.HC.Controller;

import Healthcare.HC.Security.AppUserDetails;
import Healthcare.HC.Service.AdminUserService;
import Healthcare.HC.dto.AdminUserDetail;
import Healthcare.HC.dto.AdminUserRow;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT', 'ANALYST')")
    public String listUsers(@RequestParam(name = "q", required = false) String query,
                            @RequestParam(name = "status", required = false) String status,
                            @RequestParam(name = "sort", defaultValue = "newest") String sort,
                            @RequestParam(name = "page", defaultValue = "0") int page,
                            @AuthenticationPrincipal AppUserDetails userDetails,
                            Model model) {
        Long currentUserId = userDetails != null ? userDetails.getId() : null;
        Page<AdminUserRow> usersPage = adminUserService.searchUsers(query, status, sort, page, 15, currentUserId);

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("currentStatus", status != null ? status : "ALL");
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        populateAdminAttributes(model, userDetails);

        return "admin/users";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT', 'ANALYST')")
    public String viewUserDetail(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal AppUserDetails userDetails,
                                 Model model) {
        Long currentUserId = userDetails != null ? userDetails.getId() : null;
        AdminUserDetail userDetail = adminUserService.getUserDetail(id, currentUserId);

        model.addAttribute("targetUser", userDetail);
        populateAdminAttributes(model, userDetails);

        return "admin/user_detail";
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT')")
    public String updateUserStatus(@PathVariable("id") Long id,
                                   @RequestParam("action") String action,
                                   @RequestParam("reason") String reason,
                                   @AuthenticationPrincipal AppUserDetails userDetails,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long currentUserId = userDetails != null ? userDetails.getId() : null;
            String currentEmail = userDetails != null ? userDetails.getEmail() : "admin@system";
            String ip = request.getRemoteAddr();

            adminUserService.changeUserStatus(id, action, reason, currentUserId, currentEmail, ip);

            String verb = "SUSPEND".equalsIgnoreCase(action) ? "suspended" : "activated";
            redirectAttributes.addFlashAttribute("successMessage", "Account successfully " + verb + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update account status. Please try again.");
        }

        return "redirect:/admin/users/" + id;
    }

    private void populateAdminAttributes(Model model, AppUserDetails userDetails) {
        if (userDetails != null) {
            model.addAttribute("adminName", userDetails.getFullName());
            model.addAttribute("adminEmail", userDetails.getEmail());
            String roleName = userDetails.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .filter(r -> r.contains("ADMIN") || r.contains("AGENT") || r.contains("ANALYST"))
                    .findFirst()
                    .orElse("ADMIN");
            model.addAttribute("adminRole", roleName);
            model.addAttribute("adminInitials", getInitials(userDetails.getFullName()));
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "AD";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
