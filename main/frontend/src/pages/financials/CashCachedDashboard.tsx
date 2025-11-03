import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '@/context/AuthContext'
import { api } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Separator } from '@/components/ui/separator'
import { toast } from 'sonner'
import { Spinner } from '@/components/ui/spinner'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useStablecoinConversion } from '@/hooks/useStablecoinConversion'

interface LedgerEntry {
  id: number
  customerId: string
  changeAmount: string
  balanceAfter: string
  operation: 'ISSUE' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'REDEEM'
  transactionHash?: string
  reference?: string
  createdAt: string
}

interface Summary {
  contractAddress: string
  treasuryAddress: string
  ledgerTotal: string
  onChainSupply: string
  variance: string
}

interface CustomerOption {
  id: string
  label: string
  username: string
}

function unwrapApiData<T>(payload: any): T | null {
  if (payload === null || payload === undefined) {
    return null
  }
  if (typeof payload === 'object' && 'data' in payload) {
    const inner = (payload as { data?: T }).data
    if (inner !== undefined) {
      return inner as T
    }
  }
  return payload as T
}

function toPayloadAmount(input: string) {
  const trimmed = String(input ?? '').trim()
  if (!/^[0-9]+$/.test(trimmed)) {
    throw new Error('Amount must be a whole CashCached token')
  }
  const parsed = Number(trimmed)
  if (!Number.isSafeInteger(parsed) || parsed <= 0) {
    throw new Error('Amount must be a whole CashCached token')
  }
  return parsed
}

