package Healthcare.HC.Service;

import Healthcare.HC.Entity.PasswordResetToken;
import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.PasswordResetTokenRepository;
import Healthcare.HC.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;
    private final AuditService auditService;
    private final Environment environment;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                RateLimiterService rateLimiterService,
                                AuditService auditService,
                                Environment environment,
                                @Autowired(required = false) JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
        this.auditService = auditService;
        this.environment = environment;
        this.mailSender = mailSender;
    }

    /**
     * Step 1: Send OTP for password reset.
     * Rate limited to max 3 OTP sends per email per hour.
     * Generates a 6-digit code, stores BCrypt hash with 10 min expiration.
     * Sends via JavaMailSender if spring.mail.host configured; otherwise logs to console ONLY in 'dev' profile.
     * Never leaks whether email exists.
     * Returns the created token ID (or null if user does not exist or rate limited).
     */
    @Transactional
    public Long createAndSendOtp(String email, String ip) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Check rate limiting
        if (rateLimiterService.isOtpSendBlocked(normalizedEmail)) {
            logger.warn("Password reset OTP requested but blocked by rate limit for email: {}", normalizedEmail);
            return null;
        }

        // Record attempt against rate limit
        rateLimiterService.recordOtpSend(normalizedEmail);

        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            // User does not exist, silent return (prevents enumeration)
            return null;
        }

        User user = userOpt.get();
        if (!user.isEnabled() || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            // Disabled or suspended user
            return null;
        }

        // Generate 6-digit OTP code
        int codeInt = secureRandom.nextInt(1_000_000);
        String code = String.format("%06d", codeInt);

        // Hash code using BCrypt
        String codeHash = passwordEncoder.encode(code);

        // Expiration: 10 minutes from now
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        PasswordResetToken token = new PasswordResetToken(user, codeHash, expiresAt);
        token = passwordResetTokenRepository.save(token);

        // Dispatch OTP
        dispatchOtp(normalizedEmail, code);

        return token.getId();
    }

    private void dispatchOtp(String toEmail, String code) {
        String mailHost = environment.getProperty("spring.mail.host");
        if (mailSender != null && mailHost != null && !mailHost.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject("Your FamilyCare Password Reset Verification Code");
                message.setText("Hello,\n\nYour 6-digit verification code to reset your FamilyCare password is:\n\n"
                        + code + "\n\nThis code will expire in 10 minutes.\n\n"
                        + "If you did not request a password reset, please ignore this email.");
                mailSender.send(message);
                logger.info("Password reset OTP sent via mail to {}", toEmail);
                return;
            } catch (Exception e) {
                logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            }
        }

        // If mail host not configured, log ONLY if 'dev' profile is active
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            logger.info("[DEV PROFILE] Password reset OTP for {}: {}", toEmail, code);
        }
    }

    /**
     * Step 2: Verify the 6-digit OTP code against token.
     * Enforces max 5 attempts before invalidating the token.
     */
    @Transactional
    public VerificationResult verifyOtp(Long tokenId, String rawCode) {
        if (tokenId == null || rawCode == null || rawCode.isBlank()) {
            return VerificationResult.failure("Invalid verification request.");
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findById(tokenId);
        if (tokenOpt.isEmpty()) {
            return VerificationResult.failure("Invalid or expired reset session. Please request a new code.");
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isUsed()) {
            return VerificationResult.failure("This verification code has already been used. Please request a new code.");
        }

        if (token.isExpired()) {
            return VerificationResult.failure("This verification code has expired. Please request a new code.");
        }

        // Increment attempts
        int attempts = token.getAttempts() + 1;
        token.setAttempts(attempts);

        if (attempts > 5) {
            token.setUsed(true);
            passwordResetTokenRepository.save(token);
            return VerificationResult.failure("Too many invalid attempts. This code has been invalidated. Please request a new one.");
        }

        boolean matches = passwordEncoder.matches(rawCode.trim(), token.getCodeHash());
        if (matches) {
            passwordResetTokenRepository.save(token);
            return VerificationResult.success(token);
        } else {
            if (attempts >= 5) {
                token.setUsed(true);
                passwordResetTokenRepository.save(token);
                return VerificationResult.failure("Too many invalid attempts. This code has been invalidated. Please request a new one.");
            }
            passwordResetTokenRepository.save(token);
            int remaining = 5 - attempts;
            return VerificationResult.failure("Invalid verification code. " + remaining + " attempts remaining.");
        }
    }

    /**
     * Step 3: Set new password for verified token.
     * Validates password strength, hashes password, updates user, marks token used, and writes audit log.
     */
    @Transactional
    public ResetResult resetPassword(Long tokenId, String newPassword, String confirmPassword, String ip) {
        if (newPassword == null || newPassword.isBlank()) {
            return ResetResult.failure("Password is required.");
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResetResult.failure("Passwords do not match.");
        }

        if (!isValidPasswordStrength(newPassword)) {
            return ResetResult.failure("Password must be at least 8 characters long and contain at least one uppercase letter and one digit.");
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findById(tokenId);
        if (tokenOpt.isEmpty()) {
            return ResetResult.failure("Invalid or expired session. Please start over.");
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isUsed() || token.isExpired()) {
            return ResetResult.failure("Verification session is invalid or has expired. Please request a new code.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token used
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        // Audit log: PASSWORD_RESET
        auditService.log(user.getId(), user.getEmail(), "PASSWORD_RESET", "USER", user.getId(), "Password reset successfully via OTP", ip);

        return ResetResult.success();
    }

    private boolean isValidPasswordStrength(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[0-9].*");
    }

    public static class VerificationResult {
        private final boolean successful;
        private final String message;
        private final PasswordResetToken token;

        private VerificationResult(boolean successful, String message, PasswordResetToken token) {
            this.successful = successful;
            this.message = message;
            this.token = token;
        }

        public static VerificationResult success(PasswordResetToken token) {
            return new VerificationResult(true, "Code verified successfully.", token);
        }

        public static VerificationResult failure(String message) {
            return new VerificationResult(false, message, null);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }

        public PasswordResetToken getToken() {
            return token;
        }
    }

    public static class ResetResult {
        private final boolean successful;
        private final String message;

        private ResetResult(boolean successful, String message) {
            this.successful = successful;
            this.message = message;
        }

        public static ResetResult success() {
            return new ResetResult(true, "Password updated successfully.");
        }

        public static ResetResult failure(String message) {
            return new ResetResult(false, message);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public String getMessage() {
            return message;
        }
    }
}
