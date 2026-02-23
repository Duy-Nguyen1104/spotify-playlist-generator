package com.example.playlistgenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import java.util.List;

// Map application properties that start with "app" to java object AppProperties
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Spotify spotify;
    private Gemini gemini;

    public Spotify getSpotify() { return this.spotify; }
    public void  setSpotify(Spotify spotify) { this.spotify = spotify; }
    public Gemini getGemini() { return this.gemini; }
    public void  setGemini(Gemini gemini) { this.gemini = gemini; }

    public static class Spotify {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private List<String> scopes = List.of("playlist-modify-public", "playlist-modify-private", "user-read-email", "user-read-private");

        public String getClientId() { return this.clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return this.clientSecret; }
        public void  setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return this.redirectUri; }
        public void  setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public List<String> getScopes() { return this.scopes; }
        public void  setScopes(List<String> scopes) { this.scopes = scopes; }
    }

    public static class Gemini {
        private String apiKey;
        private String model = "gemini-2.0-flash";

        public String getApiKey() { return this.apiKey; }
        public void  setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return this.model; }
        public void  setModel(String model) { this.model = model; }
    }
}
