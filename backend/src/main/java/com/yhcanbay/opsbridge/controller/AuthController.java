package com.yhcanbay.opsbridge.controller;

import com.yhcanbay.opsbridge.dto.response.CsrfResponse;
import com.yhcanbay.opsbridge.dto.response.UserResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return new UserResponse(principal.getName(), "ADMIN");
    }
}
