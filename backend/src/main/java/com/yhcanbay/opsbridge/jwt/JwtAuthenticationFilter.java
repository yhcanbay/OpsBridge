package com.yhcanbay.opsbridge.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "OPSBRIDGE_AUTH";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RequestAttributeSecurityContextRepository requestContext;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        if (cookie != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(cookie.getValue(), request, response);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request, HttpServletResponse response) {
        try {
            Claims claims = jwtService.readActiveToken(token);
            UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            user.getUsername(), null, user.getAuthorities());
            authentication.setDetails(claims);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // JWT doğrulaması bu istek için geçerlidir ve girişteki CSRF yenilemesini tetiklemez.
            requestContext.saveContext(context, request, response);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            // Kimlik oluşturulmaz. Spring Security korumalı adreslerde 401 döndürür.
        }
    }
}
