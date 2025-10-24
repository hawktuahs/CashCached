import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { 
  Calculator, 
  TrendingUp, 
  DollarSign, 
  Info,
  RefreshCw
} from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/context/AuthContext'

const calculatorSchema = z.object({
  productCode: z.string().min(3, 'Product code is required'),
  principal: z.number().min(1000, 'Minimum amount is ₹1,000'),
  tenure: z.number().min(1, 'Minimum tenure is 1 year'),
  compoundingFrequency: z.enum(['monthly', 'quarterly', 'yearly']),
})

type CalculatorFormData = z.infer<typeof calculatorSchema>

interface CalculationResult {
  principal: number
  interestEarned: number
  maturityAmount: number
  effectiveRate: number
  tenure: number
  compoundingFrequency: string
}

const interestRates = [
  { tenure: '1 year', rate: 6.5 },
  { tenure: '2 years', rate: 7.0 },
  { tenure: '3 years', rate: 7.5 },
  { tenure: '5 years', rate: 8.0 },
  { tenure: '10 years', rate: 8.5 },
]

export function FdCalculator() {
  const { user } = useAuth()
  const [result, setResult] = useState<CalculationResult | null>(null)
  const [isCalculating, setIsCalculating] = useState(false)
  const [products, setProducts] = useState<Array<{ code: string; name: string }>>([])
  const [isLoadingProducts, setIsLoadingProducts] = useState(false)
  const [customerId, setCustomerId] = useState<number | null>(null)

  const form = useForm<CalculatorFormData>({
    resolver: zodResolver(calculatorSchema),
    defaultValues: {
      productCode: '',
      principal: 100000,
      tenure: 1,
      compoundingFrequency: 'yearly',
    },
  })

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const preselect = params.get('product')
    const fetchProducts = async () => {
      setIsLoadingProducts(true)
      try {
        const res = await api.get('/api/v1/product')
        const items = Array.isArray(res.data) ? res.data : []
        const mapped = items.map((p: any) => ({
          code: String(p.productCode ?? ''),
          name: String(p.productName ?? p.productCode ?? '')
        }))
        setProducts(mapped)
        const current = form.getValues('productCode')
        if (preselect) form.setValue('productCode', preselect)
        else if (!current && mapped.length > 0) form.setValue('productCode', mapped[0].code)
      } catch (e) {
        // leave products empty
      } finally {
        setIsLoadingProducts(false)
      }
    }
    fetchProducts()
  }, [form])

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await api.get('/api/customer/profile')
        if (res?.data?.id) setCustomerId(Number(res.data.id))
      } catch {}
    }
    fetchProfile()
  }, [])

  const calculateFD = async (data: CalculatorFormData) => {
    setIsCalculating(true)
    try {
      const freq = data.compoundingFrequency === 'monthly' ? 12 : data.compoundingFrequency === 'quarterly' ? 4 : 1
      const sanitizedCode = data.productCode.replace(/_/g, '-')
      const codeValid = /^[A-Z0-9-]{3,20}$/.test(sanitizedCode)
      if (!codeValid) {
        toast.error('Selected product code is not compatible with calculator. Please choose a product with a hyphenated code (A-Z, 0-9, - only, max 20 chars).')
        return
      }
      const payload = {
        customerId: customerId ?? 0,
        productCode: sanitizedCode,
        principalAmount: data.principal,
        tenureMonths: Math.max(1, Math.round(data.tenure * 12)),
        compoundingFrequency: freq,
      }
      const response = await api.post('/api/fd/calculate', payload)
      const r = response.data as any
      const mapped: CalculationResult = {
        principal: Number(r.principalAmount ?? r.principal ?? data.principal),
        interestEarned: Number(r.interestEarned ?? 0),
        maturityAmount: Number(r.maturityAmount ?? 0),
        effectiveRate: Number(r.effectiveRate ?? 0),
        tenure: Number(r.tenureMonths ? Math.round(r.tenureMonths / 12) : data.tenure),
        compoundingFrequency: String(r.compoundingFrequency ?? data.compoundingFrequency),
      }
      setResult(mapped)
    } catch (error) {
      toast.error('Failed to calculate FD returns')
    } finally {
      setIsCalculating(false)
    }
  }

  const onSubmit = (data: CalculatorFormData) => {
    calculateFD(data)
  }

  const handleQuickCalculate = (principal: number, tenure: number, _rate: number) => {
    form.setValue('principal', principal)
    form.setValue('tenure', tenure)
    form.setValue('compoundingFrequency', 'yearly')
    const current = form.getValues()
    if (!current.productCode) return toast.error('Please enter a product code first')
    calculateFD({ productCode: current.productCode, principal, tenure, compoundingFrequency: 'yearly' })
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">FD Calculator</h1>
          <p className="text-muted-foreground">
            Calculate your fixed deposit returns and plan your investments
          </p>
        </div>
        <Badge variant="outline" className="flex items-center gap-2">
          <Calculator className="h-4 w-4" />
          Investment Calculator
        </Badge>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calculator className="h-5 w-5" />
                Calculate FD Returns
              </CardTitle>
              <CardDescription>
                Enter your investment details to calculate returns
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                  <FormField
                    control={form.control}
                    name="productCode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Product</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange} disabled={isLoadingProducts}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={isLoadingProducts ? 'Loading products...' : 'Select a product'} />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {products.map(p => (
                              <SelectItem key={p.code} value={p.code}>
                                {p.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="principal"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Principal Amount (₹)</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            placeholder="100000"
                            {...field}
                            onChange={(e) => field.onChange(Number(e.target.value))}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="tenure"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Tenure (Years)</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            placeholder="1"
                            {...field}
                            onChange={(e) => field.onChange(Number(e.target.value))}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  

                  <FormField
                    control={form.control}
                    name="compoundingFrequency"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Compounding Frequency</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select frequency" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="monthly">Monthly</SelectItem>
                            <SelectItem value="quarterly">Quarterly</SelectItem>
                            <SelectItem value="yearly">Yearly</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <Button type="submit" className="w-full" disabled={isCalculating}>
                    {isCalculating ? (
                      <>
                        <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                        Calculating...
                      </>
                    ) : (
                      <>
                        <Calculator className="mr-2 h-4 w-4" />
                        Calculate Returns
                      </>
                    )}
                  </Button>
                </form>
              </Form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="h-5 w-5" />
                Quick Calculations
              </CardTitle>
              <CardDescription>
                Common FD scenarios with current rates
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {interestRates.map((rate, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-3 border rounded-lg hover:bg-accent cursor-pointer transition-colors"
                  onClick={() => handleQuickCalculate(100000, parseInt(rate.tenure), rate.rate)}
                >
                  <div>
                    <p className="font-medium">₹1,00,000 for {rate.tenure}</p>
                    <p className="text-sm text-muted-foreground">
                      Interest Rate: {rate.rate}% p.a.
                    </p>
                  </div>
                  <Badge variant="secondary">{rate.rate}%</Badge>
                </div>
              ))}
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          {result ? (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <DollarSign className="h-5 w-5" />
                  Calculation Results
                </CardTitle>
                <CardDescription>
                  Your FD investment breakdown
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">Principal Amount</p>
                    <p className="text-2xl font-bold">{formatCurrency(result.principal)}</p>
                  </div>
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">Interest Earned</p>
                    <p className="text-2xl font-bold text-green-600">
                      {formatCurrency(result.interestEarned)}
                    </p>
                  </div>
                </div>

                <Separator />

                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">Maturity Amount</p>
                  <p className="text-3xl font-bold text-primary">
                    {formatCurrency(result.maturityAmount)}
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-4 pt-4">
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">Tenure</p>
                    <p className="text-lg font-semibold">{result.tenure} years</p>
                  </div>
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">Effective Rate</p>
                    <p className="text-lg font-semibold">{result.effectiveRate.toFixed(2)}%</p>
                  </div>
                </div>

                <div className="pt-4">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Info className="h-4 w-4" />
                    <span>
                      Compounding: {result.compoundingFrequency} | 
                      Total Return: {((result.interestEarned / result.principal) * 100).toFixed(2)}%
                    </span>
                  </div>
                </div>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle>Calculation Results</CardTitle>
                <CardDescription>
                  Enter your FD details to see the calculation results
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-center h-48 text-muted-foreground">
                  <div className="text-center">
                    <Calculator className="h-12 w-12 mx-auto mb-4 opacity-50" />
                    <p>No calculation yet</p>
                    <p className="text-sm">Fill in the form to see results</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="h-5 w-5" />
                FD Calculator Tips
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-2">
                <h4 className="font-medium">Higher Tenure = Better Returns</h4>
                <p className="text-sm text-muted-foreground">
                  Longer-term FDs typically offer higher interest rates.
                </p>
              </div>
              <div className="space-y-2">
                <h4 className="font-medium">Compounding Frequency</h4>
                <p className="text-sm text-muted-foreground">
                  More frequent compounding (monthly/quarterly) yields better returns.
                </p>
              </div>
              <div className="space-y-2">
                <h4 className="font-medium">Tax Implications</h4>
                <p className="text-sm text-muted-foreground">
                  FD interest is taxable as per your income tax slab.
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
