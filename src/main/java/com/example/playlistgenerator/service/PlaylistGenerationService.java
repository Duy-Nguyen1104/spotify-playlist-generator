package com.example.playlistgenerator.service;

import com.example.playlistgenerator.domain.PlaylistRecord;
import com.example.playlistgenerator.domain.UserSession;
import com.example.playlistgenerator.domain.dto.*;
import com.example.playlistgenerator.exception.UnauthorizedException;
import com.example.playlistgenerator.repository.PlaylistRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlaylistGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistGenerationService.class);

    private final SessionService sessionService;
    private final GeminiService geminiService;
    private final SpotifyIntegrationService spotifyIntegrationService;
    private final PlaylistRecordRepository playlistRecordRepository;

    public PlaylistGenerationService(
        SessionService sessionService,
        GeminiService geminiService,
        SpotifyIntegrationService spotifyIntegrationService,
        PlaylistRecordRepository playlistRecordRepository
    ) {
        this.sessionService = sessionService;
        this.geminiService = geminiService;
        this.spotifyIntegrationService = spotifyIntegrationService;
        this.playlistRecordRepository = playlistRecordRepository;
    }

    @Transactional
    public PlaylistGenerationResponse generatePlaylist(UUID sessionId, PlaylistGenerationRequest request) {
        log.info("Generating playlist for session {}", sessionId);

        // 1. Get and refresh session
        UserSession session = sessionService.getSession(sessionId);
        session = sessionService.refreshTokenIfNeeded(session);
        String accessToken = session.getAccessToken();

        // 2. Extract playlist intent with Gemini (single call: generates title, description, and search queries)
        log.info("Extracting playlist intent from: {}", request.userRequest());
        PlaylistIntent intent = geminiService.extractPlaylistIntent(request.userRequest());
        log.info("Playlist intent: title='{}', queries={}", intent.playlistTitle(), intent.searchQueries());

        // 3. Search Spotify with Gemini-generated queries
        int targetCount = 30;
        log.info("Searching Spotify (target: {} tracks)", targetCount);
        List<TrackInfo> tracks = spotifyIntegrationService.searchTracks(intent.searchQueries(), accessToken);

        // 4. Use Gemini to strictly filter off-genre tracks
        log.info("Filtering {} tracks to match intent '{}'", tracks.size(), request.userRequest());
        tracks = geminiService.filterTracksForIntent(request.userRequest(), tracks);

        // 4b. Supplemental search: loop until target is met (max 3 extra passes to avoid runaway calls)
        int supplementalPass = 0;
        while (tracks.size() < targetCount && supplementalPass < 3) {
            supplementalPass++;
            log.info("Not enough tracks after filter ({}<{}), running supplemental pass {}", tracks.size(), targetCount, supplementalPass);
            PlaylistIntent supplementalIntent = geminiService.extractPlaylistIntent(
                request.userRequest() + " — find more matching artists and tracks, different from already found ones (pass " + supplementalPass + ")"
            );
            List<TrackInfo> supplementalTracks = spotifyIntegrationService.searchTracks(
                supplementalIntent.searchQueries(), accessToken);
            supplementalTracks = geminiService.filterTracksForIntent(request.userRequest(), supplementalTracks);

            // Merge, deduplicating by track ID
            java.util.Set<String> existingIds = tracks.stream()
                .map(TrackInfo::id).collect(java.util.stream.Collectors.toSet());
            int before = tracks.size();
            supplementalTracks.stream()
                .filter(t -> !existingIds.contains(t.id()))
                .forEach(tracks::add);
            log.info("After supplemental pass {}: {} tracks total (+{})", supplementalPass, tracks.size(), tracks.size() - before);
            // If this pass added nothing new, no point continuing
            if (tracks.size() == before) break;
        }

        if (tracks.isEmpty()) {
            throw new RuntimeException("No tracks found matching your criteria. Try broadening your constraints.");
        }

        // 5. Generate playlist identity (refine title/description from actual tracks)
        log.info("Generating playlist identity");
        PlaylistIdentity identity = geminiService.generatePlaylistIdentity(intent, tracks);

        // 6. Create Spotify playlist
        log.info("Creating Spotify playlist");
        String playlistId = spotifyIntegrationService.createPlaylist(
            accessToken, identity.title(), identity.description()
        );

        // 7. Add tracks
        List<String> trackIds = tracks.stream().map(TrackInfo::id).collect(Collectors.toList());
        spotifyIntegrationService.addTracksToPlaylist(accessToken, playlistId, trackIds);

        // 8. Fetch cover image (Spotify generates it shortly after tracks are added)
        String coverImageUrl = spotifyIntegrationService.getPlaylistCoverImage(accessToken, playlistId);

        // 9. Save record
        String spotifyUrl = spotifyIntegrationService.getPlaylistUrl(playlistId);
        PlaylistRecord record = new PlaylistRecord();
        record.setUserSessionId(sessionId);
        record.setSpotifyPlaylistId(playlistId);
        record.setSpotifyPlaylistUrl(spotifyUrl);
        record.setTitle(identity.title());
        record.setDescription(identity.description());
        record.setUserRequest(request.userRequest());
        record.setCoverImageUrl(coverImageUrl);
        playlistRecordRepository.save(record);

        log.info("Playlist created: {} ({})", identity.title(), playlistId);
        return new PlaylistGenerationResponse(playlistId, spotifyUrl, identity.title(), identity.description(), tracks);
    }

    public List<PlaylistSummaryDto> getMyPlaylists(UUID sessionId) {
        return playlistRecordRepository.findByUserSessionIdOrderByCreatedAtDesc(sessionId)
            .stream()
            .map(r -> new PlaylistSummaryDto(
                r.getId(),
                r.getSpotifyPlaylistId(),
                r.getSpotifyPlaylistUrl(),
                r.getTitle(),
                r.getDescription(),
                r.getCoverImageUrl(),
                r.getUserRequest(),
                r.getCreatedAt()
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public void deletePlaylist(UUID sessionId, UUID playlistId) {
        PlaylistRecord record = playlistRecordRepository.findById(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found"));
        if (!record.getUserSessionId().equals(sessionId)) {
            throw new UnauthorizedException("Not authorized to delete this playlist");
        }
        playlistRecordRepository.delete(record);
        log.info("Deleted playlist {} for session {}", playlistId, sessionId);
    }
}
