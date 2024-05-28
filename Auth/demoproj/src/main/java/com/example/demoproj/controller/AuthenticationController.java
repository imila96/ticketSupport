package com.example.demoproj.controller;

import com.example.demoproj.dto.request.AuthenticateRequest;
import com.example.demoproj.dto.request.RefreshTokenRequest;
import com.example.demoproj.dto.request.RegisterRequest;
import com.example.demoproj.dto.response.AuthenticationResponse;
import com.example.demoproj.dto.response.RefreshTokenResponse;
import com.example.demoproj.services.AuthenticationService;
import com.example.demoproj.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticateRequest request
    ) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse
    refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return refreshTokenService.generateNewTokens(refreshTokenRequest);
    }
}