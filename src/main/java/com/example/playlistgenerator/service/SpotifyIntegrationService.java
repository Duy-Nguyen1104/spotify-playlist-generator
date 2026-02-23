package com.example.playlistgenerator.service;

import com.example.playlistgenerator.domain.dto.TrackInfo;
import com.example.playlistgenerator.exception.SpotifyApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SpotifyIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyIntegrationService.class);
    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SpotifyIntegrationService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    // Spotify hard cap on results per search query (Feb 2026)
    private static final int TRACKS_PER_QUERY = 10;

    public List<TrackInfo> searchTracks(List<String> queries, String accessToken) {
        List<TrackInfo> allTracks = new ArrayList<>();

        for (String query : queries) {
            try {
                StringBuilder url = new StringBuilder(SPOTIFY_API_BASE + "/search");
                url.append("?type=track");
                url.append("&limit=").append(TRACKS_PER_QUERY);
                url.append("&q=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));

                String response = restClient.get()
                    .uri(url.toString())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

                JsonNode responseNode = objectMapper.readTree(response);
                JsonNode items = responseNode.path("tracks").path("items");
                if (items.isArray()) {
                    for (JsonNode track : items) {
                        allTracks.add(mapTrack(track));
                    }
                }
            } catch (Exception e) {
                log.warn("Search query failed, skipping: '{}' — {}", query, e.getMessage());
            }
        }

        if (allTracks.isEmpty()) {
            log.error("All Spotify search queries returned no results");
            throw new SpotifyApiException("No tracks found from Spotify search", null);
        }

        // Deduplicate by track ID
        return allTracks.stream()
            .collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toMap(
                    TrackInfo::id,
                    t -> t,
                    (a, b) -> a,
                    java.util.LinkedHashMap::new
                ),
                m -> new ArrayList<>(m.values())
            ));
    }

    public String createPlaylist(String accessToken, String title, String description) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", title);
            body.put("description", description);
            body.put("public", false);

            String response = restClient.post()
                .uri(SPOTIFY_API_BASE + "/me/playlists")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            JsonNode responseNode = objectMapper.readTree(response);
            return responseNode.get("id").asString();
        } catch (Exception e) {
            log.error("Failed to create Spotify playlist", e);
            throw new SpotifyApiException("Failed to create Spotify playlist", e);
        }
    }

    public void addTracksToPlaylist(String accessToken, String playlistId, List<String> trackIds) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode uris = body.putArray("uris");
            trackIds.forEach(id -> uris.add("spotify:track:" + id));

            // /tracks endpoint removed (Feb 2026); use /items
            restClient.post()
                .uri(SPOTIFY_API_BASE + "/playlists/" + playlistId + "/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

        } catch (Exception e) {
            log.error("Failed to add tracks to Spotify playlist", e);
            throw new SpotifyApiException("Failed to add tracks to playlist", e);
        }
    }

    public String getPlaylistUrl(String playlistId) {
        return "https://open.spotify.com/playlist/" + playlistId;
    }

    /**
     * Returns the URL of the largest available cover image for a playlist,
     * or null if no image is available yet.
     */
    public String getPlaylistCoverImage(String accessToken, String playlistId) {
        try {
            String response = restClient.get()
                .uri(SPOTIFY_API_BASE + "/playlists/" + playlistId + "/images")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(String.class);
            JsonNode images = objectMapper.readTree(response);
            if (images.isArray() && images.size() > 0) {
                // Spotify returns images sorted largest first
                return images.get(0).path("url").asString(null);
            }
        } catch (Exception e) {
            log.warn("Could not fetch playlist cover image for {}: {}", playlistId, e.getMessage());
        }
        return null;
    }

    private TrackInfo mapTrack(JsonNode track) {
        String id = track.get("id").asString();
        String name = track.get("name").asString();
        List<String> artists = StreamSupport.stream(track.get("artists").spliterator(), false)
            .map(a -> a.get("name").asString())
            .collect(Collectors.toList());
        String albumName = track.path("album").path("name").asString("");
        int durationMs = track.get("duration_ms").asInt();
        String previewUrl = track.has("preview_url") && !track.get("preview_url").isNull()
            ? track.get("preview_url").asString() : null;
        return new TrackInfo(id, name, artists, albumName, durationMs, previewUrl);
    }
}
