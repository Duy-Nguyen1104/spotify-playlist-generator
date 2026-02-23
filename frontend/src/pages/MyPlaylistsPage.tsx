import { useEffect, useState } from "react";
import { playlistApi } from "../services/api";
import type { PlaylistSummary } from "../types";

function timeAgo(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(isoString).toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

const lineClamp2: React.CSSProperties = {
  display: "-webkit-box",
  WebkitBoxOrient: "vertical",
  WebkitLineClamp: 2,
  overflow: "hidden",
};

function PlaylistCard({ playlist, onDeleted }: { playlist: PlaylistSummary; onDeleted: (id: string) => void }) {
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      await playlistApi.deletePlaylist(playlist.id);
      onDeleted(playlist.id);
    } catch {
      setDeleting(false);
      setConfirming(false);
    }
  };

  return (
    <div className="card-mood p-0 overflow-hidden" style={{ borderLeft: "3px solid #1DB954" }}>
      <div className="p-4">
        {/* Header row */}
        <div className="d-flex align-items-start gap-3">
          {/* Cover image */}
          {playlist.coverImageUrl ? (
            <img
              src={playlist.coverImageUrl}
              alt="cover"
              style={{
                width: 56,
                height: 56,
                borderRadius: 6,
                objectFit: "cover",
                flexShrink: 0,
              }}
            />
          ) : (
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: 6,
                flexShrink: 0,
                background: "#1a1a1a",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "1.4rem",
              }}
            >
              🎵
            </div>
          )}
          <div className="flex-grow-1 min-width-0">
            <div className="d-flex align-items-baseline gap-2 flex-wrap mb-1">
              <span className="fw-bold" style={{ fontSize: "1rem" }}>
                {playlist.title}
              </span>
              <span className="text-secondary" style={{ fontSize: "0.78rem" }}>
                {timeAgo(playlist.createdAt)}
              </span>
            </div>
            {playlist.description && (
              <p className="text-secondary mb-2" style={{ fontSize: "0.875rem", ...lineClamp2 }}>
                {playlist.description}
              </p>
            )}
            {playlist.userRequest && (
              <div
                className="text-secondary"
                style={{
                  fontSize: "0.78rem",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                  opacity: 0.75,
                }}
              >
                <span style={{ marginRight: 4 }}>✦</span>
                <em>{playlist.userRequest}</em>
              </div>
            )}
          </div>

          {/* Action buttons */}
          <div className="d-flex align-items-center gap-2 flex-shrink-0">
            <a
              href={playlist.spotifyPlaylistUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-spotify btn btn-sm d-flex align-items-center gap-1"
              title="Open in Spotify"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
              </svg>
              Open
            </a>

            {!confirming ? (
              <button
                className="btn btn-sm btn-outline-secondary"
                style={{ padding: "4px 8px", opacity: 0.6 }}
                title="Remove from list"
                onClick={() => setConfirming(true)}
              >
                🗑
              </button>
            ) : (
              <div className="d-flex align-items-center gap-1">
                <span className="text-secondary" style={{ fontSize: "0.78rem" }}>
                  Remove?
                </span>
                <button
                  className="btn btn-sm btn-danger"
                  style={{ padding: "2px 8px", fontSize: "0.78rem" }}
                  disabled={deleting}
                  onClick={handleDelete}
                >
                  {deleting ? "…" : "Yes"}
                </button>
                <button
                  className="btn btn-sm btn-outline-secondary"
                  style={{ padding: "2px 8px", fontSize: "0.78rem" }}
                  onClick={() => setConfirming(false)}
                >
                  No
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function MyPlaylistsPage() {
  const [playlists, setPlaylists] = useState<PlaylistSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    playlistApi
      .getMyPlaylists()
      .then((data) => {
        setPlaylists(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err?.response?.data?.message || err?.message || "Failed to load playlists");
        setLoading(false);
      });
  }, []);

  const handleDeleted = (id: string) => {
    setPlaylists((prev) => prev.filter((p) => p.id !== id));
  };

  if (loading) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: "40vh" }}>
        <div className="spinner-border text-success" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="row justify-content-center">
        <div className="col-md-8 col-lg-6">
          <div className="card-mood p-4 mt-3 text-center">
            <p className="text-secondary">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-md-10 col-lg-8">
        <div className="d-flex align-items-baseline justify-content-between mt-3 mb-4">
          <h2 className="h4 fw-bold mb-0">My Playlists</h2>
          {playlists.length > 0 && (
            <span className="text-secondary small">
              {playlists.length} playlist{playlists.length !== 1 ? "s" : ""}
            </span>
          )}
        </div>

        {playlists.length === 0 ? (
          <div className="card-mood p-5 text-center text-secondary">
            <div style={{ fontSize: "2.5rem", marginBottom: "0.75rem" }}>🎵</div>
            <div className="fw-semibold mb-1">No playlists yet</div>
            <div className="small">Generate one from the home tab!</div>
          </div>
        ) : (
          <div className="d-flex flex-column gap-3">
            {playlists.map((p) => (
              <PlaylistCard key={p.id} playlist={p} onDeleted={handleDeleted} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
