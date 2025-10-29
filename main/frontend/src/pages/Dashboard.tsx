import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { 
  CreditCard, 
  Calculator, 
  Package, 
  TrendingUp, 
  Users, 
  DollarSign,
  Activity
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useI18n } from '@/context/I18nContext'
import { api } from '@/lib/api'

interface DashboardStats {
  totalAccounts: number
  totalTokens: number
  totalConverted: number
  currency: string
  activeProducts: number
  recentTransactions: number
}

interface AccountSummary {
  accountNumber: string
  productName: string
  balance: number
  interestRate: number
  maturityDate: string
  openedOn: string
}

interface ProductSummary {
  code: string
  name: string
  minInterestRate: number
  maxInterestRate: number
  minTermMonths: number
  maxTermMonths: number
  description: string
}

type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
}

export function Dashboard() {
  const { user } = useAuth()
  const { t } = useI18n()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [customerId, setCustomerId] = useState<string | null>(null)
  const [aiInput, setAiInput] = useState('')
  const [aiLoading, setAiLoading] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [accounts, setAccounts] = useState<AccountSummary[]>([])
  const [products, setProducts] = useState<ProductSummary[]>([])
  const GEMINI_API_KEY = import.meta.env.VITE_GEMINI_API_KEY || ''
  const chatContainerRef = useRef<HTMLDivElement | null>(null)

  const formatTokens = (value: number) => `${Number.isFinite(value) ? value.toLocaleString(undefined, { maximumFractionDigits: 0 }) : '0'} CCHD`
  const formatCurrency = (value: number, currency: string) => new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(value) ? value : 0)

  useEffect(() => {
    const el = chatContainerRef.current
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  }, [messages])

  const formatDate = (value: string) => {
    if (!value) return 'Not available'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return date.toLocaleDateString()
  }

  const tryLocalAnswer = (question: string) => {
    const q = question.toLowerCase()
    if (!q) return null

    const currency = stats?.currency || 'KWD'
    const fallbackTokens = accounts.reduce((sum, acc) => sum + acc.balance, 0)
    const totalTokens = typeof stats?.totalTokens === 'number' ? stats.totalTokens : fallbackTokens
    const totalConverted = typeof stats?.totalConverted === 'number' ? stats.totalConverted : fallbackTokens

    if (q.includes('how many') && q.includes('account')) {
      return accounts.length
        ? `You currently hold ${accounts.length} fixed deposit account${accounts.length === 1 ? '' : 's'} with a combined balance of ${formatTokens(totalTokens)} (${formatCurrency(totalConverted, currency)}).`
        : 'I do not see any fixed deposit accounts on your profile yet.'
    }

    if (q.includes('total balance') || q.includes('overall balance')) {
      return `Your total balance across all BankTrust accounts is ${formatCurrency(totalConverted, currency)} (${formatTokens(totalTokens)}).`
    }

    const bestKeywords = ['best', 'maximize', 'maximise', 'highest', 'top', 'gain']
    if (bestKeywords.some((word) => q.includes(word)) && (q.includes('account') || q.includes('deposit') || q.includes('return') || q.includes('interest'))) {
      const rankedAccounts = [...accounts].filter((acc) => Number.isFinite(acc.interestRate)).sort((a, b) => b.interestRate - a.interestRate)
      if (rankedAccounts.length) {
        const top = rankedAccounts[0]
        return `Among your current holdings, the ${top.productName} account (${top.accountNumber}) offers the highest rate at ${top.interestRate}% with a balance of ${formatTokens(top.balance)}.`
      }

      const rankedProducts = [...products]
        .filter((prod) => Number.isFinite(prod.maxInterestRate))
        .sort((a, b) => b.maxInterestRate - a.maxInterestRate)
      if (rankedProducts.length) {
        const topProduct = rankedProducts[0]
        return `${topProduct.name} (${topProduct.code}) currently advertises the strongest upside with up to ${topProduct.maxInterestRate}% interest for tenures between ${topProduct.minTermMonths} and ${topProduct.maxTermMonths} months.`
      }

      return 'I could not find any fixed deposit offers to compare yet. Try syncing your accounts or check back in a moment.'
    }

    if (q.includes('matur')) {
      const withMaturity = accounts
        .map((acc) => ({ ...acc, maturityMs: Date.parse(acc.maturityDate), openedMs: Date.parse(acc.openedOn) }))
        .filter((acc) => Number.isFinite(acc.maturityMs))
        .sort((a, b) => a.maturityMs! - b.maturityMs!)

      if (!withMaturity.length) {
        return 'I could not find maturity dates for your accounts yet. Once they are available, I will track them for you.'
      }

      let target = withMaturity[0]
      if (q.includes('latest') || q.includes('longest')) {
        target = withMaturity[withMaturity.length - 1]
      } else if (q.includes('first')) {
        const byOpened = withMaturity
          .filter((acc) => Number.isFinite(acc.openedMs))
          .sort((a, b) => a.openedMs! - b.openedMs!)
        if (byOpened.length) target = byOpened[0]
      }

      const maturityDate = formatDate(target.maturityDate)
      return `Your ${target.productName} account (${target.accountNumber}) matures on ${maturityDate}.`
    }

    return null
  }

  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return t('dashboard.greeting.morning')
    if (hour < 18) return t('dashboard.greeting.afternoon')
    return t('dashboard.greeting.evening')
  }

  const askGemini = async () => {
    const trimmed = aiInput.trim()
    if (!trimmed) return
    const userMessage: ChatMessage = { role: 'user', content: trimmed }
    const pendingMessages = [...messages, userMessage]
    setMessages(pendingMessages)
    setAiLoading(true)
    setAiInput('')
    try {
      const localAnswer = tryLocalAnswer(trimmed)
      if (localAnswer) {
        setMessages([...pendingMessages, { role: 'assistant', content: localAnswer }])
        return
      }

      const instruction = 'You are the BankTrust dashboard assistant. Provide actionable guidance using the customer data below. Reference specific BankTrust accounts and fixed deposit offerings from the provided context. Always keep the conversation grounded in the BankTrust application and ask for missing details instead of refusing.'
      const totalBalanceValue = stats?.totalConverted ?? 0
      const accountSection = accounts.length
        ? accounts
            .slice(0, 8)
            .map((acc, index) => `Account ${index + 1}: ${acc.productName} • Number ${acc.accountNumber} • Balance ${formatTokens(acc.balance)} • Interest ${acc.interestRate}% • Opened ${formatDate(acc.openedOn)} • Matures ${formatDate(acc.maturityDate)}`)
            .join('\n')
        : 'No fixed deposit accounts registered.'
      const productSection = products.length
        ? products
            .slice(0, 8)
            .map((prod, index) => `Product ${index + 1}: ${prod.name} (${prod.code}) • Rate range ${prod.minInterestRate}% - ${prod.maxInterestRate}% • Tenure ${prod.minTermMonths}-${prod.maxTermMonths} months • ${prod.description}`)
            .join('\n')
        : 'No fixed deposit products available.'
      const baseContext = `${instruction}\n\nCustomer snapshot:\n- Name: ${user?.firstName || 'Customer'}\n- Accounts owned: ${accounts.length}\n- Total balance: ${formatCurrency(totalBalanceValue, stats?.currency || 'KWD')} (${formatTokens(stats?.totalTokens || 0)})\n- Recent transactions counted: ${stats?.recentTransactions || 0}\n- Fixed deposit products available to open: ${products.length}\n\nAccounts detail:\n${accountSection}\n\nProduct catalogue:\n${productSection}`

      const conversationContents = pendingMessages.map((msg) => ({
        role: msg.role === 'assistant' ? 'model' : 'user',
        parts: [{ text: msg.content }],
      }))

      if (!GEMINI_API_KEY) {
        setMessages([...pendingMessages, { role: 'assistant', content: 'Gemini API key is not configured. Please add VITE_GEMINI_API_KEY to your environment.' }])
        return
      }

      const models = [
        'gemini-2.0-flash',
        'gemini-2.5-flash-lite',
        'gemini-2.5-flash',
        'gemini-2.0-flash-lite',
        'gemini-2.0-flash-exp'
      ]
      let answered = ''
      const apiVersions = ['v1beta', 'v1']
      for (const m of models) {
        for (const ver of apiVersions) {
          const res = await fetch(`https://generativelanguage.googleapis.com/${ver}/models/${m}:generateContent?key=${GEMINI_API_KEY}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contents: [{ role: 'user', parts: [{ text: baseContext }] }, ...conversationContents] })
          })
          const data = await res.json()
          if (res.ok && Array.isArray(data?.candidates)) {
            answered = (data.candidates[0]?.content?.parts || []).map((p: any) => String(p.text || '')).join('\n')
            if (answered) break
          } else if (data?.error?.code !== 404) {
            break
          }
        }
        if (answered) break
      }
      const assistantReply = answered || 'I could not generate a response right now. Please try again or specify what you need from your BankTrust accounts.'
      setMessages([...pendingMessages, { role: 'assistant', content: assistantReply }])
    } catch {
      setMessages([...pendingMessages, { role: 'assistant', content: 'I was unable to reach the AI service. Please try again shortly.' }])
    } finally {
      setAiLoading(false)
    }
  }

  const quickActions = [
    {
      title: t('dashboard.quick.calculator.title'),
      description: t('dashboard.quick.calculator.desc'),
      icon: Calculator,
      href: '/fd-calculator',
      color: 'bg-emerald-50 text-emerald-700',
    },
    {
      title: t('dashboard.quick.accounts.title'),
      description: t('dashboard.quick.accounts.desc'),
      icon: CreditCard,
      href: '/accounts',
      color: 'bg-emerald-50 text-emerald-700',
    },
    {
      title: t('dashboard.quick.products.title'),
      description: t('dashboard.quick.products.desc'),
      icon: Package,
      href: '/products',
      color: 'bg-emerald-50 text-emerald-700',
    },
    {
      title: t('dashboard.quick.profile.title'),
      description: t('dashboard.quick.profile.desc'),
      icon: Users,
      href: '/profile',
      color: 'bg-emerald-50 text-emerald-700',
    },
  ]

  const loadStats = async (cid: string) => {
    setIsLoading(true)
    try {
      const [accRes, prodRes] = await Promise.all([
        api.get(`/api/accounts/customer/${cid}`),
        api.get('/api/v1/product'),
      ])
      const accountPayload = Array.isArray(accRes.data?.data) ? accRes.data.data : accRes.data
      const mappedAccounts = (Array.isArray(accountPayload) ? accountPayload : []).map((a: any) => {
        const accountNumber = String(a.accountNo ?? a.accountNumber ?? '')
        const productName = String(a.productName ?? a.productCode ?? 'Fixed Deposit')
        const balance = Number(a.currentBalance ?? a.balance ?? a.maturityAmount ?? 0)
        const interestRate = Number(a.interestRate ?? a.rate ?? 0)
        const maturityDate = String(a.maturityDate ?? a.maturity_date ?? '')
        const openedOn = String(a.createdAt ?? a.openedOn ?? a.opened_at ?? '')
        return { accountNumber, productName, balance: isFinite(balance) ? balance : 0, interestRate, maturityDate, openedOn }
      })
      setAccounts(mappedAccounts)
      const totalAccounts = mappedAccounts.length
      const balanceResp = await api.get(`/api/financials/stablecoin/balance/${cid}`)
      const balancePayload = balanceResp?.data?.data ?? balanceResp?.data
      const totalTokens = Number(balancePayload?.balance ?? 0)
      const totalConverted = Number(balancePayload?.targetValue ?? totalTokens)
      const currency = String(balancePayload?.targetCurrency ?? balancePayload?.baseCurrency ?? 'KWD')

      const productPayload = Array.isArray(prodRes.data?.data) ? prodRes.data.data : prodRes.data
      const mappedProducts = (Array.isArray(productPayload) ? productPayload : []).map((p: any) => ({
        code: String(p.productCode ?? p.code ?? ''),
        name: String(p.productName ?? p.name ?? ''),
        minInterestRate: Number(p.minInterestRate ?? p.minRate ?? 0),
        maxInterestRate: Number(p.maxInterestRate ?? p.maxRate ?? 0),
        minTermMonths: Number(p.minTermMonths ?? p.minTenure ?? 0),
        maxTermMonths: Number(p.maxTermMonths ?? p.maxTenure ?? 0),
        description: String(p.description ?? p.productDescription ?? ''),
      }))
      setProducts(mappedProducts)
      const activeProducts = mappedProducts.length

      let recentTransactions = 0
      const accountsForTransactions = mappedAccounts.filter((a) => a.accountNumber)
      if (accountsForTransactions.length) {
        const firstFew = accountsForTransactions.slice(0, Math.min(3, accountsForTransactions.length))
        const txLists = await Promise.allSettled(firstFew.map((a) => api.get(`/api/accounts/${a.accountNumber}/transactions`)))
        recentTransactions = txLists.reduce((sum, r) => {
          if (r.status === 'fulfilled') {
            const list = Array.isArray(r.value.data?.data) ? r.value.data.data : r.value.data
            return sum + (Array.isArray(list) ? Math.min(5, list.length) : 0)
          }
          return sum
        }, 0)
      }
      setStats({ totalAccounts, totalTokens, totalConverted, currency, activeProducts, recentTransactions })
    } catch {
      setAccounts([])
      setProducts([])
      setStats({ totalAccounts: 0, totalTokens: 0, totalConverted: 0, currency: 'KWD', activeProducts: 0, recentTransactions: 0 })
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    const init = async () => {
      try {
        const res = await api.get('/api/customer/profile')
        const id = res?.data?.id ? String(res.data.id) : null
        if (id) {
          setCustomerId(id)
          loadStats(id)
        }
      } catch {}
    }
    init()
  }, [])

  useEffect(() => {
    if (!customerId) return
    const iv = setInterval(() => loadStats(customerId), 10000)
    const onVis = () => {
      if (!document.hidden) loadStats(customerId)
    }
    document.addEventListener('visibilitychange', onVis)
    return () => {
      clearInterval(iv)
      document.removeEventListener('visibilitychange', onVis)
    }
  }, [customerId])

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{getGreeting()}, {user?.firstName}!</h1>
          <p className="text-muted-foreground">
            {t('dashboard.subtitle')}
          </p>
        </div>
        <Badge variant="outline" className="text-sm">
          {user?.role?.replace('_', ' ')}
        </Badge>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{t('dashboard.card.totalBalance')}</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="h-8 w-24" />
            ) : (
              <div className="text-2xl font-bold">
                <div className="flex flex-col">
                  <span>{formatTokens(stats?.totalTokens || 0)}</span>
                  <span className="text-sm text-muted-foreground">
                    {formatCurrency(stats?.totalConverted || 0, stats?.currency || 'KWD')}
                  </span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{t('dashboard.card.activeAccounts')}</CardTitle>
            <CreditCard className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl font-bold">
                {stats?.totalAccounts || 0}
              </div>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{t('dashboard.card.availableProducts')}</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl font-bold">
                {stats?.activeProducts || 0}
              </div>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{t('dashboard.card.recentTransactions')}</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="text-2xl font-bold">
                {stats?.recentTransactions || 0}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>{t('dashboard.quick.title')}</CardTitle>
            <CardDescription>{t('dashboard.quick.subtitle')}</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            {quickActions.map((action) => (
              <Link key={action.title} to={action.href}>
                <Button
                  variant="outline"
                  className="h-auto w-full flex-col items-start p-4 hover:bg-gray-50"
                >
                  <div className="flex items-center gap-3">
                    <div className={`rounded-lg p-2 ${action.color}`}>
                      <action.icon className="h-4 w-4" />
                    </div>
                    <div className="text-left">
                      <div className="font-medium">{action.title}</div>
                      <div className="text-xs text-muted-foreground">
                        {action.description}
                      </div>
                    </div>
                  </div>
                </Button>
              </Link>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>AI Assistant</CardTitle>
            <CardDescription>Ask questions about your accounts and products</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="h-60 overflow-y-auto rounded-md border bg-muted/30 p-3 space-y-2 text-sm">
              <div ref={chatContainerRef} className="flex h-full flex-col gap-2 overflow-y-auto">
              {messages.length === 0 ? (
                <div className="text-muted-foreground">Start the conversation to receive tailored answers based on your BankTrust data.</div>
              ) : (
                messages.map((msg, index) => (
                  <div
                    key={index}
                    className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-full rounded-lg px-3 py-2 ${
                        msg.role === 'user'
                          ? 'bg-emerald-600 text-white'
                          : 'bg-white text-foreground shadow-sm'
                      }`}
                    >
                      <div className="whitespace-pre-wrap leading-relaxed">{msg.content}</div>
                    </div>
                  </div>
                ))
              )}
              </div>
            </div>
            <div className="space-y-2">
              <textarea
                value={aiInput}
                onChange={(e) => setAiInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    askGemini()
                  }
                }}
                placeholder="Ask anything..."
                className="w-full h-24 rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-600 disabled:opacity-70"
                disabled={aiLoading}
              />
              <div className="flex justify-end">
                <Button onClick={askGemini} disabled={aiLoading}>
                  {aiLoading ? 'Thinking...' : 'Send'}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('dashboard.activity.title')}</CardTitle>
            <CardDescription>{t('dashboard.activity.subtitle')}</CardDescription>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="flex items-center space-x-4">
                    <Skeleton className="h-10 w-10 rounded-full" />
                    <div className="space-y-2">
                      <Skeleton className="h-4 w-[200px]" />
                      <Skeleton className="h-4 w-[100px]" />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="space-y-4">
                <div className="flex items-center space-x-4">
                  <div className="rounded-full bg-green-100 p-2">
                    <TrendingUp className="h-4 w-4 text-green-600" />
                  </div>
                  <div className="flex-1 space-y-1">
                    <p className="text-sm font-medium">{t('dashboard.activity.accountOpened')}</p>
                    <p className="text-xs text-muted-foreground">{t('dashboard.activity.accountOpened.desc')}</p>
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {t('dashboard.activity.today')}
                  </div>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="rounded-full bg-blue-100 p-2">
                    <Calculator className="h-4 w-4 text-blue-600" />
                  </div>
                  <div className="flex-1 space-y-1">
                    <p className="text-sm font-medium">{t('dashboard.activity.calculatorUsed')}</p>
                    <p className="text-xs text-muted-foreground">{t('dashboard.activity.calculatorUsed.desc')}</p>
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {t('dashboard.activity.yesterday')}
                  </div>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="rounded-full bg-purple-100 p-2">
                    <Package className="h-4 w-4 text-purple-600" />
                  </div>
                  <div className="flex-1 space-y-1">
                    <p className="text-sm font-medium">{t('dashboard.activity.productViewed')}</p>
                    <p className="text-xs text-muted-foreground">{t('dashboard.activity.productViewed.desc')}</p>
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {t('dashboard.activity.daysAgo2')}
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
