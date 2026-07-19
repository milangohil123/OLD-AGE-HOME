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
                                                .maximumSessions(1)
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
}
