package com.example.playlistgenerator.domain.dto;

import java.time.Instant;
import java.util.UUID;

public record PlaylistSummaryDto(
    UUID id,
    String spotifyPlaylistId,
    String spotifyPlaylistUrl,
    String title,
    String description,
    String coverImageUrl,
    String userRequest,
    Instant createdAt
) {}
