package com.example.playlistgenerator.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistGenerationRequest(
    @NotBlank(message = "Request text is required")
    @Size(min = 3, max = 500, message = "Request text must be between 3 and 500 characters")
    String userRequest
) {}
