package com.yhcanbay.opsbridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

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
        String encodedPassword = encoder.encode(password);
        UserDetails adminUser = User.withUsername(username)
                .password(encodedPassword)
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(adminUser);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/api/auth/csrf", "/api/auth/login", "/actuator/health", "/error")
                    .permitAll();
            auth.anyRequest().hasRole("ADMIN");
        });

        http.requestCache(cache -> {
            cache.disable();
        });

        http.formLogin(form -> {
            form.loginProcessingUrl("/api/auth/login");
            form.successHandler((request, response, authentication) -> {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
            form.failureHandler((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            });
        });

        http.logout(logout -> {
            logout.logoutUrl("/api/auth/logout");
            logout.invalidateHttpSession(true);
            logout.deleteCookies("JSESSIONID");
            logout.logoutSuccessHandler((request, response, authentication) -> {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
        });

        http.exceptionHandling(errors -> {
            errors.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            });
            errors.accessDeniedHandler((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            });
        });

        // Oturum ve CSRF koruması Spring Security varsayılanlarıyla açık.
        return http.build();
    }
}
