package com.example.playlistgenerator.repository;

import com.example.playlistgenerator.domain.PlaylistRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaylistRecordRepository extends JpaRepository<PlaylistRecord, UUID> {
    List<PlaylistRecord> findByUserSessionIdOrderByCreatedAtDesc(UUID userSessionId);
}
