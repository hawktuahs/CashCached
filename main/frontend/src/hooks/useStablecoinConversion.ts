import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '@/context/AuthContext'
import { api } from '@/lib/api'
import { BASE_CURRENCY, formatFiat, formatTokens } from '@/lib/currency'

type RateMap = Record<string, number>

const cache: {
  rates?: RateMap
  baseCurrency?: string
} = {}

const normalizeCurrency = (code?: string) => (code ? code.toUpperCase() : BASE_CURRENCY)

export function useStablecoinConversion() {
  const { user, preferredCurrency } = useAuth()
  const [rates, setRates] = useState<RateMap>(cache.rates || {})
  const [baseCurrency, setBaseCurrency] = useState<string>(cache.baseCurrency || BASE_CURRENCY)

  useEffect(() => {
    if (!user?.id) return
    if (cache.rates) {
      return
    }
    const loadRates = async () => {
      try {
        const response = await api.get(`/api/financials/stablecoin/balance/${user.id}`)
        const payload = response?.data?.data ?? response?.data ?? {}
        const resolvedBase = normalizeCurrency(String(payload?.baseCurrency || BASE_CURRENCY))
        const rawRates: RateMap = payload?.rates || {}
        const upperRates: RateMap = {}
        Object.entries(rawRates).forEach(([key, value]) => {
          upperRates[normalizeCurrency(key)] = value
        })
        upperRates[resolvedBase] = 1
        cache.rates = upperRates
        cache.baseCurrency = resolvedBase
        setRates(upperRates)
        setBaseCurrency(resolvedBase)
      } catch {
        // ignore fetch errors; consumers will fall back to token display
      }
    }
    loadRates()
  }, [user?.id])

  const convertTokens = useCallback((tokens: number, currency = preferredCurrency) => {
    const normalized = normalizeCurrency(currency)
    const base = normalizeCurrency(baseCurrency)
    const rate = rates[normalized] ?? (normalized === base ? 1 : undefined)
    if (rate === undefined) return tokens
    return tokens * rate
  }, [rates, preferredCurrency, baseCurrency])

  const formatTokensLabel = useCallback((tokens: number, maximumFractionDigits = 0) => {
    return formatTokens(tokens, maximumFractionDigits)
  }, [])

  const formatCurrency = useCallback((amount: number, currency = preferredCurrency) => {
    return formatFiat(amount, normalizeCurrency(currency))
  }, [preferredCurrency])

  const formatConvertedTokens = useCallback((tokens: number, currency = preferredCurrency) => {
    const normalized = normalizeCurrency(currency)
    const converted = convertTokens(tokens, normalized)
    return formatFiat(converted, normalized)
  }, [convertTokens, preferredCurrency])

  return {
    preferredCurrency,
    baseCurrency,
    rates,
    convertTokens,
    formatTokens: formatTokensLabel,
    formatCurrency,
    formatConvertedTokens,
  }
}
