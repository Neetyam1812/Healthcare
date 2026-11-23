package Healthcare.HC.Controller;

import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.Security.AppUserDetails;
import Healthcare.HC.Service.AdminStatsService;
import Healthcare.HC.dto.AdminDashboardView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminStatsService adminStatsService;
    private final UserRepository userRepository;

    public AdminDashboardController(AdminStatsService adminStatsService, UserRepository userRepository) {
        this.adminStatsService = adminStatsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT', 'ANALYST')")
    public String adminDashboard(@RequestParam(name = "range", defaultValue = "30") int range,
                                 @AuthenticationPrincipal AppUserDetails userDetails,
                                 Model model) {
        AdminDashboardView dashboardView = adminStatsService.getDashboardView(range);
        model.addAttribute("dashboard", dashboardView);
        populateAdminAttributes(model, userDetails);
        return "admin_dashboard";
    }

    @PostMapping("/sos/{id}/ack")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPPORT_AGENT')")
    public Object acknowledgeSos(@PathVariable("id") Long id,
                                 @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                 @AuthenticationPrincipal AppUserDetails userDetails,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        adminStatsService.acknowledgeSos(id, userDetails.getId(), userDetails.getEmail(), request.getRemoteAddr());

        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith) || "application/json".equalsIgnoreCase(request.getHeader("Accept"))) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Emergency event acknowledged successfully."));
        }

        redirectAttributes.addFlashAttribute("successMessage", "Emergency event #" + id + " acknowledged successfully.");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String staffManagement(@AuthenticationPrincipal AppUserDetails userDetails, Model model) {
        populateAdminAttributes(model, userDetails);
        // Load staff members (SUPER_ADMIN, SUPPORT_AGENT, ANALYST)
        List<User> staffMembers = userRepository.findAll().stream()
                .filter(u -> u.getPlatformRole() != null && !u.getPlatformRole().isBlank())
                .toList();
        model.addAttribute("staffMembers", staffMembers);
        return "admin/staff";
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
        } else {
            model.addAttribute("adminName", "Admin User");
            model.addAttribute("adminEmail", "admin@familycare.io");
            model.addAttribute("adminRole", "SUPER_ADMIN");
            model.addAttribute("adminInitials", "AD");
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
