package com.yhcanbay.opsbridge.controller;

import java.security.Principal;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;

import com.yhcanbay.opsbridge.dto.response.CsrfResponse;
import com.yhcanbay.opsbridge.dto.response.UserResponse;

public interface IAuthController {

    @GetMapping("/csrf")
    CsrfResponse getCsrfToken(CsrfToken csrfToken);

    @GetMapping("/me")
    UserResponse getCurrentUser(Principal principal);
}
