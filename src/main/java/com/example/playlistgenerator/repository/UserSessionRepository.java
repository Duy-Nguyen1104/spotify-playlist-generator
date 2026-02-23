package com.example.playlistgenerator.repository;

import com.example.playlistgenerator.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findBySpotifyUserId(String spotifyUserId);
}
