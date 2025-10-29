export const TOKEN_SYMBOL = 'CCHD'
export const BASE_CURRENCY = 'KWD'

export const formatTokens = (amount: number, maximumFractionDigits = 0) => {
  const value = Number.isFinite(amount) ? amount : 0
  return `${value.toLocaleString(undefined, { maximumFractionDigits })} ${TOKEN_SYMBOL}`
}

export const formatFiat = (amount: number, currency: string) => {
  const value = Number.isFinite(amount) ? amount : 0
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value)
}
