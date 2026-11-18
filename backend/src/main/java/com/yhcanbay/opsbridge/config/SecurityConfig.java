package com.yhcanbay.opsbridge.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import com.yhcanbay.opsbridge.jwt.JwtAuthenticationFilter;
import com.yhcanbay.opsbridge.jwt.JwtService;

import io.jsonwebtoken.Claims;
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
    SecurityFilterChain security(
            HttpSecurity http,
            UserDetailsService users,
            JwtService jwtService,
            @Value("${opsbridge.auth.cookie-secure}") boolean forceSecureCookie) throws Exception {
        CookieCsrfTokenRepository csrfTokens = new CookieCsrfTokenRepository();
        csrfTokens.setCookieName("OPSBRIDGE_CSRF");
        csrfTokens.setHeaderName("X-CSRF-TOKEN");
        csrfTokens.setCookieCustomizer(cookie -> {
            cookie.sameSite("Strict");
            if (forceSecureCookie) {
                cookie.secure(true);
            }
        });

        http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokens));
        RequestAttributeSecurityContextRepository requestContext = new RequestAttributeSecurityContextRepository();
        http.securityContext(context -> context.securityContextRepository(requestContext));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

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
                String token = jwtService.generateToken(authentication.getName());
                boolean secure = forceSecureCookie || request.isSecure();
                writeAuthCookie(response, token, JwtService.TOKEN_LIFETIME, secure);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
            form.failureHandler((request, response, exception) -> {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            });
        });

        http.logout(logout -> {
            logout.logoutUrl("/api/auth/logout");
            logout.addLogoutHandler((request, response, authentication) -> {
                if (authentication != null && authentication.getDetails() instanceof Claims claims) {
                    jwtService.revokeToken(claims);
                }
                boolean secure = forceSecureCookie || request.isSecure();
                writeAuthCookie(response, "", Duration.ZERO, secure);
            });
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

        // Çıkış isteğinin hangi JWT'yi iptal edeceğini belirlemek için filtre önce çalışır.
        http.addFilterBefore(new JwtAuthenticationFilter(jwtService, users, requestContext), LogoutFilter.class);
        return http.build();
    }

    private void writeAuthCookie(
            HttpServletResponse response, String token, Duration maxAge, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
