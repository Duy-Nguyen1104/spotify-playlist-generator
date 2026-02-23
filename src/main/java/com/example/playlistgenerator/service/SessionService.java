package com.example.playlistgenerator.service;

import com.example.playlistgenerator.domain.UserSession;
import com.example.playlistgenerator.domain.dto.SpotifyUserProfile;
import com.example.playlistgenerator.exception.UnauthorizedException;
import com.example.playlistgenerator.repository.UserSessionRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// Managing session lifecycle
@Service
@AllArgsConstructor
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final UserSessionRepository userSessionRepository;
    private final SpotifyAuthService spotifyAuthService;

    @Transactional
    public UserSession createOrUpdateSession(SpotifyUserProfile profile, String accessToken, String refreshToken, int expiresIn) {
        // might not have the session || is Null -> Optional
        Optional<UserSession> existing = userSessionRepository.findBySpotifyUserId(profile.id());
        UserSession session = existing.orElse(new UserSession());
        session.setSpotifyUserId(profile.id());
        session.setSpotifyDisplayName(profile.displayName());
        session.setAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            session.setRefreshToken(refreshToken);
        }
        //subtract 60 seconds so that user does not use a token that is about to expire
        session.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn - 60));
        return userSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public UserSession getSession(UUID sessionId) {
        return userSessionRepository.findById(sessionId)
            .orElseThrow(() -> new UnauthorizedException("Session not found"));
    }

    @Transactional
    public UserSession refreshTokenIfNeeded(UserSession session) {
        // get the current expires in value , compare with now, if its smaller -> call spotify auth service to refresh
        if (session.getTokenExpiresAt().isBefore(Instant.now())) {
            log.info("Refreshing access token for user {}", session.getSpotifyUserId());
            // new access token and expires at
            var tokens = spotifyAuthService.refreshAccessToken(session.getRefreshToken());
            session.setAccessToken(tokens.get("access_token").toString());
            session.setTokenExpiresAt(Instant.now().plusSeconds((Integer) tokens.get("expires_in") - 60));
            return userSessionRepository.save(session);
        }
        return session;
    }

    @Transactional
    public void deleteSession(UUID sessionId) {
        userSessionRepository.deleteById(sessionId);
    }
}
