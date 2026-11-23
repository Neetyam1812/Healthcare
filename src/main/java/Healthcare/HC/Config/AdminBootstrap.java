package Healthcare.HC.Config;

import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * AdminBootstrap CommandLineRunner:
 * If no user with a platformRole exists AND env vars ADMIN_EMAIL and ADMIN_PASSWORD are set,
 * create one SUPER_ADMIN (BCrypt hash).
 * Never log the password. If the env vars are missing, log a warning only.
 */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:#{null}}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:#{null}}")
    private String adminPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check fallback to direct System.getenv if property resolution returns null
        String email = (adminEmail != null && !adminEmail.isBlank()) ? adminEmail : System.getenv("ADMIN_EMAIL");
        String password = (adminPassword != null && !adminPassword.isBlank()) ? adminPassword : System.getenv("ADMIN_PASSWORD");

        boolean platformAdminExists = userRepository.existsByPlatformRoleIsNotNull();
        if (!platformAdminExists) {
            if (email != null && !email.isBlank() && password != null && !password.isBlank()) {
                String normalizedEmail = email.trim().toLowerCase();
                User admin = new User();
                admin.setFullName("Super Admin");
                admin.setEmail(normalizedEmail);
                admin.setPassword(passwordEncoder.encode(password));
                admin.setPlatformRole("SUPER_ADMIN");
                admin.setRole("FAMILY_ADMIN");
                admin.setStatus("ACTIVE");
                admin.setEnabled(true);
                userRepository.save(admin);

                // Note: NEVER log the password!
                logger.info("AdminBootstrap: Successfully created initial SUPER_ADMIN account for email: {}", normalizedEmail);
            } else {
                logger.warn("AdminBootstrap: No user with a platformRole exists, and ADMIN_EMAIL / ADMIN_PASSWORD environment variables are not set. Skipping admin bootstrap.");
            }
        }
    }
}
