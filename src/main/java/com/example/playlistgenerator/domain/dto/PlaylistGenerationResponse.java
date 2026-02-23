package com.example.playlistgenerator.domain.dto;

import java.util.List;

public record PlaylistGenerationResponse(
    String playlistId,
    String spotifyUrl,
    String title,
    String description,
    List<TrackInfo> tracks
) {}
