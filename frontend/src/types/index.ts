export interface UserProfile {
  spotifyUserId: string;
  displayName: string;
}

export interface TrackInfo {
  id: string;
  name: string;
  artists: string[];
  albumName: string;
  durationMs: number;
  previewUrl?: string;
}

export interface PlaylistGenerationRequest {
  userRequest: string;
}

export interface PlaylistGenerationResponse {
  playlistId: string;
  spotifyUrl: string;
  title: string;
  description: string;
  tracks: TrackInfo[];
}

export interface PlaylistSummary {
  id: string;
  spotifyPlaylistId: string;
  spotifyPlaylistUrl: string;
  title: string;
  description: string;
  coverImageUrl?: string;
  userRequest: string;
  createdAt: string;
}
