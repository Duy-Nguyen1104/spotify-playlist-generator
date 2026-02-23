package com.example.playlistgenerator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "playlist_records")
public class PlaylistRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_session_id", nullable = false)
    private UUID userSessionId;

    @Column(name = "spotify_playlist_id")
    private String spotifyPlaylistId;

    @Column(name = "spotify_playlist_url", columnDefinition = "TEXT")
    private String spotifyPlaylistUrl;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "user_request", columnDefinition = "TEXT")
    private String userRequest;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}


