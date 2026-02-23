import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { playlistApi } from "../services/api";
import type { PlaylistGenerationRequest, PlaylistGenerationResponse, TrackInfo } from "../types";

interface PlaylistPreviewPageProps {
  requestData: PlaylistGenerationRequest | null;
}

function formatDuration(ms: number): string {
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

function formatTotalDuration(tracks: TrackInfo[]): string {
  const totalMs = tracks.reduce((sum, t) => sum + t.durationMs, 0);
  const totalMinutes = Math.floor(totalMs / 60000);
  if (totalMinutes < 60) return `${totalMinutes} min`;
  const hours = Math.floor(totalMinutes / 60);
  const mins = totalMinutes % 60;
  return `${hours} hr ${mins > 0 ? `${mins} min` : ""}`;
}

const GENERATION_STEPS = [
  "Understanding your request…",
  "Crafting search queries with AI…",
  "Searching Spotify for matching tracks…",
  "Building your playlist…",
];

function LoadingView() {
  const [stepIndex, setStepIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setStepIndex((i) => Math.min(i + 1, GENERATION_STEPS.length - 1));
    }, 2200);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="d-flex flex-column align-items-center justify-content-center" style={{ minHeight: "60vh" }}>
      <div className="spinner-border text-success mb-4" style={{ width: "3rem", height: "3rem" }} />
      <h5 className="mb-2">{GENERATION_STEPS[stepIndex]}</h5>
      <div className="d-flex gap-2 mt-3">
        {GENERATION_STEPS.map((_, i) => (
          <div
            key={i}
            style={{
              width: 8,
              height: 8,
              borderRadius: "50%",
              background: i <= stepIndex ? "#1DB954" : "#444",
              transition: "background 0.4s",
            }}
          />
        ))}
      </div>
      <p className="text-secondary text-center mt-3" style={{ maxWidth: 360, fontSize: "0.85rem" }}>
        Gemini is building your Spotify playlist from scratch. This usually takes 10–20 seconds.
      </p>
    </div>
  );
}

function TrackRow({ track, index }: { track: TrackInfo; index: number }) {
  return (
    <div className="track-item p-3 mb-2 d-flex align-items-center gap-3">
      <span className="text-secondary fw-bold" style={{ minWidth: "24px", textAlign: "right" }}>
        {index + 1}
      </span>
      <div className="flex-grow-1 min-width-0">
        <div className="fw-semibold text-truncate">{track.name}</div>
        <div className="text-secondary small text-truncate">
          {track.artists.join(", ")} · {track.albumName}
        </div>
      </div>
      <div className="text-secondary small text-nowrap" style={{ minWidth: "38px", textAlign: "right" }}>
        {formatDuration(track.durationMs)}
      </div>
      {track.previewUrl && (
        <a
          href={track.previewUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="btn btn-sm btn-outline-secondary"
          title="30s preview"
          style={{ padding: "2px 8px" }}
        >
          ▶
        </a>
      )}
    </div>
  );
}

export default function PlaylistPreviewPage({ requestData }: PlaylistPreviewPageProps) {
  const navigate = useNavigate();
  const [playlist, setPlaylist] = useState<PlaylistGenerationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!requestData) {
      navigate("/");
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);

    playlistApi
      .generate(requestData)
      .then((data) => {
        if (!cancelled) {
          setPlaylist(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          const msg = err?.response?.data?.detail || err?.message || "Failed to generate playlist";
          setError(msg);
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [requestData, navigate]);

  if (loading) {
    return <LoadingView />;
  }

  if (error) {
    return (
      <div className="row justify-content-center">
        <div className="col-md-8 col-lg-6">
          <div className="card-mood p-4 mt-3 text-center">
            <div className="mb-3" style={{ fontSize: "3rem" }}>
              😕
            </div>
            <h4 className="mb-2">Something went wrong</h4>
            <p className="text-secondary mb-4">{error}</p>
            <button className="btn-spotify btn me-2" onClick={() => navigate("/")}>
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!playlist) return null;

  return (
    <div className="row justify-content-center">
      <div className="col-md-10 col-lg-8">
        <div className="playlist-header p-4 mb-4 mt-3">
          <div className="d-flex align-items-start justify-content-between flex-wrap gap-3">
            <div className="flex-grow-1">
              <div className="text-success small fw-semibold mb-1">✓ Playlist Created</div>
              <h2 className="h3 fw-bold mb-2">{playlist.title}</h2>
              <p className="text-secondary mb-3">{playlist.description}</p>
              <div className="d-flex flex-wrap gap-3 text-secondary small mb-3">
                <span>{playlist.tracks.length} tracks</span>
                <span>·</span>
                <span>{formatTotalDuration(playlist.tracks)}</span>
                {requestData?.userRequest && (
                  <>
                    <span>·</span>
                    <span className="text-truncate" style={{ maxWidth: 300 }}>
                      <em>
                        "
                        {requestData.userRequest.length > 70
                          ? requestData.userRequest.substring(0, 70) + "…"
                          : requestData.userRequest}
                        "
                      </em>
                    </span>
                  </>
                )}
              </div>
              <div className="d-flex gap-2 flex-wrap">
                <a href={playlist.spotifyUrl} target="_blank" rel="noopener noreferrer" className="btn-spotify btn">
                  <span className="me-1">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
                    </svg>
                  </span>
                  Open in Spotify
                </a>
                <button className="btn btn-outline-secondary" onClick={() => navigate("/")}>
                  Generate Another
                </button>
              </div>
            </div>
          </div>
        </div>

        <h5 className="fw-semibold mb-3">Tracks</h5>
        {playlist.tracks.map((track, i) => (
          <TrackRow key={track.id} track={track} index={i} />
        ))}
      </div>
    </div>
  );
}
