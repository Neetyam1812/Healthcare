package Healthcare.HC.Security;

import Healthcare.HC.Entity.User;
import Healthcare.HC.Repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) {
            throw new UsernameNotFoundException("Invalid email address or password.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email address or password."));

        // Authorities: platformRole == null -> ROLE_USER; otherwise ROLE_<platformRole>
        List<GrantedAuthority> authorities;
        if (user.getPlatformRole() == null || user.getPlatformRole().isBlank()) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getPlatformRole().trim()));
        }

        // enabled: enabled && status == ACTIVE
        boolean isUserEnabled = user.isEnabled() && "ACTIVE".equalsIgnoreCase(user.getStatus());

        return new AppUserDetails(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                isUserEnabled
        );
    }
}
