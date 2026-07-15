package com.codeduels.auth.controller;

import com.codeduels.auth.dto.LoginRequest;
import com.codeduels.auth.dto.RegisterRequest;
import com.codeduels.auth.dto.TokenResponse;
import com.codeduels.auth.service.AuthService;
import com.codeduels.auth.service.LoginResult;
import com.codeduels.common.api.StandardResponse;
import com.codeduels.common.exception.InvalidCredentialsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<StandardResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        log.debug("Registered new user");
        return ResponseEntity.status(HttpStatus.CREATED).body(StandardResponse.success(null));
    }

    @PostMapping("/login")
    public ResponseEntity<StandardResponse<TokenResponse>> login(@RequestBody LoginRequest loginRequest) {
        LoginResult tokens = authService.login(loginRequest);

        ResponseCookie cookie=ResponseCookie.from("refresh_token",tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(new TokenResponse(tokens.accessToken())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<StandardResponse<TokenResponse>> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        log.debug("Refresh token{}", refreshToken);
        if (refreshToken == null) throw new InvalidCredentialsException("Invalid refresh token");

        LoginResult result = authService.refresh(refreshToken);
        ResponseCookie cookie =ResponseCookie.from("refresh_token",result.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(StandardResponse.success(new TokenResponse(result.accessToken())));
    }

}
