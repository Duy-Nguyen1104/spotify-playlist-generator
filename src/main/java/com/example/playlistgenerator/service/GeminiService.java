package com.example.playlistgenerator.service;

import com.example.playlistgenerator.config.AppProperties;
import com.example.playlistgenerator.domain.dto.PlaylistIdentity;
import com.example.playlistgenerator.domain.dto.PlaylistIntent;
import com.example.playlistgenerator.domain.dto.TrackInfo;
import com.example.playlistgenerator.exception.GeminiApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService(AppProperties appProperties, RestClient restClient, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Single-call extraction: takes raw user text
     * plus optional constraints and returns a fully-structured PlaylistIntent with a suggested
     * title, description, and ready-to-use Spotify search queries.
     */
    public PlaylistIntent extractPlaylistIntent(String userRequest) {
        int trackCount = 30;

        String prompt = """
            You are a music curator AI. A user wants a playlist described as:
            "%s"

            Your task: produce 10 diverse Spotify search queries that will surface the best matching tracks.
            Search for the artists implied by the user's request if possible to generate more accurate queries.

            Spotify search field filter syntax you MUST use where applicable:
              - genre:<genre>   
              - year:<Y1>-<Y2> (e.g. year:2015-2024)
              - artist:<name> 

            VALID Spotify genre values for common requests (use these exact strings):
              Japanese:    j-pop, j-rock, j-dance, anime
              Korean:      k-pop, k-r&b (use carefully — prefer artist: filters for Korean R&B)
              Vietnamese:  v-pop  (use for Vietnamese pop; combine with artist: filters for known V-pop artists)
              Western:     pop, rock, indie, r-n-b, hip-hop, electronic, indie-rock, alternative, ambient, lo-fi
              Latin/Other: latin, reggaeton, afrobeats

            Rules for queries:
            - Queries must be compact strings suitable as the Spotify `q` URL parameter.
            - EXTRACT GENRES/ARTISTS FROM THE REQUEST and use them in field filters where possible.
              Example: "Korean R&B similar to DEAN" → use `genre:r-n-b artist:DEAN`
              Example: "Japanese alternative rock" → use `genre:j-rock`, `genre:j-pop`, `genre:alternative` 
            - Vary the queries: at least 2 artist: filters using artists inferred from or named in the request,
              at least 2 genre: field-filter queries, and at least 2 plain keyword queries.
            - If the request implies a specific language/region, ALL queries must target that region.
              Do NOT mix in Western or other-region artists.
            - Target playlist size: %d tracks total.

            Return ONLY valid JSON (no markdown, no explanation):
            {
              "playlistTitle": "Short creative title (max 50 chars)",
              "playlistDescription": "Brief engaging description of the vibe (max 150 chars)",
              "searchQueries": [
                "query 1",
                "query 2",
                "query 3",
                "query 4",
                "query 5",
                "query 6",
                "query 7",
                "query 8",
                "query 9",
                "query 10"
              ],
              "detectedContext": "one word: The main theme, genre, or vibe you detected in the user's request "
            }
            """.formatted(userRequest, trackCount);

        String responseText = callGemini(prompt);
        try {
            JsonNode json = objectMapper.readTree(responseText);
            JsonNode queriesNode = json.get("searchQueries");
            List<String> queries = new ArrayList<>();
            if (queriesNode != null && queriesNode.isArray()) {
                queriesNode.forEach(q -> queries.add(q.asString()));
            }
            if (queries.isEmpty()) {
                // Fallback: use the user request verbatim
                queries.add(userRequest);
            }
            return new PlaylistIntent(
                json.has("playlistTitle") ? json.get("playlistTitle").asString("My Playlist") : "My Playlist",
                json.has("playlistDescription") ? json.get("playlistDescription").asString("A great playlist") : "A great playlist",
                queries,
                json.has("detectedContext") ? json.get("detectedContext").asString("general") : "general"
            );
        } catch (Exception e) {
            log.error("Failed to parse PlaylistIntent from Gemini: {}", responseText, e);
            // Graceful fallback: use request text as a single query
            return new PlaylistIntent("My Playlist", "Based on: " + userRequest,
                List.of(userRequest), "general");
        }
    }

    /**
     * Uses Gemini's LLM knowledge to filter out tracks that don't genuinely match the user's
     * intent. For example, if the user asked for J-pop, this removes K-pop or Western tracks
     * that Spotify may have returned due to loose genre matching.
     * Returns the same list unchanged if the Gemini call fails.
     */
    public List<TrackInfo> filterTracksForIntent(String userRequest, List<TrackInfo> tracks) {
        if (tracks.isEmpty()) return tracks;

        StringBuilder trackList = new StringBuilder();
        for (int i = 0; i < tracks.size(); i++) {
            TrackInfo t = tracks.get(i);
            trackList.append(i).append(". \"").append(t.name()).append("\" by ")
                .append(String.join(", ", t.artists())).append("\n");
        }

        // Always apply strict region/language filtering
        String uncertaintyRule = """
              Apply STRICT country/language filtering:
              - ONLY keep artists you are confident belong to the genre/region/language the user requested.
              - If you are uncertain whether an artist fits the requested region or language, REMOVE the track.
              - Do NOT keep Western artists, multi-region artists, or artists from other countries unless the \
user explicitly asked for them.""";

        String prompt = """
            A user wants a playlist: "%s"

            Below are candidate tracks from a Spotify search. Using YOUR knowledge of each artist,
            keep tracks that fit the user's genre/language/style intent.

            Rules:
            - Artists from the requested region often release songs with English titles — this is fine,
              keep them as long as the ARTIST is from the correct region.
            - %s
            - Examples of what to remove:
                User wants Vietnamese pop → ONLY keep Vietnamese artists (e.g. Sơn Tùng M-TP, HIEUTHUHAI, tlinh, Wren Evans, Hoàng Thùy Linh). Remove all non-Vietnamese artists.
                User wants Korean R&B → remove Western artists (Ed Sheeran, Taylor Swift) and J-pop artists
                User wants J-pop → remove K-pop or Western artists
                User wants US/UK only → remove non-English-language artists
            - If a track's artist is clearly from a different country than requested, REMOVE it.

            Tracks (0-based index):
            %s
            Return ONLY valid JSON — a single "keep" array of integer indices to KEEP:
            { "keep": [0, 1, 2, 5, 8] }
            """.formatted(userRequest, uncertaintyRule, trackList.toString());

        try {
            String responseText = callGemini(prompt, 512);
            JsonNode json = objectMapper.readTree(responseText);
            JsonNode keepNode = json.get("keep");
            if (keepNode == null || !keepNode.isArray()) {
                log.warn("filterTracksForIntent: no 'keep' array in response, keeping all tracks");
                return tracks;
            }
            List<TrackInfo> filtered = new ArrayList<>();
            for (JsonNode indexNode : keepNode) {
                int idx = indexNode.asInt(-1);
                if (idx >= 0 && idx < tracks.size()) {
                    filtered.add(tracks.get(idx));
                }
            }
            log.info("filterTracksForIntent: kept {}/{} tracks for intent '{}'",
                filtered.size(), tracks.size(), userRequest);
            // Only fall back if the response was completely garbled (empty keep list)
            return filtered.isEmpty() ? tracks : filtered;
        } catch (Exception e) {
            log.warn("filterTracksForIntent failed, keeping all tracks: {}", e.getMessage());
            return tracks;
        }
    }

    /**
     * Optionally refines the Gemini-suggested title/description based on the actual
     * tracks that were found on Spotify, producing a more accurate playlist identity.
     */
    public PlaylistIdentity generatePlaylistIdentity(PlaylistIntent intent, List<TrackInfo> tracks) {
        String trackList = tracks.stream()
            .limit(10)
            .map(t -> "- " + t.name() + " by " + String.join(", ", t.artists()))
            .collect(Collectors.joining("\n"));

        String prompt = """
            You are naming a Spotify playlist.
            The user requested: "%s"
            The playlist context is: %s
            The first few tracks that were found:
            %s

            Based on the actual tracks above, refine the playlist title and description.
            Return ONLY valid JSON (no markdown, no explanation):
            {
              "title": "Creative playlist title (max 50 chars)",
              "description": "Engaging description of the playlist vibe (max 150 chars)"
            }
            """.formatted(intent.playlistTitle(), intent.detectedContext(), trackList);

        String responseText = callGemini(prompt);

        try {
            JsonNode json = objectMapper.readTree(responseText);
            return new PlaylistIdentity(
                json.has("title") ? json.get("title").asString(intent.playlistTitle()) : intent.playlistTitle(),
                json.has("description") ? json.get("description").asString(intent.playlistDescription()) : intent.playlistDescription()
            );
        } catch (Exception e) {
            log.warn("Failed to refine playlist identity, using intent defaults", e);
            return new PlaylistIdentity(intent.playlistTitle(), intent.playlistDescription());
        }
    }

    private String callGemini(String prompt) {
        return callGemini(prompt, 1024);
    }

    private String callGemini(String prompt, int maxOutputTokens) {
        AppProperties.Gemini gemini = appProperties.getGemini();
        String url = GEMINI_API_BASE + "/" + gemini.getModel() + ":generateContent?key=" + gemini.getApiKey();

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", maxOutputTokens);

        try {
            String requestJson = objectMapper.writeValueAsString(requestBody);
            String response = restClient.post()
                .uri(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(requestJson)
                .retrieve()
                .body(String.class);

            JsonNode responseNode = objectMapper.readTree(response);
            String text = responseNode
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asString();

            // Strip markdown code blocks if present
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            return text.trim();

        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            throw new GeminiApiException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }
}
