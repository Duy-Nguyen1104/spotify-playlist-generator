import { useState } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import RequestInputPage from "./pages/RequestInputPage";
import PlaylistPreviewPage from "./pages/PlaylistPreviewPage";
import MyPlaylistsPage from "./pages/MyPlaylistsPage";
import Navbar from "./components/Navbar";
import type { PlaylistGenerationRequest } from "./types";

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return (
      <div className="text-center mt-5">
        <div className="spinner-border text-success" />
      </div>
    );
  }
  // Forward any ?error= params to /login so LoginPage can display them
  if (!isAuthenticated) {
    const to = location.search ? `/login${location.search}` : "/login";
    return <Navigate to={to} replace />;
  }
  return <>{children}</>;
}

function AppRoutes() {
  const [requestData, setRequestData] = useState<PlaylistGenerationRequest | null>(null);

  return (
    <>
      <Navbar />
      <div className="container mt-4">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <RequestInputPage onRequestData={setRequestData} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/playlist"
            element={
              <ProtectedRoute>
                <PlaylistPreviewPage requestData={requestData} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/my-playlists"
            element={
              <ProtectedRoute>
                <MyPlaylistsPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </div>
    </>
  );
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
