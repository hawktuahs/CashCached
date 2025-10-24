import { useState, useEffect } from 'react'
import { Link } from 'react-router'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { 
  CreditCard, 
  Plus, 
  Search, 
  Filter,
  TrendingUp,
  Calendar,
  DollarSign,
  Eye,
  MoreHorizontal
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
}

export function AccountsList() {
  const { user } = useAuth()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [typeFilter, setTypeFilter] = useState('all')

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'BANKOFFICER'

  useEffect(() => {
    const fetchAccounts = async () => {
      try {
        if (!user?.id) throw new Error('Missing user id')
        const response = await api.get(`/api/accounts/customer/${user.id}`)
        setAccounts(response.data)
      } catch (error) {
        console.error('Failed to fetch accounts:', error)
        toast.error('Failed to load accounts')
      } finally {
        setIsLoading(false)
      }
    }

    fetchAccounts()
  }, [user?.id])

  const filteredAccounts = accounts
    .filter(account => {
      const matchesSearch = account.accountNumber.includes(searchTerm) ||
                           account.productName.toLowerCase().includes(searchTerm.toLowerCase())
      const matchesStatus = statusFilter === 'all' || account.status === statusFilter
      const matchesType = typeFilter === 'all' || account.accountType === typeFilter
      return matchesSearch && matchesStatus && matchesType
    })

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

  const getAccountTypeColor = (type: string) => {
    switch (type) {
      case 'FIXED_DEPOSIT':
        return 'bg-blue-500'
      case 'RECURRING_DEPOSIT':
        return 'bg-green-500'
      case 'SAVINGS':
        return 'bg-purple-500'
      default:
        return 'bg-gray-500'
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(6)].map((_, i) => (
            <Skeleton key={i} className="h-64 w-full" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {isAdmin ? 'All Accounts' : 'My Accounts'}
          </h1>
          <p className="text-muted-foreground">
            {isAdmin 
              ? 'Manage and monitor all customer accounts'
              : 'View and manage your fixed deposit accounts'
            }
          </p>
        </div>
        {!isAdmin && (
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Open New Account
          </Button>
        )}
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search accounts..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-32">
              <Filter className="mr-2 h-4 w-4" />
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Status</SelectItem>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="MATURED">Matured</SelectItem>
              <SelectItem value="CLOSED">Closed</SelectItem>
            </SelectContent>
          </Select>
          <Select value={typeFilter} onValueChange={setTypeFilter}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Type" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Types</SelectItem>
              <SelectItem value="FIXED_DEPOSIT">Fixed Deposit</SelectItem>
              <SelectItem value="RECURRING_DEPOSIT">Recurring Deposit</SelectItem>
              <SelectItem value="SAVINGS">Savings</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {filteredAccounts.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">No accounts found</h3>
            <p className="text-muted-foreground text-center">
              {searchTerm || statusFilter !== 'all' || typeFilter !== 'all'
                ? 'Try adjusting your search or filter criteria'
                : isAdmin 
                  ? 'No accounts are currently registered'
                  : 'You don\'t have any accounts yet. Open your first account to get started.'
              }
            </p>
            {!isAdmin && (
              <Button className="mt-4">
                <Plus className="mr-2 h-4 w-4" />
                Open New Account
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {filteredAccounts.map((account) => (
            <Card key={account.id} className="relative">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <div className={`rounded-lg p-2 ${getAccountTypeColor(account.accountType)} text-white`}>
                        <CreditCard className="h-4 w-4" />
                      </div>
                      {account.productName}
                    </CardTitle>
                    <CardDescription>
                      Account: {account.accountNumber}
                    </CardDescription>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={getStatusBadgeVariant(account.status)}>
                      {account.status}
                    </Badge>
                    <Button variant="ghost" size="sm">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <DollarSign className="h-4 w-4" />
                      Current Balance
                    </div>
                    <p className="text-xl font-bold">
                      {formatCurrency(account.balance)}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <TrendingUp className="h-4 w-4" />
                      Interest Rate
                    </div>
                    <p className="text-lg font-semibold text-green-600">
                      {account.interestRate}% p.a.
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <DollarSign className="h-4 w-4" />
                    Principal Amount
                  </div>
                  <p className="text-sm">
                    {formatCurrency(account.principalAmount)}
                  </p>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-4 w-4" />
                    Maturity Date
                  </div>
                  <p className="text-sm">
                    {new Date(account.maturityDate).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Calendar className="h-4 w-4" />
                  Opened: {new Date(account.createdAt).toLocaleDateString()}
                </div>

                <div className="pt-2">
                  <Link to={`/accounts/${account.accountNumber}`}>
                    <Button className="w-full" variant="outline">
                      <Eye className="mr-2 h-4 w-4" />
                      View Details
                    </Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
