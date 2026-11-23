package Healthcare.HC.Controller;

import Healthcare.HC.Entity.AuditLog;
import Healthcare.HC.Repository.AuditLogRepository;
import Healthcare.HC.Security.AppUserDetails;
import Healthcare.HC.dto.AdminDashboardView;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AdminAuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ANALYST')")
    public String listAuditLogs(@RequestParam(name = "action", required = false) String action,
                                @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                @RequestParam(name = "page", defaultValue = "0") int page,
                                @AuthenticationPrincipal AppUserDetails userDetails,
                                Model model) {
        Pageable pageable = PageRequest.of(Math.max(0, page), 25);
        LocalDateTime startDt = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDt = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        Page<AuditLog> auditPage = auditLogRepository.searchAuditLogs(action, startDt, endDt, pageable);

        Page<AdminDashboardView.AuditLogItemDto> dtoPage = auditPage.map(a -> new AdminDashboardView.AuditLogItemDto(
                a.getId(),
                a.getActorEmail(),
                a.getAction(),
                a.getTargetType(),
                a.getTargetId(),
                a.getDetail(),
                a.getIp(),
                a.getCreatedAt() != null ? a.getCreatedAt().format(TIME_FMT) : "—"
        ));

        model.addAttribute("auditPage", dtoPage);
        model.addAttribute("selectedAction", action != null ? action : "");
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", dtoPage.getTotalPages());
        populateAdminAttributes(model, userDetails);

        return "admin/audit_logs";
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void exportAuditCsv(HttpServletResponse response) {
        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"audit_logs_" + LocalDate.now() + ".csv\"");

            List<AuditLog> allLogs = auditLogRepository.findAll();
            PrintWriter writer = response.getWriter();
            writer.println("ID,Timestamp,Actor Email,Action,Target Type,Target ID,IP Address,Details");

            for (AuditLog log : allLogs) {
                String timeStr = log.getCreatedAt() != null ? log.getCreatedAt().format(TIME_FMT) : "";
                String actor = escapeCsv(log.getActorEmail());
                String action = escapeCsv(log.getAction());
                String targetType = escapeCsv(log.getTargetType());
                Long targetId = log.getTargetId() != null ? log.getTargetId() : 0L;
                String ip = escapeCsv(log.getIp());
                String detail = escapeCsv(log.getDetail());

                writer.println(String.format("%d,%s,%s,%s,%s,%d,%s,%s",
                        log.getId(), timeStr, actor, action, targetType, targetId, ip, detail));
            }
            writer.flush();
        } catch (Exception ignored) {
        }
    }

    private String escapeCsv(String text) {
        if (text == null) return "";
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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
