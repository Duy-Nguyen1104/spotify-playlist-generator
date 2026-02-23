package com.example.playlistgenerator.service;

import com.example.playlistgenerator.config.AppProperties;
import com.example.playlistgenerator.domain.dto.SpotifyUserProfile;
import com.example.playlistgenerator.exception.SpotifyApiException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

// starting with spotify oauth flow: authorization -> access token ->
@Service
@AllArgsConstructor
public class SpotifyAuthService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAuthService.class);
    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public String buildAuthorizationUrl(String state) {
        AppProperties.Spotify spotify = appProperties.getSpotify();
        String scopes = String.join(" ", spotify.getScopes());
        return SPOTIFY_AUTH_URL + "?" +
            "client_id=" + spotify.getClientId() +
            "&response_type=code" +
            "&redirect_uri=" + encodeParam(spotify.getRedirectUri()) +
            "&scope=" + encodeParam(scopes) +
            "&state=" + encodeParam(state) +
            "&show_dialog=true";
    }

    public Map<String, Object> exchangeCodeForTokens(String code) {
        AppProperties.Spotify spotify = appProperties.getSpotify();
        // Spotify requires HTTP Basic Authentication for token exchange
        // Format: Basic base64(client_id:client_secret)
        // so we convert it to bytes and encode in Base64
        String credentials = Base64.getEncoder().encodeToString(
            (spotify.getClientId() + ":" + spotify.getClientSecret()).getBytes(StandardCharsets.UTF_8)
        );

        // form url-encoded POST body
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", spotify.getRedirectUri());

        try {
            String response = restClient.post()
                .uri(SPOTIFY_TOKEN_URL)
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(String.class); // turn body response into String

            // Convert Json String to Json tree -> access fields easier
            JsonNode node = objectMapper.readTree(response);
            return Map.of(
                "access_token", node.get("access_token").asString(),
                    "refresh_token", node.has("refresh_token") ? node.get("refresh_token").asString() : "",
                    "expires_in", node.get("expires_in").asInt()
            );
        } catch (Exception e) {
            log.error("Failed to exchange code for tokens", e);
            throw new SpotifyApiException("Failed to exchange authorization code for tokens", e);
        }
    }

    // after having the access token, we have a method to refresh it using refresh token
    // send to the same token url, but with refresh_token and grant_type
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        AppProperties.Spotify spotify = appProperties.getSpotify();
        String credentials = Base64.getEncoder().encodeToString(
            (spotify.getClientId() + ":" + spotify.getClientSecret()).getBytes(StandardCharsets.UTF_8)
        );

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);

        // post to the uri using the rest client
        try {
            String response = restClient.post()
                .uri(SPOTIFY_TOKEN_URL)
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            return Map.of(
                "access_token", node.get("access_token").asString(),
                "expires_in", node.get("expires_in").asInt()
            );
        } catch (Exception e) {
            log.error("Failed to refresh access token", e);
            throw new SpotifyApiException("Failed to refresh access token", e);
        }
    }

    public SpotifyUserProfile getUserProfile(String accessToken) {
        try {
            String response = restClient.get()
                .uri(SPOTIFY_API_BASE + "/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            String id = node.get("id").asString();
            String displayName = node.has("display_name") && !node.get("display_name").isNull()
                ? node.get("display_name").asString() : id;

            return new SpotifyUserProfile(id, displayName);
        } catch (Exception e) {
            log.error("Failed to get user profile", e);
            throw new SpotifyApiException("Failed to get user profile", e);
        }
    }

    private String encodeParam(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
