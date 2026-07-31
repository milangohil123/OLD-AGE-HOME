package com.oldagehome.portal.config;

import com.oldagehome.portal.auth.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final com.oldagehome.portal.audit.AuditService auditService;

        public SecurityConfig(CustomUserDetailsService userDetailsService,
                        com.oldagehome.portal.audit.AuditService auditService) {
                this.userDetailsService = userDetailsService;
                this.auditService = auditService;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                new AntPathRequestMatcher("/login"),
                                                                new AntPathRequestMatcher("/login/**"),
                                                                new AntPathRequestMatcher("/css/**"),
                                                                new AntPathRequestMatcher("/js/**"),
                                                                new AntPathRequestMatcher("/images/**"),
                                                                new AntPathRequestMatcher("/uploads/**"),
                                                                new AntPathRequestMatcher("/favicon.ico"),
                                                                new AntPathRequestMatcher("/error/**"))
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler((request, response, authentication) -> {
                                                        auditService.logActivity(
                                                                        com.oldagehome.portal.audit.AuditModule.AUTH,
                                                                        com.oldagehome.portal.audit.AuditAction.LOGIN,
                                                                        "User " + authentication.getName()
                                                                                        + " logged in successfully",
                                                                        "User", null, true,
                                                                        null);
                                                        response.sendRedirect(request.getContextPath() + "/dashboard");
                                                })
                                                .failureHandler((request, response, exception) -> {
                                                        String username = request.getParameter("username");
                                                        auditService.logActivity(
                                                                        com.oldagehome.portal.audit.AuditModule.AUTH,
                                                                        com.oldagehome.portal.audit.AuditAction.LOGIN,
                                                                        "Failed login attempt for user: " + username,
                                                                        "User", null, false,
                                                                        exception.getMessage());
                                                        response.sendRedirect(
                                                                        request.getContextPath() + "/login?error=true");
                                                })
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .sessionManagement(session -> session
                                                .invalidSessionUrl("/login?timeout=true")
                                                .maximumSessions(3)
                                                .sessionRegistry(sessionRegistry())
                                                .expiredUrl("/login?timeout=true"))
                                .headers(headers -> headers
                                                .cacheControl(cache -> cache.disable())

                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; " +
                                                                                "img-src 'self' data: blob: https:; " +
                                                                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                                                                                +
                                                                                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                                                                                +
                                                                                "font-src 'self' data: https://fonts.gstatic.com https://cdn.jsdelivr.net; "
                                                                                +
                                                                                "connect-src 'self';"))

                                                .frameOptions(frame -> frame.sameOrigin())

                                                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)))
                                .exceptionHandling(exception -> exception
                                                .accessDeniedPage("/403"));

                http.authenticationProvider(authenticationProvider());

                return http.build();
        }

        /**
         * SessionRegistry tracks all active sessions in memory.
         * Required for correct concurrent session enforcement when maximumSessions > 0.
         * Without an explicit bean, Spring Security creates an internal one that may
         * not be properly wired when customizing session management.
         */
        @Bean
        public SessionRegistry sessionRegistry() {
                return new SessionRegistryImpl();
        }

        /**
         * HttpSessionEventPublisher notifies Spring Security when an HttpSession
         * is created or destroyed by the Servlet container.
         * This is REQUIRED for maximumSessions to correctly release expired session
         * slots. Without it, logged-out or timed-out sessions are never deregistered
         * from the SessionRegistry, causing the concurrent session count to creep up
         * until every new login is incorrectly rejected or the wrong session is evicted.
         */
        @Bean
        public HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
        }
}
