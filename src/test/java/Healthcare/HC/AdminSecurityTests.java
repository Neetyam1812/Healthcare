package Healthcare.HC;

import Healthcare.HC.Entity.AuditLog;
import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.AuditLogRepository;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.Security.AppUserDetails;
import Healthcare.HC.Service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AdminSecurityTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AdminUserService adminUserService;

    private User sampleUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        sampleUser = new User();
        sampleUser.setFullName("Regular Test User");
        sampleUser.setEmail("reguser@test.org");
        sampleUser.setPassword("encodedpass");
        sampleUser.setRole("FAMILY_ADMIN");
        sampleUser.setStatus("ACTIVE");
        sampleUser.setEnabled(true);
        sampleUser = userRepository.save(sampleUser);

        superAdminUser = new User();
        superAdminUser.setFullName("Platform Admin");
        superAdminUser.setEmail("platformadmin@test.org");
        superAdminUser.setPassword("encodedpass");
        superAdminUser.setRole("FAMILY_ADMIN");
        superAdminUser.setPlatformRole("SUPER_ADMIN");
        superAdminUser.setStatus("ACTIVE");
        superAdminUser.setEnabled(true);
        superAdminUser = userRepository.save(superAdminUser);
    }

    @Test
    @DisplayName("1. ANALYST role cannot POST to /admin/users/{id}/status and receives 403 Forbidden")
    @WithMockUser(username = "analyst@test.org", roles = {"ANALYST"})
    void analystCannotChangeUserStatus() throws Exception {
        mockMvc.perform(post("/admin/users/" + sampleUser.getId() + "/status")
                .with(csrf())
                .param("action", "SUSPEND")
                .param("reason", "Administrative suspension audit test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2. SUPPORT_AGENT role cannot access /admin/staff and receives 403 Forbidden")
    @WithMockUser(username = "agent@test.org", roles = {"SUPPORT_AGENT"})
    void supportAgentCannotAccessStaffManagement() throws Exception {
        mockMvc.perform(get("/admin/staff"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Regular USER (ROLE_USER) cannot access /admin/dashboard and receives 403 Forbidden")
    @WithMockUser(username = "patient@test.org", roles = {"USER"})
    void regularUserCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Suspending an account records an AuditLog entry")
    void suspendingUserCreatesAuditRecord() throws Exception {
        AppUserDetails adminPrincipal = new AppUserDetails(
                superAdminUser.getId(),
                superAdminUser.getFullName(),
                superAdminUser.getEmail(),
                superAdminUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
                true
        );

        mockMvc.perform(post("/admin/users/" + sampleUser.getId() + "/status")
                .with(user(adminPrincipal))
                .with(csrf())
                .param("action", "SUSPEND")
                .param("reason", "Suspended for safety policy violations"))
                .andExpect(status().is3xxRedirection());

        List<AuditLog> records = auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc("USER", sampleUser.getId());
        assertFalse(records.isEmpty(), "An audit log entry must be created when an account is suspended");

        AuditLog latest = records.get(0);
        assertEquals("USER_SUSPENDED", latest.getAction());
        assertEquals(superAdminUser.getEmail(), latest.getActorEmail());
        assertTrue(latest.getDetail().contains("safety policy violations"));

        User refreshed = userRepository.findById(sampleUser.getId()).orElseThrow();
        assertEquals("SUSPENDED", refreshed.getStatus());
        assertFalse(refreshed.isEnabled());
    }

    @Test
    @DisplayName("5. Administrator cannot suspend their own account")
    @WithMockUser(username = "platformadmin@test.org", roles = {"SUPER_ADMIN"})
    void adminCannotSuspendSelf() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminUserService.changeUserStatus(
                        superAdminUser.getId(),
                        "SUSPEND",
                        "Attempting self suspension",
                        superAdminUser.getId(),
                        superAdminUser.getEmail(),
                        "127.0.0.1"
                )
        );

        assertTrue(ex.getMessage().contains("cannot suspend"));
    }

    @Test
    @DisplayName("6. SUPER_ADMIN accounts cannot be suspended by any administrator")
    @WithMockUser(username = "agent@test.org", roles = {"SUPPORT_AGENT"})
    void superAdminAccountCannotBeSuspended() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                adminUserService.changeUserStatus(
                        superAdminUser.getId(),
                        "SUSPEND",
                        "Attempting to suspend super admin",
                        999L,
                        "agent@test.org",
                        "127.0.0.1"
                )
        );

        assertTrue(ex.getMessage().contains("SUPER_ADMIN accounts cannot be suspended"));
    }

}
