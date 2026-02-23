package com.example.playlistgenerator.domain.dto;

import java.util.List;

public record TrackInfo(
    String id,
    String name,
    List<String> artists,
    String albumName,
    int durationMs,
    String previewUrl
) {}
