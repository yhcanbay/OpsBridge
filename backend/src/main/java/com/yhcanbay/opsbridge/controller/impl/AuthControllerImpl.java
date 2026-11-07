package com.yhcanbay.opsbridge.controller.impl;

import java.security.Principal;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yhcanbay.opsbridge.controller.IAuthController;
import com.yhcanbay.opsbridge.dto.response.CsrfResponse;
import com.yhcanbay.opsbridge.dto.response.UserResponse;

@RestController
@RequestMapping(path = "/api/auth")
public class AuthControllerImpl implements IAuthController {

    @Override
    public CsrfResponse getCsrfToken(CsrfToken csrfToken) {
        return CsrfResponse.builder()
                .headerName(csrfToken.getHeaderName())
                .token(csrfToken.getToken())
                .build();
    }

    @Override
    public UserResponse getCurrentUser(Principal principal) {
        return UserResponse.builder()
                .username(principal.getName())
                .role("ADMIN")
                .build();
    }
}
