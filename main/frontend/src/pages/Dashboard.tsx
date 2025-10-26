import { useEffect, useState } from 'react'
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
  totalBalance: number
  activeProducts: number
  recentTransactions: number
}

export function Dashboard() {
  const { user } = useAuth()
  const { t } = useI18n()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [customerId, setCustomerId] = useState<string | null>(null)
  

  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return t('dashboard.greeting.morning')
    if (hour < 18) return t('dashboard.greeting.afternoon')
    return t('dashboard.greeting.evening')
  }

  const quickActions = [
    {
      title: t('dashboard.quick.calculator.title'),
      description: t('dashboard.quick.calculator.desc'),
      icon: Calculator,
      href: '/fd-calculator',
      color: 'bg-blue-500',
    },
    {
      title: t('dashboard.quick.accounts.title'),
      description: t('dashboard.quick.accounts.desc'),
      icon: CreditCard,
      href: '/accounts',
      color: 'bg-green-500',
    },
    {
      title: t('dashboard.quick.products.title'),
      description: t('dashboard.quick.products.desc'),
      icon: Package,
      href: '/products',
      color: 'bg-purple-500',
    },
    {
      title: t('dashboard.quick.profile.title'),
      description: t('dashboard.quick.profile.desc'),
      icon: Users,
      href: '/profile',
      color: 'bg-orange-500',
    },
  ]

  const loadStats = async (cid: string) => {
    setIsLoading(true)
    try {
      const [accRes, prodRes] = await Promise.all([
        api.get(`/api/accounts/customer/${cid}`),
        api.get('/api/v1/product'),
      ])
      const accounts = Array.isArray(accRes.data?.data) ? accRes.data.data : accRes.data
      const totalAccounts = Array.isArray(accounts) ? accounts.length : 0
      const totalBalance = (Array.isArray(accounts) ? accounts : []).reduce((s: number, a: any) => {
        const b = Number(a.currentBalance ?? a.balance ?? a.maturityAmount ?? 0)
        return s + (isFinite(b) ? b : 0)
      }, 0)
      const activeProducts = Array.isArray(prodRes.data) ? prodRes.data.length : 0
      let recentTransactions = 0
      if (Array.isArray(accounts) && accounts.length) {
        const firstFew = accounts.slice(0, Math.min(3, accounts.length))
        const txLists = await Promise.allSettled(firstFew.map((a: any) => api.get(`/api/accounts/${a.accountNo ?? a.accountNumber}/transactions`)))
        recentTransactions = txLists.reduce((sum, r) => {
          if (r.status === 'fulfilled') {
            const list = Array.isArray(r.value.data?.data) ? r.value.data.data : r.value.data
            return sum + (Array.isArray(list) ? Math.min(5, list.length) : 0)
          }
          return sum
        }, 0)
      }
      setStats({ totalAccounts, totalBalance, activeProducts, recentTransactions })
    } catch {
      setStats({ totalAccounts: 0, totalBalance: 0, activeProducts: 0, recentTransactions: 0 })
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
                ₹{stats?.totalBalance?.toLocaleString() || '0'}
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
                  className="h-auto w-full flex-col items-start p-4 hover:bg-accent"
                >
                  <div className="flex items-center gap-3">
                    <div className={`rounded-lg p-2 ${action.color} text-white`}>
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
