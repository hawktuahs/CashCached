import { useState, useEffect, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { 
  CreditCard, 
  ArrowLeft,
  Calendar,
  DollarSign,
  Download,
  Share2,
  Activity,
  FileText
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { api } from '@/lib/api'
import { toast } from 'sonner'
import { useI18n } from '@/context/I18nContext'

interface Account {
  id: string
  accountNumber: string
  accountType: string
  balance: number
  interestRate: number
  maturityDate: string
  status: 'ACTIVE' | 'MATURED' | 'CLOSED'
  createdAt: string
  productName: string
  principalAmount: number
  customerId: string
  customerName: string
  customerEmail: string
}

interface Transaction {
  id: string
  type: 'DEPOSIT' | 'WITHDRAWAL' | 'INTEREST' | 'MATURITY'
  amount: number
  description: string
  timestamp: string
  balance: number
}

export function AccountDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { t } = useI18n()
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(false)

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'BANKOFFICER'

  const [depositAmount, setDepositAmount] = useState<number>(0)
  const [withdrawAmount, setWithdrawAmount] = useState<number>(0)
  const [isPosting, setIsPosting] = useState(false)

  const postSelfTransaction = async (t: 'DEPOSIT' | 'WITHDRAWAL', amount: number) => {
    if (!id || amount <= 0) return
    setIsPosting(true)
    try {
      await api.post(
        `/api/accounts/${id}/transactions/self`,
        {
          transactionType: t,
          amount,
          description: t === 'DEPOSIT' ? 'Customer deposit' : 'Customer withdrawal',
        },
        {
          headers: {
            'X-User-Id': String(account?.customerId || user?.id || ''),
          },
        }
      )
      const accRes = await api.get(`/api/accounts/${id}`)
      const payload = accRes?.data?.data ?? accRes?.data
      const a = payload || {}
      setAccount({
        id: String(a.id ?? ''),
        accountNumber: String(a.accountNo ?? a.accountNumber ?? id ?? ''),
        accountType: String(a.accountType ?? 'FIXED_DEPOSIT'),
        balance: Number(a.currentBalance ?? a.balance ?? a.maturityAmount ?? 0),
        interestRate: Number(a.interestRate ?? 0),
        maturityDate: String(a.maturityDate ?? new Date().toISOString()),
        status: String(a.status ?? 'ACTIVE') as any,
        createdAt: String(a.createdAt ?? new Date().toISOString()),
        productName: String(a.productName ?? a.productCode ?? 'Product'),
        principalAmount: Number(a.principalAmount ?? 0),
        customerId: String(a.customerId ?? ''),
        customerName: String(a.customerName ?? ''),
        customerEmail: String(a.customerEmail ?? ''),
      })
      toast.success(`${t} recorded`)
      fetchTransactions()
    } catch (e: any) {
      const msg = e?.response?.data?.message || 'Failed to record transaction'
      toast.error(String(msg))
    } finally {
      setIsPosting(false)
    }
  }

  useEffect(() => {
    const fetchAccountDetails = async () => {
      try {
        const response = await api.get(`/api/accounts/${id}`)
        const payload = response?.data?.data ?? response?.data
        const a = payload || {}
        const mapped: Account = {
          id: String(a.id ?? ''),
          accountNumber: String(a.accountNo ?? a.accountNumber ?? id ?? ''),
          accountType: String(a.accountType ?? 'FIXED_DEPOSIT'),
          balance: Number(a.currentBalance ?? a.balance ?? a.maturityAmount ?? 0),
          interestRate: Number(a.interestRate ?? 0),
          maturityDate: String(a.maturityDate ?? new Date().toISOString()),
          status: String(a.status ?? 'ACTIVE') as any,
          createdAt: String(a.createdAt ?? new Date().toISOString()),
          productName: String(a.productName ?? a.productCode ?? 'Product'),
          principalAmount: Number(a.principalAmount ?? 0),
          customerId: String(a.customerId ?? ''),
          customerName: String(a.customerName ?? ''),
          customerEmail: String(a.customerEmail ?? ''),
        }
        setAccount(mapped)
      } catch (error) {
        console.error('Failed to fetch account details:', error)
        toast.error('Failed to load account details')
        navigate('/accounts')
      } finally {
        setIsLoading(false)
      }
    }

    if (id) {
      fetchAccountDetails()
    }
  }, [id, navigate])

  const fetchTransactions = async () => {
    setIsLoadingTransactions(true)
    try {
      const response = await api.get(`/api/accounts/${id}/transactions`)
      const list = response?.data?.data ?? response?.data ?? []
      const mapped = (Array.isArray(list) ? list : []).map((t: any) => ({
        id: String(t.id ?? ''),
        type: ((): Transaction['type'] => {
          const v = String(t.type ?? '').toUpperCase()
          if (v === 'WITHDRAWAL') return 'WITHDRAWAL'
          if (v === 'INTEREST') return 'INTEREST'
          if (v === 'MATURITY') return 'MATURITY'
          return 'DEPOSIT'
        })(),
        amount: Number(t.amount ?? 0),
        description: String(t.description ?? ''),
        timestamp: String(t.timestamp ?? new Date().toISOString()),
        balance: Number(t.balanceAfter ?? t.balance ?? 0),
      }))
      setTransactions(mapped)
    } catch (error) {
      console.error('Failed to fetch transactions:', error)
      toast.error('Failed to load transactions')
    } finally {
      setIsLoadingTransactions(false)
    }
  }

  useEffect(() => {
    if (id) fetchTransactions()
  }, [id])

  const interestEarned = useMemo(() => {
    if (!transactions || transactions.length === 0) return 0
    return transactions
      .filter((x) => x.type === 'INTEREST')
      .reduce((sum, x) => sum + (x.amount || 0), 0)
  }, [transactions])

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'default'
      case 'MATURED':
        return 'secondary'
      case 'CLOSED':
        return 'destructive'
      default:
        return 'outline'
    }
  }

  const getTransactionTypeColor = (type: string) => {
    switch (type) {
      case 'DEPOSIT':
      case 'INTEREST':
        return 'text-green-600'
      case 'WITHDRAWAL':
        return 'text-red-600'
      case 'MATURITY':
        return 'text-blue-600'
      default:
        return 'text-gray-600'
    }
  }

  const getTransactionIcon = (type: string) => {
    switch (type) {
      case 'DEPOSIT':
        return '↗'
      case 'WITHDRAWAL':
        return '↙'
      case 'INTEREST':
        return '💰'
      case 'MATURITY':
        return '🎯'
      default:
        return '📄'
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Skeleton className="h-10 w-32" />
          <Skeleton className="h-8 w-48" />
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          <div className="md:col-span-2">
            <Skeleton className="h-96 w-full" />
          </div>
          <div>
            <Skeleton className="h-96 w-full" />
          </div>
        </div>
      </div>
    )
  }

  if (!account) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate('/accounts')}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Accounts
          </Button>
        </div>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">{t('details.notFound.title')}</h3>
            <p className="text-muted-foreground text-center">{t('details.notFound.desc')}</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/accounts')}
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('details.back')}
        </Button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold tracking-tight">{t('details.title')}</h1>
          <p className="text-muted-foreground">
            {account.accountNumber} - {account.productName}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm">
            <Download className="mr-2 h-4 w-4" />
            {t('action.download')}
          </Button>
          <Button variant="outline" size="sm">
            <Share2 className="mr-2 h-4 w-4" />
            {t('action.share')}
          </Button>
        </div>
      </div>

      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview">{t('details.tabs.overview')}</TabsTrigger>
          <TabsTrigger value="transactions">{t('details.tabs.transactions')}</TabsTrigger>
          <TabsTrigger value="documents">{t('details.tabs.documents')}</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-5 w-5" />
                {t('details.info')}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.accountNumber')}</p>
                  <p className="text-lg font-semibold">{account.accountNumber}</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.accountType')}</p>
                  <p className="text-lg font-semibold">{(account.accountType || '').replace('_', ' ') || 'FIXED DEPOSIT'}</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.productName')}</p>
                  <p className="text-lg font-semibold">{account.productName}</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.status')}</p>
                  <Badge variant={getStatusBadgeVariant(account.status)}>
                    {account.status}
                  </Badge>
                </div>
              </div>
              {user?.role === 'CUSTOMER' && account.status === 'ACTIVE' && (
                <div className="grid grid-cols-2 gap-4 pt-2">
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">{t('details.deposit.label')}</p>
                    <div className="flex gap-2">
                      <input type="number" className="w-full border rounded px-2 py-1" value={depositAmount} onChange={(e) => setDepositAmount(Number(e.target.value))} />
                      <Button size="sm" disabled={isPosting || depositAmount <= 0} onClick={() => postSelfTransaction('DEPOSIT', depositAmount)}>{t('details.deposit.action')}</Button>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">{t('details.withdraw.label')}</p>
                    <div className="flex gap-2">
                      <input type="number" className="w-full border rounded px-2 py-1" value={withdrawAmount} onChange={(e) => setWithdrawAmount(Number(e.target.value))} />
                      <Button size="sm" variant="destructive" disabled={isPosting || withdrawAmount <= 0} onClick={() => postSelfTransaction('WITHDRAWAL', withdrawAmount)}>{t('details.withdraw.action')}</Button>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                {t('details.financial')}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.currentBalance')}</p>
                  <p className="text-2xl font-bold">{formatCurrency(account.balance)}</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.principal')}</p>
                  <p className="text-xl font-semibold">{formatCurrency(account.principalAmount)}</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.interestRate')}</p>
                  <p className="text-xl font-semibold text-green-600">{account.interestRate}% p.a.</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">{t('details.interestEarned')}</p>
                  <p className="text-xl font-semibold text-green-600">
                    {formatCurrency(interestEarned)}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="transactions" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Activity className="h-5 w-5" />
                {t('details.txn.title')}
              </CardTitle>
              <CardDescription>
                {t('details.txn.subtitle')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button onClick={fetchTransactions} disabled={isLoadingTransactions} className="mb-4">
                {isLoadingTransactions ? 'Loading...' : t('details.txn.load')}
              </Button>

              {transactions.length === 0 ? (
                <div className="text-center py-8">
                  <Activity className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">{t('details.txn.empty')}</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {transactions.map((transaction) => (
                    <div key={transaction.id} className="flex items-center justify-between p-4 border rounded-lg">
                      <div className="flex items-center gap-3">
                        <div className="text-2xl">
                          {getTransactionIcon(transaction.type)}
                        </div>
                        <div>
                          <p className="font-medium">{transaction.type}</p>
                          <p className="text-sm text-muted-foreground">
                            {transaction.description}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            {new Date(transaction.timestamp).toLocaleString()}
                          </p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className={`font-semibold ${getTransactionTypeColor(transaction.type)}`}>
                          {transaction.type === 'WITHDRAWAL' ? '-' : '+'}
                          {formatCurrency(transaction.amount)}
                        </p>
                        <p className="text-sm text-muted-foreground">
                          Balance: {formatCurrency(transaction.balance)}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="documents" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileText className="h-5 w-5" />
                Account Documents
              </CardTitle>
              <CardDescription>
                Download account statements and documents
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <FileText className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                <p className="text-muted-foreground">No documents available</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              {t('details.dates')}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">{t('details.opened')}</p>
              <p className="text-sm">{new Date(account.createdAt).toLocaleDateString()}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">{t('details.maturityDate')}</p>
              <p className="text-sm">{new Date(account.maturityDate).toLocaleDateString()}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">{t('details.daysToMaturity')}</p>
              <p className="text-sm">
                {Math.ceil((new Date(account.maturityDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))} days
              </p>
            </div>
          </CardContent>
        </Card>

        {isAdmin && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-5 w-5" />
                {t('details.customerInfo')}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">Customer Name</p>
                <p className="text-sm">{account.customerName}</p>
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">Email</p>
                <p className="text-sm">{account.customerEmail}</p>
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">Customer ID</p>
                <p className="text-sm font-mono">{account.customerId}</p>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
