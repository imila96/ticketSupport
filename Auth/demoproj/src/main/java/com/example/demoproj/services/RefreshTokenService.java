package com.example.demoproj.services;

import com.example.demoproj.config.ApplicationConfig;
import com.example.demoproj.dto.model.RefreshToken;
import com.example.demoproj.dto.model.User;
import com.example.demoproj.dto.request.RefreshTokenRequest;
import com.example.demoproj.dto.response.RefreshTokenResponse;
import com.example.demoproj.exception.TokenException;
import com.example.demoproj.exception.UserNotFoundException;
import com.example.demoproj.repository.RefreshTokenRepository;
import com.example.demoproj.repository.UserRepository;
import com.example.demoproj.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ApplicationConfig appConfig;

    public String generateRefreshToken(Integer userId) {

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId, "User not found"));

        RefreshToken refreshToken = RefreshToken.builder()
                .token(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()))
                .user(user)
                .expires(new Date(System.currentTimeMillis() + (appConfig.getRefreshTokenExpireTime() * 86400 * 1000)))
                .blackListed(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public RefreshTokenResponse generateNewTokens(RefreshTokenRequest refreshTokenRequest) {

        User user = refreshTokenRepository.findByToken(refreshTokenRequest.getRefreshToken())
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .orElseThrow(() -> new TokenException(refreshTokenRequest.getRefreshToken(), "Refresh token does not exist"));

        String token = jwtUtil.generateToken(user);


        return RefreshTokenResponse.builder()
                .accessToken(token)
                .refreshToken(refreshTokenRequest.getRefreshToken())
                .build();
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpires().compareTo(Date.from(Instant.now())) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }
}
