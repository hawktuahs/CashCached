import { useState, useEffect } from 'react'
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
  MoreHorizontal,
  Activity,
  FileText
} from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { api } from '@/lib/api'
import { toast } from 'sonner'

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
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(false)

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'BANKOFFICER'

  useEffect(() => {
    const fetchAccountDetails = async () => {
      try {
        const response = await api.get(`/api/accounts/${id}`)
        setAccount(response.data)
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
      setTransactions(response.data)
    } catch (error) {
      console.error('Failed to fetch transactions:', error)
      toast.error('Failed to load transactions')
    } finally {
      setIsLoadingTransactions(false)
    }
  }

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
            <h3 className="text-lg font-semibold mb-2">Account not found</h3>
            <p className="text-muted-foreground text-center">
              The account you're looking for doesn't exist or you don't have permission to view it.
            </p>
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
          Back to Accounts
        </Button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold tracking-tight">
            Account Details
          </h1>
          <p className="text-muted-foreground">
            {account.accountNumber} - {account.productName}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm">
            <Download className="mr-2 h-4 w-4" />
            Download
          </Button>
          <Button variant="outline" size="sm">
            <Share2 className="mr-2 h-4 w-4" />
            Share
          </Button>
          <Button variant="outline" size="sm">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <div className="md:col-span-2">
          <Tabs defaultValue="overview" className="w-full">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="transactions">Transactions</TabsTrigger>
              <TabsTrigger value="documents">Documents</TabsTrigger>
            </TabsList>
            
            <TabsContent value="overview" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <CreditCard className="h-5 w-5" />
                    Account Information
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Account Number</p>
                      <p className="text-lg font-semibold">{account.accountNumber}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Account Type</p>
                      <p className="text-lg font-semibold">{account.accountType.replace('_', ' ')}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Product Name</p>
                      <p className="text-lg font-semibold">{account.productName}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Status</p>
                      <Badge variant={getStatusBadgeVariant(account.status)}>
                        {account.status}
                      </Badge>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <DollarSign className="h-5 w-5" />
                    Financial Details
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Current Balance</p>
                      <p className="text-2xl font-bold">{formatCurrency(account.balance)}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Principal Amount</p>
                      <p className="text-xl font-semibold">{formatCurrency(account.principalAmount)}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Interest Rate</p>
                      <p className="text-xl font-semibold text-green-600">{account.interestRate}% p.a.</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">Interest Earned</p>
                      <p className="text-xl font-semibold text-green-600">
                        {formatCurrency(account.balance - account.principalAmount)}
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
                    Transaction History
                  </CardTitle>
                  <CardDescription>
                    View all transactions for this account
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <Button 
                    onClick={fetchTransactions}
                    disabled={isLoadingTransactions}
                    className="mb-4"
                  >
                    {isLoadingTransactions ? 'Loading...' : 'Load Transactions'}
                  </Button>
                  
                  {transactions.length === 0 ? (
                    <div className="text-center py-8">
                      <Activity className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                      <p className="text-muted-foreground">No transactions found</p>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {transactions.map((transaction) => (
                        <div
                          key={transaction.id}
                          className="flex items-center justify-between p-4 border rounded-lg"
                        >
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
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calendar className="h-5 w-5" />
                Key Dates
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">Account Opened</p>
                <p className="text-sm">{new Date(account.createdAt).toLocaleDateString()}</p>
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">Maturity Date</p>
                <p className="text-sm">{new Date(account.maturityDate).toLocaleDateString()}</p>
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">Days to Maturity</p>
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
                  Customer Information
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
    </div>
  )
}
