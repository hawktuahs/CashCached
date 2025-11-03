import { useState, useCallback } from 'react'
import { api } from '@/lib/api'
import { toast } from 'sonner'

interface UseApiOptions {
  showSuccessToast?: boolean
  showErrorToast?: boolean
  successMessage?: string
  errorMessage?: string
}

export function useApi() {
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const execute = useCallback(async <T>(
    apiCall: () => Promise<T>,
    options: UseApiOptions = {}
  ): Promise<T | null> => {
    const {
      showSuccessToast = false,
      showErrorToast = true,
      successMessage = 'Operation completed successfully',
      errorMessage = 'An error occurred'
    } = options

    setIsLoading(true)
    setError(null)

    try {
      const result = await apiCall()
      
      if (showSuccessToast) {
        toast.success(successMessage)
      }
      
      return result
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || errorMessage
      setError(errorMsg)
      
      if (showErrorToast) {
        toast.error(errorMsg)
      }
      
      return null
    } finally {
      setIsLoading(false)
    }
  }, [])

  const get = useCallback(async <T>(
    url: string,
    options?: UseApiOptions
  ): Promise<T | null> => {
    return execute(() => api.get(url).then(res => res.data), options)
  }, [execute])

  const post = useCallback(async <T>(
    url: string,
    data?: any,
    options?: UseApiOptions
  ): Promise<T | null> => {
    return execute(() => api.post(url, data).then(res => res.data), options)
  }, [execute])

  const put = useCallback(async <T>(
    url: string,
    data?: any,
    options?: UseApiOptions
  ): Promise<T | null> => {
    return execute(() => api.put(url, data).then(res => res.data), options)
  }, [execute])

  const del = useCallback(async <T>(
    url: string,
    options?: UseApiOptions
  ): Promise<T | null> => {
    return execute(() => api.delete(url).then(res => res.data), options)
  }, [execute])

  return {
    isLoading,
    error,
    execute,
    get,
    post,
    put,
    delete: del,
  }
}
