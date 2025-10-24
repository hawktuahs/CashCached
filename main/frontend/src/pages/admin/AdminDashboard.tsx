import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { toast } from 'sonner'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { Download } from 'lucide-react'

import { useAuth } from '@/context/AuthContext'
import { api } from '@/lib/api'

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

const openAccountSchema = z.object({
  customerId: z.string().min(1),
  productCode: z.string().min(1),
  principalAmount: z.number().min(1000),
  interestRate: z.number().min(0.01).max(20),
  tenureMonths: z.number().min(1).max(120),
  branchCode: z.string().regex(/^[A-Z0-9]{3,20}$/),
  remarks: z.string().max(500).optional().or(z.literal('')),
})

type OpenAccountFormData = z.infer<typeof openAccountSchema>

export function AdminDashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const isStaff = user?.role === 'ADMIN' || user?.role === 'BANKOFFICER'

  const [customers, setCustomers] = useState<Array<{ id: string; name: string; username: string }>>([])
  const [products, setProducts] = useState<Array<{ id: number; code: string; name: string }>>([])
  const [selectedCustomer, setSelectedCustomer] = useState<string>('')
  const [customerSearch, setCustomerSearch] = useState('')
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null)
  const [rules, setRules] = useState<Array<any>>([])
  const [isSavingRule, setIsSavingRule] = useState(false)
  const [editingRule, setEditingRule] = useState<any | null>(null)
  const [systemTime, setSystemTime] = useState<string>('')
  const [offsetSeconds, setOffsetSeconds] = useState<number>(0)
  const [advanceBy, setAdvanceBy] = useState<string>('3600')
  const emptyRule = {
    productId: 0,
    ruleName: '',
    ruleDescription: '',
    minThreshold: 0,
    maxThreshold: 0,
    interestRate: 0,
    feeAmount: 0,
    discountPercentage: 0,
    priorityOrder: 0,
    isActive: true,
  }

  const refreshSystemTime = async () => {
    try {
      const t = await api.get('/api/accounts/admin/time')
      const d = t?.data?.data ?? t?.data
      if (d) {
        setSystemTime(String(d.systemTime))
        setOffsetSeconds(Number(d.offsetSeconds ?? 0))
      }
    } catch {
      toast.error('Failed to load system time')
    }
  }

  const setAbsoluteTime = async (iso: string) => {
    try {
      await api.put('/api/accounts/admin/time', { systemTime: iso })
      toast.success('System time updated')
      refreshSystemTime()
    } catch {
      toast.error('Failed to update system time')
    }
  }

  const advanceTime = async (sec: number) => {
    try {
      await api.post('/api/accounts/admin/time/advance', { seconds: sec })
      toast.success('System time advanced')
      refreshSystemTime()
    } catch {
      toast.error('Failed to advance system time')
    }
  }

  const resetTime = async () => {
    try {
      await api.post('/api/accounts/admin/time/reset', {})
      toast.success('System time reset')
      refreshSystemTime()
    } catch {
      toast.error('Failed to reset system time')
    }
  }
  const [ruleForm, setRuleForm] = useState<any>({ ...emptyRule })

  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoadingCustomers, setIsLoadingCustomers] = useState(true)
  const [isLoadingAccounts, setIsLoadingAccounts] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const form = useForm<OpenAccountFormData>({
    resolver: zodResolver(openAccountSchema),
    defaultValues: {
      customerId: '',
      productCode: '',
      principalAmount: 100000,
      interestRate: 7,
      tenureMonths: 12,
      branchCode: 'HQ001',
      remarks: '',
    },
  })

  useEffect(() => {
    if (!isStaff) {
      navigate('/dashboard', { replace: true })
      return
    }
    const load = async () => {
      setIsLoadingCustomers(true)
      try {
        const [custRes, prodRes] = await Promise.all([
          api.get('/api/customer/all'),
          api.get('/api/v1/product'),
        ])
        const c = Array.isArray(custRes.data) ? custRes.data : []
        const p = Array.isArray(prodRes.data) ? prodRes.data : []
        const mappedCustomers = c.map((u: any) => ({ id: String(u.id), name: String(u.fullName || u.username || u.email || u.id), username: String(u.username || '') }))
        const mappedProducts = p.map((x: any) => ({ id: Number(x.id ?? 0), code: String(x.productCode || ''), name: String(x.productName || x.productCode || '') }))
        setCustomers(mappedCustomers)
        setProducts(mappedProducts)
        if (mappedCustomers.length > 0) setSelectedCustomer(mappedCustomers[0].id)
        if (mappedProducts.length > 0) setSelectedProductId(mappedProducts[0].id)
        try {
          const t = await api.get('/api/accounts/admin/time')
          const d = t?.data?.data ?? t?.data
          if (d) {
            setSystemTime(String(d.systemTime))
            setOffsetSeconds(Number(d.offsetSeconds ?? 0))
          }
        } catch {}
      } finally {
        setIsLoadingCustomers(false)
      }
    }
    load()
  }, [isStaff, navigate])

  const resetRuleForm = () => setRuleForm({ ...emptyRule, productId: selectedProductId || 0 })

  const saveRule = async () => {
    if (!selectedProductId) return
    setIsSavingRule(true)
    try {
      const payload = { ...ruleForm, productId: selectedProductId }
      if (editingRule?.id) {
        await api.put(`/api/v1/pricing-rule/${editingRule.id}`, payload)
      } else {
        await api.post('/api/v1/pricing-rule', payload)
      }
      const res = await api.get(`/api/v1/pricing-rule/product/${selectedProductId}`)
      const list = res?.data?.data ?? res?.data ?? []
      setRules(Array.isArray(list) ? list : [])
      setEditingRule(null)
      resetRuleForm()
      toast.success('Rule saved')
    } catch {
      toast.error('Failed to save rule')
    } finally {
      setIsSavingRule(false)
    }
  }

  const editRule = (r: any) => {
    setEditingRule(r)
    setRuleForm({
      productId: r.productId,
      ruleName: r.ruleName,
      ruleDescription: r.ruleDescription,
      minThreshold: Number(r.minThreshold ?? 0),
      maxThreshold: Number(r.maxThreshold ?? 0),
      interestRate: Number(r.interestRate ?? 0),
      feeAmount: Number(r.feeAmount ?? 0),
      discountPercentage: Number(r.discountPercentage ?? 0),
      priorityOrder: Number(r.priorityOrder ?? 0),
      isActive: Boolean(r.isActive ?? true),
    })
  }

  const deleteRule = async (id: number) => {
    if (!selectedProductId) return
    try {
      await api.delete(`/api/v1/pricing-rule/${id}`)
      const res = await api.get(`/api/v1/pricing-rule/product/${selectedProductId}`)
      const list = res?.data?.data ?? res?.data ?? []
      setRules(Array.isArray(list) ? list : [])
      toast.success('Rule deleted')
    } catch {
      toast.error('Failed to delete rule')
    }
  }

  useEffect(() => {
    const fetchAccounts = async () => {
      if (!selectedCustomer) return
      setIsLoadingAccounts(true)
      try {
        const res = await api.get(`/api/accounts/customer/${selectedCustomer}`)
        setAccounts(Array.isArray(res.data) ? res.data : [])
      } finally {
        setIsLoadingAccounts(false)
      }
    }
    fetchAccounts()
  }, [selectedCustomer])

  useEffect(() => {
    const loadRules = async () => {
      if (!selectedProductId) return
      try {
        const res = await api.get(`/api/v1/pricing-rule/product/${selectedProductId}`)
        const list = res?.data?.data ?? res?.data ?? []
        setRules(Array.isArray(list) ? list : [])
      } catch {}
    }
    loadRules()
  }, [selectedProductId])

  const exportCsv = () => {
    const rows = [
      ['Account Number','Product Name','Type','Status','Principal Amount','Balance','Interest Rate','Opened On','Maturity Date'],
      ...accounts.map(a => [
        a.accountNumber,
        a.productName,
        a.accountType,
        a.status,
        String(a.principalAmount),
        String(a.balance),
        String(a.interestRate),
        new Date(a.createdAt).toISOString(),
        new Date(a.maturityDate).toISOString()
      ])
    ]
    const csv = rows.map(r => r.map(v => `"${String(v).replace(/"/g,'""')}"`).join(',')).join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'accounts.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }

  const selectedCustomerName = useMemo(() => customers.find(c => c.id === selectedCustomer)?.name || '', [customers, selectedCustomer])
  const filteredCustomers = useMemo(() => {
    const q = customerSearch.trim().toLowerCase()
    if (!q) return customers
    return customers.filter(c =>
      c.id.toLowerCase().includes(q) ||
      c.name.toLowerCase().includes(q) ||
      c.username.toLowerCase().includes(q)
    )
  }, [customers, customerSearch])

  const submitOpenAccount = async (data: OpenAccountFormData) => {
    setIsSubmitting(true)
    try {
      const payload = {
        customerId: data.customerId,
        productCode: data.productCode,
        principalAmount: data.principalAmount,
        interestRate: data.interestRate,
        tenureMonths: data.tenureMonths,
        branchCode: data.branchCode,
        remarks: data.remarks || '',
      }
      const res = await api.post('/api/accounts/create', payload)
      if (res?.data?.data?.accountNumber) toast.success('Account created successfully')
      const refreshed = await api.get(`/api/accounts/customer/${selectedCustomer}`)
      setAccounts(Array.isArray(refreshed.data) ? refreshed.data : [])
    } catch {
      toast.error('Failed to create account')
    } finally {
      setIsSubmitting(false)
    }
  }

  const closeAccount = async (accountNo: string) => {
    try {
      await api.put(`/api/accounts/${accountNo}/close`, { reason: 'Closed by admin' })
      const refreshed = await api.get(`/api/accounts/customer/${selectedCustomer}`)
      setAccounts(Array.isArray(refreshed.data) ? refreshed.data : [])
      toast.success('Account closed')
    } catch {
      toast.error('Failed to close account')
    }
  }

  if (!isStaff) return null

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Admin Dashboard</h1>
          <p className="text-muted-foreground">Manage customers and their FD accounts</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={exportCsv} disabled={accounts.length === 0}>
            <Download className="mr-2 h-4 w-4" />
            Export CSV
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Customer</CardTitle>
          <CardDescription>Select a customer to manage accounts</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Input placeholder="Search by ID, name, or username" value={customerSearch} onChange={(e) => setCustomerSearch(e.target.value)} />
            {isLoadingCustomers ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <Select value={selectedCustomer} onValueChange={(v) => { setSelectedCustomer(v); form.setValue('customerId', v) }}>
                <SelectTrigger>
                  <SelectValue placeholder="Select customer" />
                </SelectTrigger>
                <SelectContent>
                  {filteredCustomers.map(c => (
                    <SelectItem key={c.id} value={c.id}>{`${c.name} · @${c.username} · ID: ${c.id}`}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          <div className="grid gap-2">
            <div className="text-sm text-muted-foreground">Selected</div>
            <div className="text-base">{selectedCustomerName || 'None'}</div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>System Time</CardTitle>
          <CardDescription>Control global time for accrual simulation</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid md:grid-cols-3 gap-4 items-end">
            <div className="space-y-1">
              <div className="text-sm font-medium">Current System Time</div>
              <div className="text-sm font-mono">{systemTime || '—'}</div>
              <div className="text-xs text-muted-foreground">Offset: {offsetSeconds}s</div>
              <Button variant="outline" size="sm" className="mt-2" onClick={refreshSystemTime}>Refresh</Button>
            </div>
            <div className="space-y-1">
              <div className="text-sm font-medium">Set Absolute Time (ISO)</div>
              <Input placeholder="2025-12-31T00:00:00Z" onKeyDown={(e) => {
                if (e.key === 'Enter') setAbsoluteTime((e.target as HTMLInputElement).value)
              }} />
              <div className="text-xs text-muted-foreground">Press Enter to apply</div>
            </div>
            <div className="space-y-1">
              <div className="text-sm font-medium">Advance By (seconds)</div>
              <div className="flex gap-2">
                <Input value={advanceBy} onChange={(e) => setAdvanceBy(e.target.value)} />
                <Button onClick={() => advanceTime(Number(advanceBy || '0'))}>Advance</Button>
              </div>
              <div className="flex gap-2 mt-2">
                <Button variant="outline" size="sm" onClick={() => setAdvanceBy('86400')}>+1d</Button>
                <Button variant="outline" size="sm" onClick={() => setAdvanceBy('2592000')}>+30d</Button>
                <Button variant="outline" size="sm" onClick={() => setAdvanceBy('7776000')}>+90d</Button>
                <Button variant="destructive" size="sm" onClick={resetTime}>Reset</Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Pricing Rules</CardTitle>
          <CardDescription>Manage pricing rules for products</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Product</label>
              <Select value={selectedProductId ? String(selectedProductId) : ''} onValueChange={(v) => setSelectedProductId(Number(v))}>
                <SelectTrigger>
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent>
                  {products.map(p => (
                    <SelectItem key={p.id} value={String(p.id)}>{p.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="md:col-span-2">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-sm font-medium">Rule Name</label>
                  <Input value={ruleForm.ruleName} onChange={(e) => setRuleForm({ ...ruleForm, ruleName: e.target.value })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Priority</label>
                  <Input type="number" value={ruleForm.priorityOrder} onChange={(e) => setRuleForm({ ...ruleForm, priorityOrder: Number(e.target.value) })} />
                </div>
                <div className="col-span-2">
                  <label className="text-sm font-medium">Description</label>
                  <Input value={ruleForm.ruleDescription} onChange={(e) => setRuleForm({ ...ruleForm, ruleDescription: e.target.value })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Min Threshold</label>
                  <Input type="number" value={ruleForm.minThreshold} onChange={(e) => setRuleForm({ ...ruleForm, minThreshold: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Max Threshold</label>
                  <Input type="number" value={ruleForm.maxThreshold} onChange={(e) => setRuleForm({ ...ruleForm, maxThreshold: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Interest Rate</label>
                  <Input type="number" step="0.01" value={ruleForm.interestRate} onChange={(e) => setRuleForm({ ...ruleForm, interestRate: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Fee Amount</label>
                  <Input type="number" value={ruleForm.feeAmount} onChange={(e) => setRuleForm({ ...ruleForm, feeAmount: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Discount %</label>
                  <Input type="number" step="0.01" value={ruleForm.discountPercentage} onChange={(e) => setRuleForm({ ...ruleForm, discountPercentage: Number(e.target.value) })} />
                </div>
                <div>
                  <label className="text-sm font-medium">Status</label>
                  <Select value={ruleForm.isActive ? 'true' : 'false'} onValueChange={(v) => setRuleForm({ ...ruleForm, isActive: v === 'true' })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="true">Active</SelectItem>
                      <SelectItem value="false">Inactive</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="flex justify-end gap-2 mt-3">
                {editingRule && (
                  <Button variant="outline" onClick={() => { setEditingRule(null); resetRuleForm() }}>Cancel</Button>
                )}
                <Button onClick={saveRule} disabled={!selectedProductId || isSavingRule}>{editingRule ? 'Update Rule' : 'Create Rule'}</Button>
              </div>
            </div>
          </div>

          <div className="space-y-2">
            {rules.length === 0 ? (
              <div className="text-sm text-muted-foreground">No rules yet for this product</div>
            ) : (
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                {rules.map((r: any) => (
                  <Card key={r.id}>
                    <CardHeader>
                      <CardTitle className="text-base">{r.ruleName}</CardTitle>
                      <CardDescription>Priority: {r.priorityOrder} • {r.isActive ? 'Active' : 'Inactive'}</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <div className="text-sm">{r.ruleDescription}</div>
                      <div className="text-xs text-muted-foreground">Min: {r.minThreshold} | Max: {r.maxThreshold}</div>
                      <div className="text-xs text-muted-foreground">Rate: {r.interestRate}% | Fee: {r.feeAmount} | Discount: {r.discountPercentage}%</div>
                      <div className="flex justify-end gap-2 pt-2">
                        <Button size="sm" variant="outline" onClick={() => editRule(r)}>Edit</Button>
                        <Button size="sm" variant="destructive" onClick={() => deleteRule(r.id)}>Delete</Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Open Account</CardTitle>
          <CardDescription>Create a Fixed Deposit account for the selected customer</CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(submitOpenAccount)} className="grid gap-4 md:grid-cols-2">
              <FormField
                control={form.control}
                name="customerId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Customer</FormLabel>
                    <Select value={field.value || selectedCustomer} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select customer" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {filteredCustomers.map(c => (
                          <SelectItem key={c.id} value={c.id}>{`${c.name} · @${c.username} · ID: ${c.id}`}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="productCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Product</FormLabel>
                    <Select value={field.value} onValueChange={field.onChange}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select product" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {products.map(p => (
                          <SelectItem key={p.code} value={p.code}>{p.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="principalAmount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Principal Amount (₹)</FormLabel>
                    <FormControl>
                      <Input type="number" value={field.value} onChange={(e) => field.onChange(Number(e.target.value))} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="interestRate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Interest Rate (% p.a.)</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.01" value={field.value} onChange={(e) => field.onChange(Number(e.target.value))} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="tenureMonths"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tenure (Months)</FormLabel>
                    <FormControl>
                      <Input type="number" value={field.value} onChange={(e) => field.onChange(Number(e.target.value))} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="branchCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Branch Code</FormLabel>
                    <FormControl>
                      <Input placeholder="HQ001" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="remarks"
                render={({ field }) => (
                  <FormItem className="md:col-span-2">
                    <FormLabel>Remarks</FormLabel>
                    <FormControl>
                      <Input placeholder="Optional" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="md:col-span-2 flex justify-end gap-2">
                <Button type="submit" disabled={isSubmitting}>Create Account</Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Accounts</CardTitle>
          <CardDescription>Accounts for the selected customer</CardDescription>
        </CardHeader>
        <CardContent>
          {isLoadingAccounts ? (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {[...Array(6)].map((_, i) => (
                <Skeleton key={i} className="h-48 w-full" />
              ))}
            </div>
          ) : accounts.length === 0 ? (
            <div className="text-sm text-muted-foreground">No accounts found</div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {accounts.map(acc => (
                <Card key={acc.id}>
                  <CardHeader>
                    <CardTitle className="text-lg">{acc.productName}</CardTitle>
                    <CardDescription>Account: {acc.accountNumber}</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Status</div>
                      <Badge>{acc.status}</Badge>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Principal</div>
                      <div className="text-sm">{acc.principalAmount}</div>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Rate (% p.a.)</div>
                      <div className="text-sm">{acc.interestRate}</div>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Balance</div>
                      <div className="text-sm">{acc.balance}</div>
                    </div>
                    <Separator />
                    <div className="flex justify-end gap-2">
                      <Button variant="destructive" size="sm" onClick={() => closeAccount(acc.accountNumber)}>Close</Button>
                      <Button variant="outline" size="sm" disabled>Change Number</Button>
                      <Button variant="outline" size="sm" disabled>Upgrade</Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default AdminDashboard
