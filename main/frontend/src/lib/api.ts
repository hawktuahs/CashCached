import axios from 'axios'
import { jwtDecode } from 'jwt-decode'

export const api = axios.create({
  baseURL: import.meta.env.DEV ? '' : 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.response.use(
  (response) => {
    const data = response?.data
    if (data && typeof data === 'object' && 'success' in data && 'data' in data) {
      response.data = data.data
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    if (!config.headers['Authorization']) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    try {
      const decoded = jwtDecode<{ sub: string }>(token)
      const hasHeader = typeof config.headers['X-User-Id'] !== 'undefined'
      if (decoded?.sub && !hasHeader) {
        config.headers['X-User-Id'] = decoded.sub
      }
    } catch { }
  }
  return config
})
