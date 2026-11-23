package Healthcare.HC.Controller;

import Healthcare.HC.Entity.FamilyMember;
import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.FamilyMemberRepository;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.Security.AppUserDetails;
import Healthcare.HC.Service.DashboardService;
import Healthcare.HC.dto.UserDashboardView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/app")
public class UserDashboardController {

    private final DashboardService dashboardService;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    public UserDashboardController(DashboardService dashboardService,
                                   FamilyMemberRepository familyMemberRepository,
                                   UserRepository userRepository) {
        this.dashboardService = dashboardService;
        this.familyMemberRepository = familyMemberRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String userDashboard(
            @AuthenticationPrincipal AppUserDetails userDetails,
            Authentication authentication,
            @RequestParam(value = "memberId", required = false) Long memberId,
            Model model) {

        // If user has admin authorities, redirect to admin dashboard
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_SUPPORT_AGENT")
                        || a.getAuthority().equals("ROLE_ANALYST"))) {
            return "redirect:/admin/dashboard";
        }

        Long userId = resolveUserId(userDetails, authentication);
        if (userId == null) {
            return "redirect:/login";
        }

        UserDashboardView dashboardView = dashboardService.getDashboardView(userId, memberId);
        List<FamilyMember> members = familyMemberRepository.findByUserId(userId);

        String fullName = userDetails != null ? userDetails.getFullName() : "User";
        String email = userDetails != null ? userDetails.getEmail() : "";
        String initials = extractInitials(fullName);

        model.addAttribute("dashboard", dashboardView);
        model.addAttribute("members", members);
        model.addAttribute("selectedMemberId", dashboardView.getSelectedMemberId());
        model.addAttribute("userFullName", fullName);
        model.addAttribute("userEmail", email);
        model.addAttribute("userInitials", initials);

        return "user_dashboard";
    }

    @PostMapping("/medicines/{id}/take")
    @ResponseBody
    public ResponseEntity<?> takeMedicine(
            @PathVariable("id") Long medicineId,
            @AuthenticationPrincipal AppUserDetails userDetails,
            Authentication authentication,
            HttpServletRequest request) {

        Long userId = resolveUserId(userDetails, authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        String ip = getClientIp(request);
        try {
            Map<String, Object> result = dashboardService.recordMedicineTaken(medicineId, userId, ip);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to mark medicine as taken"));
        }
    }

    @PostMapping("/emergency/sos")
    @ResponseBody
    public ResponseEntity<?> triggerEmergencySos(
            @RequestBody(required = false) Map<String, Object> payload,
            @AuthenticationPrincipal AppUserDetails userDetails,
            Authentication authentication,
            HttpServletRequest request) {

        Long userId = resolveUserId(userDetails, authentication);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        Double latitude = null;
        Double longitude = null;
        if (payload != null) {
            if (payload.get("latitude") instanceof Number num) {
                latitude = num.doubleValue();
            }
            if (payload.get("longitude") instanceof Number num) {
                longitude = num.doubleValue();
            }
        }

        String ip = getClientIp(request);
        try {
            Map<String, Object> result = dashboardService.recordEmergencySos(userId, latitude, longitude, ip);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to record SOS event"));
        }
    }

    private Long resolveUserId(AppUserDetails userDetails, Authentication authentication) {
        if (userDetails != null && userDetails.getId() != null) {
            return userDetails.getId();
        }
        if (authentication != null && authentication.getName() != null) {
            User user = userRepository.findByEmail(authentication.getName().trim().toLowerCase()).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "FC";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
