package com.example.playlistgenerator.controller;

import com.example.playlistgenerator.domain.dto.PlaylistGenerationRequest;
import com.example.playlistgenerator.domain.dto.PlaylistGenerationResponse;
import com.example.playlistgenerator.domain.dto.PlaylistSummaryDto;
import com.example.playlistgenerator.exception.UnauthorizedException;
import com.example.playlistgenerator.service.PlaylistGenerationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistGenerationService playlistGenerationService;

    public PlaylistController(PlaylistGenerationService playlistGenerationService) {
        this.playlistGenerationService = playlistGenerationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<PlaylistGenerationResponse> generate(
        @Valid @RequestBody PlaylistGenerationRequest request,
        HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESSION_USER_ID) == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        UUID sessionId = (UUID) session.getAttribute(AuthController.SESSION_USER_ID);
        PlaylistGenerationResponse response = playlistGenerationService.generatePlaylist(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PlaylistSummaryDto>> getMyPlaylists(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESSION_USER_ID) == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        UUID sessionId = (UUID) session.getAttribute(AuthController.SESSION_USER_ID);
        return ResponseEntity.ok(playlistGenerationService.getMyPlaylists(sessionId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlaylist(
        @PathVariable UUID id,
        HttpServletRequest httpRequest
    ) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESSION_USER_ID) == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        UUID sessionId = (UUID) session.getAttribute(AuthController.SESSION_USER_ID);
        playlistGenerationService.deletePlaylist(sessionId, id);
        return ResponseEntity.noContent().build();
    }
}
