package com.example.playlistgenerator.domain.dto;

import java.util.List;

/**
 * Structured playlist intent extracted by Gemini from a free-text user request.
 */
public record PlaylistIntent(
    String playlistTitle,
    String playlistDescription,
    List<String> searchQueries,
    String detectedContext
) {}
