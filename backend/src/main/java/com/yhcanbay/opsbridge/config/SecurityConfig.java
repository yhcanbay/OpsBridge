package com.yhcanbay.opsbridge.config;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(
            @Value("${opsbridge.admin.username}") String username,
            @Value("${opsbridge.admin.password}") String password,
            PasswordEncoder encoder) {
        if (username.isBlank() || password.isBlank() || password.startsWith("replace-with-")) {
            throw new IllegalStateException(
                    "Set OPSBRIDGE_ADMIN_USERNAME and OPSBRIDGE_ADMIN_PASSWORD before starting.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(password))
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth ->
                        auth.requestMatchers(
                                        "/api/auth/csrf",
                                        "/api/auth/login",
                                        "/actuator/health",
                                        "/error")
                                .permitAll()
                                .anyRequest()
                                .hasRole("ADMIN"))
                .requestCache(cache -> cache.disable())
                .formLogin(form ->
                        form.loginProcessingUrl("/api/auth/login")
                                .successHandler((request, response, authentication) ->
                                        response.setStatus(HttpServletResponse.SC_NO_CONTENT))
                                .failureHandler((request, response, exception) ->
                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
                .logout(logout ->
                        logout.logoutUrl("/api/auth/logout")
                                .invalidateHttpSession(true)
                                .deleteCookies("JSESSIONID")
                                .logoutSuccessHandler((request, response, authentication) ->
                                        response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(errors ->
                        errors.authenticationEntryPoint((request, response, exception) ->
                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                                .accessDeniedHandler((request, response, exception) ->
                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN)))
                .build(); // Oturum + CSRF koruması Spring Security varsayılanlarıyla açık.
    }
}
