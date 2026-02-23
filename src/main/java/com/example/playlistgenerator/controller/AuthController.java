package com.example.playlistgenerator.controller;

import com.example.playlistgenerator.domain.UserSession;
import com.example.playlistgenerator.domain.dto.SpotifyUserProfile;
import com.example.playlistgenerator.exception.UnauthorizedException;
import com.example.playlistgenerator.service.SessionService;
import com.example.playlistgenerator.service.SpotifyAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    static final String SESSION_USER_ID = "SESSION_USER_ID";
    static final String SESSION_STATE = "OAUTH_STATE";

    private final SpotifyAuthService spotifyAuthService;
    private final SessionService sessionService;

    public AuthController(SpotifyAuthService spotifyAuthService, SessionService sessionService) {
        this.spotifyAuthService = spotifyAuthService;
        this.sessionService = sessionService;
    }

    @GetMapping("/spotify/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Spotify only allows 127.0.0.1 as redirect URI for local dev, not localhost.
        // If the browser opened the app on localhost, bounce it to 127.0.0.1 first so
        // the JSESSIONID cookie domain matches the callback origin and state validation works.
        if ("localhost".equals(request.getServerName())) {
            int port = request.getServerPort();
            String bounceUrl = "http://127.0.0.1:" + port + "/api/auth/spotify/login";
            log.info("Bouncing localhost request to {} to align OAuth cookie domain", bounceUrl);
            response.sendRedirect(bounceUrl);
            return;
        }
        String state = UUID.randomUUID().toString().replace("-", "");
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_STATE, state);
        String authUrl = spotifyAuthService.buildAuthorizationUrl(state);
        log.info("Redirecting to Spotify authorization");
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public void callback(
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String state,
        @RequestParam(required = false) String error,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        // Spotify sends ?error=access_denied&state=... (no `code`) when the user denies access.
        // `code` must be optional — otherwise Spring throws MissingServletRequestParameterException
        // before this method runs, making the error branch below unreachable.
        if (error != null) {
            log.warn("Spotify OAuth error: {}", error);
            invalidateSession(request, response);
            response.sendRedirect("/?error=spotify_auth_denied");
            return;
        }
        if (code == null) {
            log.warn("OAuth callback received with no code and no error — rejecting");
            invalidateSession(request, response);
            response.sendRedirect("/?error=missing_code");
            return;
        }

        // Session MUST be present: we stored STATE in it during /login.
        // If it's missing, the JSESSIONID cookie was not sent — most likely the
        // redirect_uri hostname (e.g. 127.0.0.1) differs from the hostname the user
        // opened the app on (e.g. localhost), causing the browser to drop the cookie.
        // Reject the request to prevent CSRF and to surface the misconfiguration.
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("OAuth callback received with no session — possible hostname mismatch between app URL and redirect_uri");
            response.sendRedirect("/?error=session_missing");
            return;
        }
        String savedState = (String) session.getAttribute(SESSION_STATE);
        if (savedState == null || !savedState.equals(state)) {
            log.warn("State mismatch in OAuth callback — possible CSRF attempt");
            invalidateSession(request, response);
            response.sendRedirect("/?error=state_mismatch");
            return;
        }

        try {
            Map<String, Object> tokens = spotifyAuthService.exchangeCodeForTokens(code);
            SpotifyUserProfile profile = spotifyAuthService.getUserProfile((String) tokens.get("access_token"));
            UserSession userSession = sessionService.createOrUpdateSession(
                profile,
                (String) tokens.get("access_token"),
                (String) tokens.get("refresh_token"),
                (Integer) tokens.get("expires_in")
            );

            HttpSession httpSession = request.getSession(true);
            httpSession.setAttribute(SESSION_USER_ID, userSession.getId());
            log.info("User {} logged in successfully", profile.displayName());
            response.sendRedirect("/");
        } catch (Exception e) {
            log.error("OAuth callback failed", e);
            invalidateSession(request, response);
            response.sendRedirect("/?error=auth_failed");
        }
    }

    private void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SESSION_USER_ID) == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        UUID sessionId = (UUID) session.getAttribute(SESSION_USER_ID);
        try {
            UserSession userSession = sessionService.getSession(sessionId);
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "spotifyUserId", userSession.getSpotifyUserId(),
                "displayName", userSession.getSpotifyDisplayName() != null ? userSession.getSpotifyDisplayName() : userSession.getSpotifyUserId()
            ));
        } catch (UnauthorizedException e) {
            session.invalidate();
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            UUID sessionId = (UUID) session.getAttribute(SESSION_USER_ID);
            if (sessionId != null) {
                try { sessionService.deleteSession(sessionId); } catch (Exception ignored) {}
            }
            session.invalidate();
        }
        // Explicitly expire the JSESSIONID cookie in the browser
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}