export function CashCachedDashboard() {
  const { user } = useAuth()
  const { preferredCurrency, formatTokens, formatConvertedTokens } = useStablecoinConversion()
  const [isLoading, setIsLoading] = useState(true)
  const [history, setHistory] = useState<LedgerEntry[]>([])
  const [allHistory, setAllHistory] = useState<LedgerEntry[]>([])
  const [allPage, setAllPage] = useState(0)
  const [allSize, setAllSize] = useState(25)
  const [allTotalPages, setAllTotalPages] = useState(0)
  const [isLoadingAll, setIsLoadingAll] = useState(false)
  const [summary, setSummary] = useState<Summary | null>(null)
  const [customers, setCustomers] = useState<CustomerOption[]>([])
  const [customerSearch, setCustomerSearch] = useState('')
  const [isLoadingCustomers, setIsLoadingCustomers] = useState(false)
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerOption | null>(null)
  const [selectedWalletTokens, setSelectedWalletTokens] = useState<number | null>(null)
  const [isWalletRefreshing, setIsWalletRefreshing] = useState(false)
  const [issueAmount, setIssueAmount] = useState('')
  const [issueCustomer, setIssueCustomer] = useState('')
  const [issueReference, setIssueReference] = useState('')
  const [redeemAmount, setRedeemAmount] = useState('')
  const [redeemCustomer, setRedeemCustomer] = useState('')
  const [redeemReference, setRedeemReference] = useState('')
  const [transferAmount, setTransferAmount] = useState('')
  const [transferFrom, setTransferFrom] = useState('')
  const [transferTo, setTransferTo] = useState('')
  const [transferReference, setTransferReference] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const canManage = useMemo(() => {
    return user?.role === 'ADMIN' || user?.role === 'BANKOFFICER'
  }, [user])

  const filteredCustomers = useMemo(() => {
    if (!canManage) {
      return []
    }
    const query = customerSearch.trim().toLowerCase()
    if (!query) {
      return customers
    }
    const matches = customers.filter((option) => {
      return (
        option.id.toLowerCase().includes(query) ||
        option.label.toLowerCase().includes(query) ||
        option.username.toLowerCase().includes(query)
      )
    })
    if (selectedCustomer && matches.every((option) => option.id !== selectedCustomer.id)) {
      return [selectedCustomer, ...matches]
    }
    return matches
  }, [canManage, customerSearch, customers, selectedCustomer])

  const refreshHistory = async (targetCustomerId?: string) => {
    const id = targetCustomerId || (canManage ? selectedCustomer?.id : user?.id)
    if (!id) {
      return []
    }
    try {
      const historyResp = await api.get(`/api/financials/stablecoin/history/${id}`)
      const payload = unwrapApiData<LedgerEntry[]>(historyResp?.data) ?? []
      const entries = Array.isArray(payload) ? payload : []
      setHistory(entries)
      return entries
    } catch {
      toast.error('Unable to refresh history')
      setHistory([])
      return []
    }
  }

  const refreshAllHistory = async (page = allPage, size = allSize) => {
    if (!canManage) {
      return
    }
    setIsLoadingAll(true)
    try {
      const response = await api.get('/api/financials/stablecoin/history/all', {
        params: { page, size },
      })
      const payload = unwrapApiData<any>(response?.data) ?? {}
      const content = Array.isArray(payload?.content) ? payload.content : []
      const resolvedPage = Number(payload?.number ?? payload?.pageable?.pageNumber ?? page)
      const resolvedSize = Number(payload?.size ?? payload?.pageable?.pageSize ?? size)
      const resolvedTotalPages = Number(payload?.totalPages ?? 0)
      setAllHistory(content)
      setAllPage(Number.isFinite(resolvedPage) ? resolvedPage : page)
      setAllSize(Number.isFinite(resolvedSize) ? resolvedSize : size)
      setAllTotalPages(Number.isFinite(resolvedTotalPages) ? resolvedTotalPages : 0)
    } catch {
      toast.error('Unable to load global history')
      setAllHistory([])
    } finally {
      setIsLoadingAll(false)
    }
  }

  const refreshSummary = async () => {
    if (!canManage) {
      return
    }
    try {
      const summaryResp = await api.get('/api/financials/stablecoin/summary')
      const payload = unwrapApiData<Summary>(summaryResp?.data)
      setSummary(payload ?? null)
    } catch {
      toast.error('Unable to refresh summary')
    }
  }

  const refreshWalletBalance = async (targetCustomerId?: string) => {
    const id = targetCustomerId || (canManage ? selectedCustomer?.id : user?.id)
    if (!id) {
      return
    }
    setIsWalletRefreshing(true)
    try {
      const response = await api.get(`/api/financials/stablecoin/balance/${id}`)
      const payload = response?.data?.data ?? response?.data
      const balance = Number(payload?.balance ?? 0)
      setSelectedWalletTokens(Number.isFinite(balance) ? balance : 0)
    } catch {
      toast.error('Unable to load wallet balance')
      setSelectedWalletTokens(null)
    } finally {
      setIsWalletRefreshing(false)
    }
  }

  const handleSelectCustomer = (value: string) => {
    if (!value) {
      setSelectedCustomer(null)
      setIssueCustomer('')
      setRedeemCustomer('')
      setTransferFrom('')
      return
    }
    const match = customers.find((option) => option.id === value)
    const next = match ?? { id: value, label: value, username: value }
    setSelectedCustomer(next)
    setIssueCustomer(next.id)
    setRedeemCustomer(next.id)
    setTransferFrom((current) => (current ? current : next.id))
    refreshHistory(next.id)
    refreshWalletBalance(next.id)
  }

  useEffect(() => {
    const load = async () => {
      setIsLoading(true)
      try {
        if (canManage) {
          await Promise.all([refreshSummary(), refreshHistory(), refreshAllHistory()])
        } else if (user) {
          await refreshHistory(user.id)
        }
        if (!canManage) {
          await refreshWalletBalance(user?.id)
        }
      } catch (error) {
        toast.error('Unable to load CashCached data')
      } finally {
        setIsLoading(false)
      }
    }

    load()
  }, [user, canManage])

  useEffect(() => {
    if (!canManage) {
      return
    }
    let cancelled = false
    const loadCustomers = async () => {
      setIsLoadingCustomers(true)
      try {
        const response = await api.get('/api/customer/all')
        if (cancelled) {
          return
        }
        const payload = Array.isArray(response.data) ? response.data : []
        const mapped = payload
          .map((entry: any) => {
            const username = String(entry.username ?? '').trim()
            const fallbackId = String(entry.customerId ?? entry.id ?? '').trim()
            const resolvedId = fallbackId || username
            if (!resolvedId) {
              return null
            }
            const fullName = String(entry.fullName ?? '').trim()
            const email = String(entry.email ?? '').trim()
            const labelSegments: string[] = []
            if (fullName) {
              labelSegments.push(fullName)
            }
            if (username) {
              labelSegments.push(`@${username}`)
            }
            if (email) {
              labelSegments.push(email)
            }
            if (resolvedId) {
              labelSegments.push(`ID: ${resolvedId}`)
            }
            const labelSource = labelSegments.length > 0 ? labelSegments.join(' • ') : resolvedId
            return {
              id: resolvedId,
              label: labelSource,
              username: username || resolvedId,
            }
          })
          .filter(Boolean) as CustomerOption[]
        setCustomers(mapped)
        if (mapped.length > 0) {
          setSelectedCustomer((current) => current ?? mapped[0])
        }
      } catch {
        if (!cancelled) {
          toast.error('Unable to load customers')
          setCustomers([])
          setSelectedCustomer(null)
        }
      } finally {
        if (!cancelled) {
          setIsLoadingCustomers(false)
        }
      }
    }

    loadCustomers()
    return () => {
      cancelled = true
    }
  }, [canManage])

  useEffect(() => {
    if (!canManage) {
      return
    }
    if (!selectedCustomer && customers.length > 0) {
      setSelectedCustomer(customers[0])
      return
    }
    if (selectedCustomer && customers.every((option) => option.id !== selectedCustomer.id)) {
      setSelectedCustomer(customers[0] ?? null)
    }
  }, [canManage, customers, selectedCustomer])

  useEffect(() => {
    if (!canManage) {
      return
    }
    if (!selectedCustomer?.id) {
      return
    }
    setIssueCustomer(selectedCustomer.id)
    setRedeemCustomer(selectedCustomer.id)
    setTransferFrom((current) => (current ? current : selectedCustomer.id))
    refreshHistory(selectedCustomer.id)
    refreshAllHistory()
    refreshWalletBalance(selectedCustomer.id)
  }, [canManage, selectedCustomer?.id])

  const handleIssue = async () => {
    if (!canManage) return
    setIsSubmitting(true)
    try {
      const payloadAmount = toPayloadAmount(issueAmount)
      await api.post('/api/financials/stablecoin/issue', {
        customerId: issueCustomer.trim(),
        amount: payloadAmount,
        reference: issueReference.trim() || undefined,
      })
      toast.success('CashCached issued')
      const tasks: Promise<any>[] = []
      tasks.push(refreshHistory(issueCustomer))
      tasks.push(refreshSummary())
      tasks.push(refreshWalletBalance(issueCustomer))
      tasks.push(refreshAllHistory())
      await Promise.all(tasks)
      window.dispatchEvent(new CustomEvent('cashcached:refresh-wallet'))
      setIssueAmount('')
      setIssueReference('')
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Issue failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleRedeem = async () => {
    if (!canManage) return
    setIsSubmitting(true)
    try {
      const payloadAmount = toPayloadAmount(redeemAmount)
      await api.post('/api/financials/stablecoin/redeem', {
        customerId: redeemCustomer.trim(),
        amount: payloadAmount,
        reference: redeemReference.trim() || undefined,
      })
      toast.success('CashCached redeemed')
      const tasks: Promise<any>[] = []
      tasks.push(refreshHistory(redeemCustomer))
      tasks.push(refreshSummary())
      tasks.push(refreshWalletBalance(redeemCustomer))
      tasks.push(refreshAllHistory())
      await Promise.all(tasks)
      window.dispatchEvent(new CustomEvent('cashcached:refresh-wallet'))
      setRedeemAmount('')
      setRedeemReference('')
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Redeem failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleTransfer = async () => {
    if (!canManage) return
    setIsSubmitting(true)
    try {
      const payloadAmount = toPayloadAmount(transferAmount)
      await api.post('/api/financials/stablecoin/transfer', {
        fromCustomerId: transferFrom.trim(),
        toCustomerId: transferTo.trim(),
        amount: payloadAmount,
        reference: transferReference.trim() || undefined,
      })
      toast.success('CashCached transfer recorded')
      const tasks: Promise<any>[] = []
      if (selectedCustomer?.id) {
        tasks.push(refreshHistory(selectedCustomer.id))
      }
      tasks.push(refreshSummary())
      tasks.push(refreshAllHistory())
      if (selectedCustomer?.id === transferFrom.trim()) {
        tasks.push(refreshWalletBalance(transferFrom))
      }
      if (selectedCustomer?.id === transferTo.trim()) {
        tasks.push(refreshWalletBalance(transferTo))
      }
      await Promise.all(tasks)
      window.dispatchEvent(new CustomEvent('cashcached:refresh-wallet'))
      setTransferAmount('')
      setTransferFrom('')
      setTransferTo('')
      setTransferReference('')
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Transfer failed')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">CashCached Financials</h1>
          <p className="text-muted-foreground">Monitor and manage the CashCached stablecoin lifecycle.</p>
        </div>
        <div className="flex items-center gap-3">
          {canManage && (
            <Button variant="outline" onClick={() => refreshSummary()} disabled={isSubmitting}>
              Refresh Summary
            </Button>
          )}
          <Button variant="outline" onClick={() => refreshHistory()} disabled={isSubmitting}>
            Refresh History
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        {canManage && (
          <Card>
            <CardHeader>
              <CardTitle>Contract</CardTitle>
              <CardDescription>Current smart contract deployment</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              <div className="text-sm">
                <Label>Address</Label>
                <p className="break-all text-sm font-mono">{summary?.contractAddress ?? 'N/A'}</p>
              </div>
              <div className="text-sm">
                <Label>Treasury</Label>
                <p className="break-all text-sm font-mono">{summary?.treasuryAddress ?? 'N/A'}</p>
              </div>
            </CardContent>
          </Card>
        )}

        {canManage && (
          <Card>
            <CardHeader>
              <CardTitle>Ledger Total</CardTitle>
              <CardDescription>Sum of all issued balances</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-semibold">{formatTokens(Number(summary?.ledgerTotal ?? 0))}</p>
              <p className="text-sm text-muted-foreground">{formatConvertedTokens(Number(summary?.ledgerTotal ?? 0), preferredCurrency)}</p>
            </CardContent>
          </Card>
        )}

        {canManage && (
          <Card>
            <CardHeader>
              <CardTitle>On-chain Supply</CardTitle>
              <CardDescription>Total supply reported by the contract</CardDescription>
            </CardHeader>
            <CardContent className="space-y-1">
              <p className="text-2xl font-semibold">{formatTokens(Number(summary?.onChainSupply ?? 0))}</p>
              <p className="text-sm text-muted-foreground">{formatConvertedTokens(Number(summary?.onChainSupply ?? 0), preferredCurrency)}</p>
              <Badge variant={Number(summary?.variance ?? 0) === 0 ? 'secondary' : 'destructive'}>
                Variance {formatTokens(Number(summary?.variance ?? 0))}
              </Badge>
            </CardContent>
          </Card>
        )}
      </div>

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle>Select Customer</CardTitle>
            <CardDescription>Pick a wallet to manage CashCached balances</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-2 md:grid-cols-[minmax(0,1fr)_minmax(0,320px)]">
              <Input
                value={customerSearch}
                onChange={(event) => setCustomerSearch(event.target.value)}
                placeholder="Search by name, username, or ID"
                disabled={isLoadingCustomers}
              />
              <Select
                value={selectedCustomer?.id ?? ''}
                onValueChange={handleSelectCustomer}
                disabled={isLoadingCustomers || customers.length === 0}
              >
                <SelectTrigger>
                  <SelectValue placeholder={isLoadingCustomers ? 'Loading customers…' : 'Select customer'} />
                </SelectTrigger>
                <SelectContent>
                  {filteredCustomers.length === 0 ? (
                    <SelectItem value="" disabled>
                      {customerSearch ? 'No matches found' : 'No customers available'}
                    </SelectItem>
                  ) : (
                    filteredCustomers.map((option) => (
                      <SelectItem key={option.id} value={option.id}>
                        {option.label}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>
      )}

      {canManage && (
        <Card>
          <CardHeader>
            <CardTitle>Customer Wallet</CardTitle>
            <CardDescription>
              {selectedCustomer ? `Balance for ${selectedCustomer.label}` : 'Select a customer to view wallet details'}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <div className="text-3xl font-semibold">
                {isWalletRefreshing ? '—' : formatTokens(selectedWalletTokens ?? 0)}
              </div>
              <div className="text-sm text-muted-foreground">
                {isWalletRefreshing ? 'Refreshing…' : formatConvertedTokens(selectedWalletTokens ?? 0, preferredCurrency)}
              </div>
            </div>
            <Button
              variant="outline"
              onClick={() => selectedCustomer?.id && refreshWalletBalance(selectedCustomer.id)}
              disabled={isWalletRefreshing || !selectedCustomer?.id}
            >
              Refresh Balance
            </Button>
          </CardContent>
        </Card>
      )}

      {canManage && (
        <Tabs defaultValue="issue" className="space-y-4">
          <TabsList>
            <TabsTrigger value="issue">Issue</TabsTrigger>
            <TabsTrigger value="redeem">Redeem</TabsTrigger>
            <TabsTrigger value="transfer">Transfer</TabsTrigger>
          </TabsList>

          <TabsContent value="issue">
            <Card>
              <CardHeader>
                <CardTitle>Issue CashCached</CardTitle>
                <CardDescription>Mint new tokens to a customer account</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4 md:grid-cols-3">
                <div className="space-y-2">
                  <Label>Customer</Label>
                  <Select
                    value={issueCustomer}
                    onValueChange={(value) => {
                      setIssueCustomer(value)
                      handleSelectCustomer(value)
                    }}
                    disabled={customers.length === 0}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select customer" />
                    </SelectTrigger>
                    <SelectContent>
                      {filteredCustomers.length === 0 ? (
                        <SelectItem value="" disabled>
                          {customerSearch ? 'No matches found' : 'No customers available'}
                        </SelectItem>
                      ) : (
                        filteredCustomers.map((option) => (
                          <SelectItem key={option.id} value={option.id}>
                            {option.label}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Amount</Label>
                  <Input
                    value={issueAmount}
                    onChange={(event) => setIssueAmount(event.target.value)}
                    placeholder="100"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Reference (optional)</Label>
                  <Input
                    value={issueReference}
                    onChange={(event) => setIssueReference(event.target.value)}
                    placeholder="Issuance note"
                  />
                </div>
                <div className="md:col-span-3 flex justify-end">
                  <Button onClick={handleIssue} disabled={isSubmitting}>
                    Issue
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="redeem">
            <Card>
              <CardHeader>
                <CardTitle>Redeem CashCached</CardTitle>
                <CardDescription>Burn tokens from a customer account</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4 md:grid-cols-3">
                <div className="space-y-2">
                  <Label>Customer</Label>
                  <Select
                    value={redeemCustomer}
                    onValueChange={(value) => {
                      setRedeemCustomer(value)
                      handleSelectCustomer(value)
                    }}
                    disabled={customers.length === 0}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select customer" />
                    </SelectTrigger>
                    <SelectContent>
                      {filteredCustomers.length === 0 ? (
                        <SelectItem value="" disabled>
                          {customerSearch ? 'No matches found' : 'No customers available'}
                        </SelectItem>
                      ) : (
                        filteredCustomers.map((option) => (
                          <SelectItem key={option.id} value={option.id}>
                            {option.label}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Amount</Label>
                  <Input
                    value={redeemAmount}
                    onChange={(event) => setRedeemAmount(event.target.value)}
                    placeholder="50"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Reference (optional)</Label>
                  <Input
                    value={redeemReference}
                    onChange={(event) => setRedeemReference(event.target.value)}
                    placeholder="Redemption note"
                  />
                </div>
                <div className="md:col-span-3 flex justify-end">
                  <Button onClick={handleRedeem} disabled={isSubmitting}>
                    Redeem
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="transfer">
            <Card>
              <CardHeader>
                <CardTitle>Transfer CashCached</CardTitle>
                <CardDescription>Move tokens between customers</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4 md:grid-cols-4">
                <div className="space-y-2">
                  <Label>From Customer</Label>
                  <Select
                    value={transferFrom}
                    onValueChange={(value) => setTransferFrom(value)}
                    disabled={customers.length === 0}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select sender" />
                    </SelectTrigger>
                    <SelectContent>
                      {filteredCustomers.length === 0 ? (
                        <SelectItem value="" disabled>
                          {customerSearch ? 'No matches found' : 'No customers available'}
                        </SelectItem>
                      ) : (
                        filteredCustomers.map((option) => (
                          <SelectItem key={option.id} value={option.id}>
                            {option.label}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>To Customer</Label>
                  <Select
                    value={transferTo}
                    onValueChange={(value) => setTransferTo(value)}
                    disabled={customers.length === 0}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select recipient" />
                    </SelectTrigger>
                    <SelectContent>
                      {filteredCustomers.length === 0 ? (
                        <SelectItem value="" disabled>
                          {customerSearch ? 'No matches found' : 'No customers available'}
                        </SelectItem>
                      ) : (
                        filteredCustomers.map((option) => (
                          <SelectItem key={option.id} value={option.id}>
                            {option.label}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Amount</Label>
                  <Input
                    value={transferAmount}
                    onChange={(event) => setTransferAmount(event.target.value)}
                    placeholder="10"
                  />
                </div>
                <div className="space-y-2">
                  <Label>Reference (optional)</Label>
                  <Input
                    value={transferReference}
                    onChange={(event) => setTransferReference(event.target.value)}
                    placeholder="Transfer note"
                  />
                </div>
                <div className="md:col-span-4 flex justify-end">
                  <Button onClick={handleTransfer} disabled={isSubmitting}>
                    Transfer
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Ledger History</CardTitle>
          <CardDescription>Recent CashCached transactions</CardDescription>
        </CardHeader>
        <CardContent>
          <Tabs defaultValue={canManage ? 'customer' : 'customer'} className="space-y-4">
            <TabsList>
              <TabsTrigger value="customer">Customer</TabsTrigger>
              {canManage && <TabsTrigger value="all">All</TabsTrigger>}
            </TabsList>
            <TabsContent value="customer">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>ID</TableHead>
                      <TableHead>Customer</TableHead>
                      <TableHead>Change</TableHead>
                      <TableHead>Balance After</TableHead>
                      <TableHead>Operation</TableHead>
                      <TableHead>Reference</TableHead>
                      <TableHead>Tx Hash</TableHead>
                      <TableHead>Created</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {history.map((entry) => (
                      <TableRow key={entry.id}>
                        <TableCell className="font-mono text-xs">{entry.id}</TableCell>
                        <TableCell>{entry.customerId}</TableCell>
                        <TableCell>
                          <div className="flex flex-col">
                            <span>{formatTokens(Number(entry.changeAmount))}</span>
                            <span className="text-xs text-muted-foreground">{formatConvertedTokens(Number(entry.changeAmount), preferredCurrency)}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-col">
                            <span>{formatTokens(Number(entry.balanceAfter))}</span>
                            <span className="text-xs text-muted-foreground">{formatConvertedTokens(Number(entry.balanceAfter), preferredCurrency)}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              entry.operation === 'REDEEM' || entry.operation === 'TRANSFER_OUT'
                                ? 'destructive'
                                : 'secondary'
                            }
                          >
                            {entry.operation.replace('_', ' ')}
                          </Badge>
                        </TableCell>
                        <TableCell>{entry.reference || '-'}</TableCell>
                        <TableCell>
                          {entry.transactionHash ? (
                            <a
                              href={`https://amoy.polygonscan.com/tx/${entry.transactionHash}`}
                              target="_blank"
                              rel="noreferrer"
                              className="text-primary underline"
                            >
                              {entry.transactionHash.slice(0, 10)}&hellip;
                            </a>
                          ) : (
                            '-'
                          )}
                        </TableCell>
                        <TableCell>{new Date(entry.createdAt).toLocaleString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </TabsContent>
            {canManage && (
              <TabsContent value="all">
                <div className="flex items-center justify-between mb-3">
                  <div className="text-sm text-muted-foreground">
                    Page {allPage + 1} of {Math.max(allTotalPages, 1)}
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={isLoadingAll || allPage <= 0}
                      onClick={() => refreshAllHistory(allPage - 1, allSize)}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={isLoadingAll || allPage >= allTotalPages - 1}
                      onClick={() => refreshAllHistory(allPage + 1, allSize)}
                    >
                      Next
                    </Button>
                  </div>
                </div>
                <div className="overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>ID</TableHead>
                        <TableHead>Customer</TableHead>
                        <TableHead>Change</TableHead>
                        <TableHead>Balance After</TableHead>
                        <TableHead>Operation</TableHead>
                        <TableHead>Reference</TableHead>
                        <TableHead>Tx Hash</TableHead>
                        <TableHead>Created</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {isLoadingAll ? (
                        <TableRow>
                          <TableCell colSpan={8} className="text-center">
                            Loading…
                          </TableCell>
                        </TableRow>
                      ) : allHistory.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={8} className="text-center text-muted-foreground">
                            No entries available
                          </TableCell>
                        </TableRow>
                      ) : (
                        allHistory.map((entry) => (
                          <TableRow key={`all-${entry.id}`}>
                            <TableCell className="font-mono text-xs">{entry.id}</TableCell>
                            <TableCell>{entry.customerId}</TableCell>
                            <TableCell>
                              <div className="flex flex-col">
                                <span>{formatTokens(Number(entry.changeAmount))}</span>
                                <span className="text-xs text-muted-foreground">{formatConvertedTokens(Number(entry.changeAmount), preferredCurrency)}</span>
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="flex flex-col">
                                <span>{formatTokens(Number(entry.balanceAfter))}</span>
                                <span className="text-xs text-muted-foreground">{formatConvertedTokens(Number(entry.balanceAfter), preferredCurrency)}</span>
                              </div>
                            </TableCell>
                            <TableCell>
                              <Badge
                                variant={
                                  entry.operation === 'REDEEM' || entry.operation === 'TRANSFER_OUT'
                                    ? 'destructive'
                                    : 'secondary'
                                }
                              >
                                {entry.operation.replace('_', ' ')}
                              </Badge>
                            </TableCell>
                            <TableCell>{entry.reference || '-'}</TableCell>
                            <TableCell>
                              {entry.transactionHash ? (
                                <a
                                  href={`https://amoy.polygonscan.com/tx/${entry.transactionHash}`}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="text-primary underline"
                                >
                                  {entry.transactionHash.slice(0, 10)}&hellip;
                                </a>
                              ) : (
                                '-'
                              )}
                            </TableCell>
                            <TableCell>{new Date(entry.createdAt).toLocaleString()}</TableCell>
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </div>
              </TabsContent>
            )}
          </Tabs>
        </CardContent>
      </Card>

      <Separator />
      <div className="text-sm text-muted-foreground">
        CashCached contract is deployed on Polygon Amoy at <span className="font-mono">{summary?.contractAddress ?? 'N/A'}</span>.
      </div>
    </div>
  )
}
