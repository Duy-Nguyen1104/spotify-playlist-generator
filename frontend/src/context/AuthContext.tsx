import React, { createContext, useContext, useEffect, useState } from 'react'
import { authApi } from '../services/api'
import type { UserProfile } from '../types'

interface AuthContextType {
  user: UserProfile | null
  isAuthenticated: boolean
  loading: boolean
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  isAuthenticated: false,
  loading: true,
  logout: async () => {},
  refresh: async () => {}
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = async () => {
    try {
      const data = await authApi.me()
      if (data.authenticated && data.spotifyUserId) {
        setUser({ spotifyUserId: data.spotifyUserId, displayName: data.displayName || data.spotifyUserId })
      } else {
        setUser(null)
      }
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  const logout = async () => {
    await authApi.logout()
    setUser(null)
    window.location.href = '/login'
  }

  useEffect(() => { refresh() }, [])

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, loading, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
