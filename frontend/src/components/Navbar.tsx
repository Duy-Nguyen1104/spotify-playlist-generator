import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const location = useLocation();

  const navLink = (to: string, label: string) => (
    <Link
      to={to}
      className={`text-decoration-none small fw-semibold px-2 py-1 rounded ${
        location.pathname === to ? "text-white" : "text-secondary"
      }`}
      style={location.pathname === to ? { background: "rgba(255,255,255,0.08)" } : undefined}
    >
      {label}
    </Link>
  );

  return (
    <nav className="navbar navbar-custom navbar-expand-lg px-3" style={{ borderBottom: "1px solid #30363d" }}>
      <Link className="navbar-brand fw-bold text-white text-decoration-none me-4" to="/">
        🎵 Playlist Generator
      </Link>
      {isAuthenticated && (
        <div className="d-flex gap-2 me-auto">
          {navLink("/", "Generate")}
          {navLink("/my-playlists", "My Playlists")}
        </div>
      )}
      <div className="ms-auto d-flex align-items-center gap-3">
        {isAuthenticated && user && (
          <>
            <span className="text-secondary small d-none d-sm-inline">{user.displayName}</span>
            <button className="btn btn-sm btn-outline-secondary" onClick={() => logout()}>
              Logout
            </button>
          </>
        )}
      </div>
    </nav>
  );
}
