import axios from 'axios'
import type { PlaylistGenerationRequest, PlaylistGenerationResponse, PlaylistSummary, UserProfile } from '../types'

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

export const authApi = {
  me: (): Promise<{ authenticated: boolean; spotifyUserId?: string; displayName?: string }> =>
    api.get('/auth/me').then(r => r.data),

  login: () => {
    window.location.href = '/api/auth/spotify/login'
  },

  logout: (): Promise<void> =>
    api.post('/auth/logout').then(() => undefined)
}

export const playlistApi = {
  generate: (request: PlaylistGenerationRequest): Promise<PlaylistGenerationResponse> =>
    api.post('/playlists/generate', request).then(r => r.data),

  getMyPlaylists: (): Promise<PlaylistSummary[]> =>
    api.get('/playlists').then(r => r.data),

  deletePlaylist: (id: string): Promise<void> =>
    api.delete(`/playlists/${id}`).then(() => undefined)
}

export type { UserProfile }
export default api
