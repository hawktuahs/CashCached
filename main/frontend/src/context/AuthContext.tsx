import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { jwtDecode } from 'jwt-decode'
import { api } from '@/lib/api'

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
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
}

interface RegisterData {
  username: string
  email: string
  password: string
  firstName: string
  lastName: string
  phoneNumber: string
  role: 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = !!user && !!token

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
            role: decoded.role as 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
          })
          setToken(storedToken)
          api.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`
          api.defaults.headers.common['X-User-Id'] = decoded.sub
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
    const { token: newToken } = response.data
    localStorage.setItem('token', newToken)
    setToken(newToken)
    api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
    const decoded = jwtDecode<{ sub: string; role: string }>(newToken)
    setUser({
      id: decoded.sub,
      email: '',
      firstName: '',
      lastName: '',
      role: decoded.role as 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
    })
    api.defaults.headers.common['X-User-Id'] = decoded.sub
  }

  const verifyOtp = async (username: string, code: string) => {
    const response = await api.post('/api/auth/verify-otp', { username, code })
    const { token: newToken } = response.data
    localStorage.setItem('token', newToken)
    setToken(newToken)
    api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
    const decoded = jwtDecode<{ sub: string; role: string }>(newToken)
    setUser({
      id: decoded.sub,
      email: '',
      firstName: '',
      lastName: '',
      role: decoded.role as 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
    })
    api.defaults.headers.common['X-User-Id'] = decoded.sub
  }

  const register = async (userData: RegisterData) => {
    try {
      const payload = {
        username: userData.username,
        password: userData.password,
        fullName: `${userData.firstName} ${userData.lastName}`,
        email: userData.email,
        phoneNumber: userData.phoneNumber,
        role: userData.role,
      }
      const response = await api.post('/api/auth/register', payload)
      const { token: newToken } = response.data
      
      localStorage.setItem('token', newToken)
      setToken(newToken)
      api.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
      
      const decoded = jwtDecode<{ sub: string; role: string }>(newToken)
      setUser({
        id: decoded.sub,
        email: userData.email,
        firstName: userData.firstName,
        lastName: userData.lastName,
        role: decoded.role as 'CUSTOMER' | 'BANKOFFICER' | 'ADMIN'
      })
      api.defaults.headers.common['X-User-Id'] = decoded.sub
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
      verifyOtp
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
