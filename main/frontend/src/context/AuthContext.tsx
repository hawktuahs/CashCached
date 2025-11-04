import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { jwtDecode } from 'jwt-decode'
import { api } from '@/lib/api'

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
  preferredCurrency?: string
  address?: string
  aadhaarNumber?: string
  panNumber?: string
  dateOfBirth?: string
  classification?: string
}

interface AuthContextType {
  user: User | null
  token: string | null
  login: (username: string, password: string) => Promise<void>
  register: (userData: RegisterData) => Promise<void>
  logout: () => void
  isLoading: boolean
  isAuthenticated: boolean
  verifyOtp: (username: string, code: string) => Promise<void>
  requestMagicLink: (email: string) => Promise<void>
  verifyMagicLink: (token: string) => Promise<void>
  refreshProfile: () => Promise<void>
  preferredCurrency: string
}

interface RegisterData {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
  phoneNumber: string
  address?: string
  dateOfBirth?: string
  aadhaarNumber?: string
  panNumber?: string
  preferredCurrency?: string
  role: 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = !!user && !!token

  const hydrateProfile = async () => {
    try {
      const response = await api.get('/api/customer/profile')
      const payload = response?.data || response
      setUser((current) => {
        if (!current) {
          return null
        }
        const resolvedId = payload?.id ?? payload?.customerId ?? current.id
        const fullName = String(payload?.fullName ?? '').trim()
        const [first, ...rest] = fullName ? fullName.split(' ') : [current.firstName, current.lastName]
        return {
          ...current,
          id: resolvedId ? String(resolvedId) : current.id,
          email: payload?.email || current.email || '',
          firstName: first || current.firstName,
          lastName: rest.length > 0 ? rest.join(' ') : current.lastName,
          preferredCurrency: payload?.preferredCurrency || current.preferredCurrency || 'KWD',
        }
      })
    } catch {}
  }

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('token')
      if (storedToken) {
        try {
          const decoded = jwtDecode<{ sub: string; role: string }>(storedToken)
          setUser({
            id: decoded.sub,
            email: '',
            firstName: '',
            lastName: '',
            role: decoded.role as 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN',
            preferredCurrency: 'KWD',
          })
          setToken(storedToken)
          api.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`
          api.defaults.headers.common['X-User-Id'] = decoded.sub
          await hydrateProfile()
        } catch (error) {
          localStorage.removeItem('token')
          setToken(null)
        }
      }
      setIsLoading(false)
    }

    initializeAuth()
  }, [])

  const login = async (username: string, password: string) => {
    const response = await api.post('/api/auth/login', { username, password })
    if (response?.data?.twoFactorRequired) {
      throw new Error(`OTP_REQUIRED:${username}`)
    }
    const { token: sessionId } = response.data
    localStorage.setItem('token', sessionId)
    setToken(sessionId)
    api.defaults.headers.common['Authorization'] = `Bearer ${sessionId}`
    setUser({
      id: '',
      email: '',
      firstName: '',
      lastName: '',
      role: 'CUSTOMER',
      preferredCurrency: 'KWD',
    })
    await hydrateProfile()
  }

  const verifyOtp = async (username: string, code: string) => {
    const response = await api.post('/api/auth/verify-otp', { username, code })
    const { token: sessionId } = response.data
    localStorage.setItem('token', sessionId)
    setToken(sessionId)
    api.defaults.headers.common['Authorization'] = `Bearer ${sessionId}`
    setUser({
      id: '',
      email: '',
      firstName: '',
      lastName: '',
      role: 'CUSTOMER'
    })
    await hydrateProfile()
  }

  const requestMagicLink = async (email: string) => {
    await api.post('/api/auth/magic-link/request', { email })
  }

  const verifyMagicLink = async (token: string) => {
    const response = await api.post('/api/auth/magic-link/verify', {}, { params: { token } })
    const { token: sessionId } = response.data
    localStorage.setItem('token', sessionId)
    setToken(sessionId)
    api.defaults.headers.common['Authorization'] = `Bearer ${sessionId}`
    setUser({
      id: '',
      email: '',
      firstName: '',
      lastName: '',
      role: 'CUSTOMER',
      preferredCurrency: 'KWD',
    })
    await hydrateProfile()
  }

  const register = async (userData: RegisterData) => {
    try {
      const payload = {
        username: userData.username,
        password: userData.password,
        fullName: `${userData.firstName} ${userData.lastName}`,
        email: userData.email,
        phoneNumber: userData.phoneNumber,
        address: userData.address,
        dateOfBirth: userData.dateOfBirth,
        aadhaarNumber: userData.aadhaarNumber,
        panNumber: userData.panNumber,
        preferredCurrency: userData.preferredCurrency || 'KWD',
        role: userData.role,
      }
      const response = await api.post('/api/auth/register', payload)
      const { token: sessionId } = response.data
      
      localStorage.setItem('token', sessionId)
      setToken(sessionId)
      api.defaults.headers.common['Authorization'] = `Bearer ${sessionId}`
      
      setUser({
        id: '',
        email: userData.email,
        firstName: userData.firstName,
        lastName: userData.lastName,
        role: userData.role,
        preferredCurrency: userData.preferredCurrency || 'KWD',
        address: userData.address,
        aadhaarNumber: userData.aadhaarNumber,
        panNumber: userData.panNumber,
        dateOfBirth: userData.dateOfBirth,
      })
    } catch (error) {
      throw new Error('Registration failed')
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    setToken(null)
    setUser(null)
    delete api.defaults.headers.common['Authorization']
  }

  return (
    <AuthContext.Provider value={{
      user,
      token,
      login,
      register,
      logout,
      isLoading,
      isAuthenticated,
      verifyOtp,
      requestMagicLink,
      verifyMagicLink,
      refreshProfile: hydrateProfile,
      preferredCurrency: user?.preferredCurrency || 'KWD',
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
