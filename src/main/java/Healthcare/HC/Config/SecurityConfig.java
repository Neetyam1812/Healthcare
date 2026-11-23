package Healthcare.HC.Config;

import Healthcare.HC.Security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final RoleBasedSuccessHandler roleBasedSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final RateLimitLoginFilter rateLimitLoginFilter;

    @Value("${COOKIE_SECURE:false}")
    private boolean cookieSecure;

    public SecurityConfig(AppUserDetailsService appUserDetailsService,
                          RoleBasedSuccessHandler roleBasedSuccessHandler,
                          CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
                          CustomAccessDeniedHandler customAccessDeniedHandler,
                          RateLimitLoginFilter rateLimitLoginFilter) {
        this.appUserDetailsService = appUserDetailsService;
        this.roleBasedSuccessHandler = roleBasedSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.rateLimitLoginFilter = rateLimitLoginFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public static ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitLoginFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/login",
                    "/register",
                    "/forgot-password/**",
                    "/error",
                    "/error/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll()
                .requestMatchers("/actuator/health").hasAnyRole("SUPER_ADMIN", "SUPPORT_AGENT", "ANALYST")
                .requestMatchers("/actuator/**").denyAll()
                .requestMatchers("/admin/staff/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "SUPPORT_AGENT", "ANALYST")
                .requestMatchers("/app/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(roleBasedSuccessHandler)
                .failureHandler(customAuthenticationFailureHandler)
                .permitAll()
            )
            .rememberMe(rm -> rm
                .rememberMeParameter("remember")
                .tokenValiditySeconds(14 * 24 * 60 * 60) // 14 days
                .userDetailsService(appUserDetailsService)
                .key("HealthCareSecretRememberMeKey_2026")
                .useSecureCookie(cookieSecure)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation(sf -> sf.changeSessionId())
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry())
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(fo -> fo.deny())
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
            );

        return http.build();
    }
}
