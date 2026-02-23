import { Navigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { authApi } from "../services/api";

const ERROR_MESSAGES: Record<string, string> = {
  spotify_auth_denied: "You denied access to Spotify. Please try again.",
  state_mismatch: "Login failed: security check failed (state mismatch). Please try again.",
  session_missing: "Login failed: session expired during redirect. Please try again.",
  missing_code: "Login failed: no authorization code received from Spotify.",
  auth_failed:
    "Login failed: could not complete authentication with Spotify. Make sure your Spotify account is registered in the app's Developer Dashboard.",
};

export default function LoginPage() {
  const { isAuthenticated, loading } = useAuth();
  const [searchParams] = useSearchParams();
  const errorKey = searchParams.get("error");
  const errorMessage = errorKey ? (ERROR_MESSAGES[errorKey] ?? `Login error: ${errorKey}`) : null;

  if (loading) {
    return (
      <div className="text-center mt-5">
        <div className="spinner-border text-success" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="d-flex flex-column align-items-center justify-content-center" style={{ minHeight: "80vh" }}>
      <div className="card-mood p-5 text-center" style={{ maxWidth: 480, width: "100%" }}>
        <div className="mb-4" style={{ fontSize: "4rem" }}>
          🎵
        </div>
        <h1 className="h2 fw-bold mb-2"> Playlist Generator</h1>
        <p className="text-secondary mb-4">
          Tell us how you're feeling and we'll create the perfect Spotify playlist using AI.
        </p>
        {errorMessage && (
          <div className="alert alert-danger py-2 text-start small mb-3" role="alert">
            {errorMessage}
          </div>
        )}
        <hr className="border-secondary mb-4" />
        <p className="mb-4 text-secondary small">
          Sign in with your Spotify account to get started. We'll create playlists directly in your library.
        </p>
        <button className="btn-spotify btn" onClick={() => authApi.login()}>
          <span className="me-2">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 0C5.4 0 0 5.4 0 12s5.4 12 12 12 12-5.4 12-12S18.66 0 12 0zm5.521 17.34c-.24.359-.66.48-1.021.24-2.82-1.74-6.36-2.101-10.561-1.141-.418.122-.779-.179-.899-.539-.12-.421.18-.78.54-.9 4.56-1.021 8.52-.6 11.64 1.32.42.18.479.659.301 1.02zm1.44-3.3c-.301.42-.841.6-1.262.3-3.239-1.98-8.159-2.58-11.939-1.38-.479.12-1.02-.12-1.14-.6-.12-.48.12-1.021.6-1.141C9.6 9.9 15 10.561 18.72 12.84c.361.181.54.78.241 1.2zm.12-3.36C15.24 8.4 8.82 8.16 5.16 9.301c-.6.179-1.2-.181-1.38-.721-.18-.601.18-1.2.72-1.381 4.26-1.26 11.28-1.02 15.721 1.621.539.3.719 1.02.419 1.56-.299.421-1.02.599-1.559.3z" />
            </svg>
          </span>
          Login with Spotify
        </button>
        <p className="mt-4 text-secondary" style={{ fontSize: "0.75rem" }}>
          We only request permission to create and modify playlists in your account.
        </p>
      </div>
    </div>
  );
}
