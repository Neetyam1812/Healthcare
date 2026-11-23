package Healthcare.HC.Controller;

import Healthcare.HC.Entity.PasswordResetToken;
import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.PasswordResetTokenRepository;
import Healthcare.HC.Repository.UserRepository;
import Healthcare.HC.Service.AuditService;
import Healthcare.HC.Service.PasswordResetService;
import Healthcare.HC.Service.PasswordResetService.ResetResult;
import Healthcare.HC.Service.PasswordResetService.VerificationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService,
                          PasswordResetService passwordResetService,
                          PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.passwordResetService = passwordResetService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "reset", required = false) String reset,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email address or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been safely signed out of your account.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created successfully! Please sign in with your credentials.");
        }
        if (reset != null) {
            model.addAttribute("successMessage", "Password updated successfully. Please sign in with your new password.");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String register(
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        if ("exists".equals(error)) {
            model.addAttribute("errorMessage", "An account with this email address already exists.");
        } else if ("validation".equals(error) && !model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Please provide valid registration details.");
        }
        return "auth/register";
    }

    /**
     * POST /register:
     * - Server-side validation:
     *   - full name 2-100 chars
     *   - valid email
     *   - password >= 8 chars with >= 1 uppercase letter and >= 1 digit
     *   - confirm password matches
     *   - optional phone (length <= 20)
     * - Lowercase email
     * - Reject duplicates with message: "An account with this email address already exists."
     * - Hash password with PasswordEncoder (BCrypt)
     * - Force role FAMILY_ADMIN, platformRole null (never read role/platformRole from request params)
     * - Audit log: REGISTER
     */
    @PostMapping("/register")
    public String handleRegisterSubmit(
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String ip = getClientIp(request);

        // Validation: Full name
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            redirectAttributes.addFlashAttribute("errorMessage", "Full name must be between 2 and 100 characters.");
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phoneNumber", phoneNumber);
            return "redirect:/register?error=validation";
        }

        // Validation: Email
        if (email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please enter a valid email address.");
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("phoneNumber", phoneNumber);
            return "redirect:/register?error=validation";
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Validation: Duplicate check
        if (userRepository.existsByEmail(normalizedEmail)) {
            redirectAttributes.addFlashAttribute("errorMessage", "An account with this email address already exists.");
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("phoneNumber", phoneNumber);
            return "redirect:/register?error=exists";
        }

        // Validation: Password strength
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[0-9].*")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 8 characters long and contain at least one uppercase letter and one digit.");
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phoneNumber", phoneNumber);
            return "redirect:/register?error=validation";
        }

        // Validation: Confirm password
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("phoneNumber", phoneNumber);
            return "redirect:/register?error=validation";
        }

        // Construct User: forced role FAMILY_ADMIN, platformRole null
        User newUser = new User();
        newUser.setFullName(fullName.trim());
        newUser.setEmail(normalizedEmail);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole("FAMILY_ADMIN");
        newUser.setPlatformRole(null);
        newUser.setStatus("ACTIVE");
        newUser.setEnabled(true);

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String trimmedPhone = phoneNumber.trim();
            if (trimmedPhone.length() <= 20) {
                newUser.setPhoneNumber(trimmedPhone);
            }
        }

        User savedUser = userRepository.save(newUser);

        // Audit log: REGISTER
        auditService.log(savedUser.getId(), savedUser.getEmail(), "REGISTER", "USER", savedUser.getId(), "User registered successfully", ip);

        return "redirect:/login?registered=true";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam(value = "step", required = false, defaultValue = "1") String step,
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        model.addAttribute("currentStep", step);
        if ("invalid".equals(error) && !model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", "Invalid code or expired session. Please try again.");
        }
        return "auth/forgot-password";
    }

    /**
     * Fallback POST endpoint for forgot-password.
     * Prevents resetting any account without verified OTP!
     */
    @PostMapping("/forgot-password")
    public String handleForgotPasswordSubmit(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "otp", required = false) String otp,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String ip = getClientIp(request);

        // If user submitted an email and no OTP or newPassword: step 1
        if (email != null && !email.isBlank() && (otp == null || otp.isBlank()) && (newPassword == null || newPassword.isBlank())) {
            Long tokenId = passwordResetService.createAndSendOtp(email, ip);
            session.setAttribute("RESET_EMAIL", email.trim().toLowerCase());
            if (tokenId != null) {
                session.setAttribute("RESET_TOKEN_ID", tokenId);
            }
            session.removeAttribute("RESET_TOKEN_VERIFIED");
            return "redirect:/forgot-password?step=2";
        }

        // If user submitted OTP: step 2
        if (otp != null && !otp.isBlank() && (newPassword == null || newPassword.isBlank())) {
            Long tokenId = (Long) session.getAttribute("RESET_TOKEN_ID");
            VerificationResult result = passwordResetService.verifyOtp(tokenId, otp);
            if (result.isSuccessful()) {
                session.setAttribute("RESET_TOKEN_VERIFIED", true);
                return "redirect:/forgot-password?step=3";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", result.getMessage());
                return "redirect:/forgot-password?step=2";
            }
        }

        // If user submitted newPassword: requires verified OTP token!
        if (newPassword != null && !newPassword.isBlank()) {
            Long tokenId = (Long) session.getAttribute("RESET_TOKEN_ID");
            Boolean isVerified = (Boolean) session.getAttribute("RESET_TOKEN_VERIFIED");

            // Direct API call fallback: if session attributes are missing, check if email + otp were provided
            if ((tokenId == null || !Boolean.TRUE.equals(isVerified)) && email != null && otp != null) {
                User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
                if (user != null) {
                    PasswordResetToken token = passwordResetTokenRepository
                            .findFirstByUserAndUsedFalseOrderByCreatedAtDesc(user)
                            .orElse(null);
                    if (token != null) {
                        VerificationResult vRes = passwordResetService.verifyOtp(token.getId(), otp);
                        if (vRes.isSuccessful()) {
                            tokenId = token.getId();
                            isVerified = true;
                        }
                    }
                }
            }

            if (tokenId == null || !Boolean.TRUE.equals(isVerified)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Password reset failed. Invalid or unverified OTP.");
                return "redirect:/forgot-password?step=1&error=invalid";
            }

            ResetResult resetResult = passwordResetService.resetPassword(tokenId, newPassword, newPassword, ip);
            if (resetResult.isSuccessful()) {
                session.removeAttribute("RESET_TOKEN_ID");
                session.removeAttribute("RESET_TOKEN_VERIFIED");
                session.removeAttribute("RESET_EMAIL");
                return "redirect:/login?reset=true";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", resetResult.getMessage());
                return "redirect:/forgot-password?step=3";
            }
        }

        return "redirect:/forgot-password?step=1";
    }

    // =========================================================================
    // Explicit Form POST Endpoints for Forgot Password Steps
    // =========================================================================

    @PostMapping("/forgot-password/step1")
    public String handleStep1Submit(
            @RequestParam("email") String email,
            HttpServletRequest request,
            HttpSession session) {
        String ip = getClientIp(request);
        Long tokenId = passwordResetService.createAndSendOtp(email, ip);
        session.setAttribute("RESET_EMAIL", email.trim().toLowerCase());
        if (tokenId != null) {
            session.setAttribute("RESET_TOKEN_ID", tokenId);
        }
        session.removeAttribute("RESET_TOKEN_VERIFIED");
        return "redirect:/forgot-password?step=2";
    }

    @PostMapping("/forgot-password/step2")
    public String handleStep2Submit(
            @RequestParam("otp") String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long tokenId = (Long) session.getAttribute("RESET_TOKEN_ID");
        VerificationResult result = passwordResetService.verifyOtp(tokenId, otp);
        if (result.isSuccessful()) {
            session.setAttribute("RESET_TOKEN_VERIFIED", true);
            return "redirect:/forgot-password?step=3";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.getMessage());
            return "redirect:/forgot-password?step=2";
        }
    }

    @PostMapping("/forgot-password/step3")
    public String handleStep3Submit(
            @RequestParam("newPassword") String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Boolean isVerified = (Boolean) session.getAttribute("RESET_TOKEN_VERIFIED");
        Long tokenId = (Long) session.getAttribute("RESET_TOKEN_ID");

        if (tokenId == null || !Boolean.TRUE.equals(isVerified)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Session not verified. Please start over.");
            return "redirect:/forgot-password?step=1&error=invalid";
        }

        String ip = getClientIp(request);
        ResetResult result = passwordResetService.resetPassword(
                tokenId,
                newPassword,
                confirmPassword != null ? confirmPassword : newPassword,
                ip
        );

        if (result.isSuccessful()) {
            session.removeAttribute("RESET_TOKEN_ID");
            session.removeAttribute("RESET_TOKEN_VERIFIED");
            session.removeAttribute("RESET_EMAIL");
            return "redirect:/login?reset=true";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result.getMessage());
            return "redirect:/forgot-password?step=3";
        }
    }

    // =========================================================================
    // AJAX Endpoints for Client-Side Stepper Flow (auth.js)
    // =========================================================================

    @PostMapping("/forgot-password/send-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendOtp(
            @RequestParam("email") String email,
            HttpServletRequest request,
            HttpSession session) {
        String ip = getClientIp(request);
        Long tokenId = passwordResetService.createAndSendOtp(email, ip);

        String normalizedEmail = (email != null) ? email.trim().toLowerCase() : "";
        session.setAttribute("RESET_EMAIL", normalizedEmail);
        if (tokenId != null) {
            session.setAttribute("RESET_TOKEN_ID", tokenId);
        }
        session.removeAttribute("RESET_TOKEN_VERIFIED");

        // Always return generic success response
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If an account exists for this email, a verification code has been dispatched.",
                "email", normalizedEmail
        ));
    }

    @PostMapping("/forgot-password/verify-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestParam("otp") String otp,
            HttpSession session) {
        Long tokenId = (Long) session.getAttribute("RESET_TOKEN_ID");
        VerificationResult result = passwordResetService.verifyOtp(tokenId, otp);

        if (result.isSuccessful()) {
            session.setAttribute("RESET_TOKEN_VERIFIED", true);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", result.getMessage()
            ));
        }
    }

    @PostMapping("/forgot-password/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestParam("newPassword") String newPassword,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpServletRequest request,
            HttpSession session) {
        Boolean isVerified = (Boolean) session.getAttribute("RESET_TOKEN_VERIFIED");
        Long tokenId = (Long) session.getAttribute("RESET_TOKEN_ID");

        if (tokenId == null || !Boolean.TRUE.equals(isVerified)) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Verification session is invalid or has expired. Please start over."
            ));
        }

        String ip = getClientIp(request);
        ResetResult result = passwordResetService.resetPassword(
                tokenId,
                newPassword,
                confirmPassword != null ? confirmPassword : newPassword,
                ip
        );

        if (result.isSuccessful()) {
            session.removeAttribute("RESET_TOKEN_ID");
            session.removeAttribute("RESET_TOKEN_VERIFIED");
            session.removeAttribute("RESET_EMAIL");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result.getMessage()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", result.getMessage()
            ));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
