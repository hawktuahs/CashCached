import { useEffect, useMemo, useState } from 'react'
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
import { useI18n } from '@/context/I18nContext'
import { 
  Calculator, 
  TrendingUp, 
  DollarSign, 
  Info,
  AlertTriangle,
} from 'lucide-react'
import { api } from '@/lib/api'
import { useStablecoinConversion } from '@/hooks/useStablecoinConversion'

const calculatorSchema = z.object({
  productCode: z.string().min(3, 'Product code is required'),
  principal: z.number().min(1, 'Minimum amount is 1 CashCached token'),
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
  interestRate: number
}

const interestRates = [
  { tenure: '1 year', rate: 6.5 },
  { tenure: '2 years', rate: 7.0 },
  { tenure: '3 years', rate: 7.5 },
  { tenure: '5 years', rate: 8.0 },
  { tenure: '10 years', rate: 8.5 },
]

const quickPrincipalOptions = [
  { principal: 100, tenure: 1 },
  { principal: 250, tenure: 2 },
  { principal: 500, tenure: 3 },
  { principal: 1000, tenure: 5 },
]

export function FdCalculator() {
  const { t } = useI18n()
  const { formatTokens, formatConvertedTokens, preferredCurrency } = useStablecoinConversion()
  const [result, setResult] = useState<CalculationResult | null>(null)
  const [isCalculating, setIsCalculating] = useState(false)
  const [products, setProducts] = useState<Array<{ code: string; name: string; compoundingFrequency?: string; minTermMonths?: number; maxTermMonths?: number; minAmount?: number; maxAmount?: number }>>([])
  const [isLoadingProducts, setIsLoadingProducts] = useState(false)
  const [customerId, setCustomerId] = useState<number | null>(null)
  const [errorReason, setErrorReason] = useState<string | null>(null)
  const [notes, setNotes] = useState<string[]>([])
  const [lockedCompLabel, setLockedCompLabel] = useState<string>('yearly')

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
        const root = res?.data
        const items = Array.isArray(root?.data) ? root.data : (Array.isArray(root) ? root : [])
        const mapped = items.map((p: any) => ({
          code: String(p.productCode ?? ''),
          name: String(p.productName ?? p.productCode ?? ''),
          compoundingFrequency: String(p.compoundingFrequency || ''),
          minTermMonths: typeof p.minTermMonths === 'number' ? p.minTermMonths : undefined,
          maxTermMonths: typeof p.maxTermMonths === 'number' ? p.maxTermMonths : undefined,
          minAmount: typeof p.minAmount === 'number' ? p.minAmount : undefined,
          maxAmount: typeof p.maxAmount === 'number' ? p.maxAmount : undefined,
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
    setErrorReason(null)
    try {
      const sanitizedCode = data.productCode.replace(/_/g, '-')
      const codeValid = /^[A-Z0-9-]{3,20}$/.test(sanitizedCode)
      if (!codeValid) {
        toast.error(t('calculator.error.productCodeInvalid'))
        return
      }
      const payload: any = {
        customerId: customerId ?? 0,
        productCode: sanitizedCode,
        principalAmount: data.principal,
        tenureMonths: Math.max(1, Math.round(data.tenure * 12)),
      }
      // omit compoundingFrequency so backend uses product's default
      const response = await api.post('/api/fd/calculate', payload)
      const r = (response?.data?.data ?? response?.data) as any
      const cf = r?.compoundingFrequency
      const cfLabel = typeof cf === 'number'
        ? (cf === 12 ? 'monthly' : cf === 4 ? 'quarterly' : cf === 2 ? 'semi-annual' : cf === 1 ? 'yearly' : cf === 0 ? 'simple' : `${cf}/year`)
        : String(cf ?? data.compoundingFrequency)
      const mapped: CalculationResult = {
        principal: Number(r.principalAmount ?? r.principal ?? data.principal),
        interestEarned: Number(r.interestEarned ?? 0),
        maturityAmount: Number(r.maturityAmount ?? 0),
        effectiveRate: Number(r.effectiveRate ?? 0),
        tenure: Number(r.tenureMonths ? Math.round(r.tenureMonths / 12) : data.tenure),
        compoundingFrequency: cfLabel,
        interestRate: Number(r.interestRate ?? 0),
      }
      setResult(mapped)
    } catch (error) {
      const msg = (error as any)?.response?.data?.message || t('calculator.error.calcFailed')
      setErrorReason(String(msg))
      toast.error(String(msg))
    } finally {
      setIsCalculating(false)
    }
  }

  const handleQuickCalculate = (principal: number, tenure: number) => {
    form.setValue('principal', principal)
    form.setValue('tenure', tenure)
    form.setValue('compoundingFrequency', 'yearly')
    const current = form.getValues()
    if (!current.productCode) return toast.error(t('calculator.error.enterProductFirst'))
    calculateFD({ productCode: current.productCode, principal, tenure, compoundingFrequency: 'yearly' })
  }

  const productCode = form.watch('productCode')
  const principalVal = form.watch('principal')
  const tenureYearsVal = form.watch('tenure')
  const freqVal = form.watch('compoundingFrequency')

  const selectedProduct = useMemo(() => products.find(p => p.code === productCode), [products, productCode])

  useEffect(() => {
    if (!selectedProduct) return
    const f = (selectedProduct.compoundingFrequency || '').toUpperCase()
    const label = f === 'MONTHLY' ? 'monthly' : f === 'QUARTERLY' ? 'quarterly' : 'yearly'
    setLockedCompLabel(label)
    form.setValue('compoundingFrequency', label)
  }, [selectedProduct, form])

  useEffect(() => {
    const ns: string[] = []
    const months = Math.max(1, Math.round((tenureYearsVal || 0) * 12))
    if (selectedProduct?.minTermMonths != null && selectedProduct?.maxTermMonths != null) {
      if (months < selectedProduct.minTermMonths || months > selectedProduct.maxTermMonths) {
        ns.push(`${t('calculator.validation.tenure')}: ${selectedProduct.minTermMonths} ${t('common.months')} ${t('common.to')} ${selectedProduct.maxTermMonths} ${t('common.months')}`)
      }
    }
    if (selectedProduct?.minAmount != null && selectedProduct?.maxAmount != null) {
      if ((principalVal || 0) < selectedProduct.minAmount || (principalVal || 0) > selectedProduct.maxAmount) {
        ns.push(
          `${t('calculator.validation.amount')}: ${formatTokens(selectedProduct.minAmount)} ${t('common.to')} ${formatTokens(selectedProduct.maxAmount)}`
        )
      }
    }
    if (freqVal !== 'yearly') {
      ns.push(t('calculator.validation.freq'))
    }
    setNotes(ns)
  }, [selectedProduct, principalVal, tenureYearsVal, freqVal, t])

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('calculator.title')}</h1>
          <p className="text-muted-foreground">
            {t('calculator.subtitle')}
          </p>
        </div>
        <Badge variant="outline" className="flex items-center gap-2">
          <Calculator className="h-4 w-4" />
          {t('calculator.badge')}
        </Badge>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calculator className="h-5 w-5" />
                {t('calculator.calculateHeadline')}
              </CardTitle>
              <CardTitle>{t('calculator.section.inputs')}</CardTitle>
              <CardDescription>{t('calculator.section.inputsDesc')}</CardDescription>
            </CardHeader>
            <CardContent>
              <Form {...form}>
                <form onSubmit={form.handleSubmit((data) => calculateFD({ productCode: data.productCode, principal: data.principal, tenure: data.tenure, compoundingFrequency: data.compoundingFrequency }))} className="space-y-4">
                  <FormField
                    control={form.control}
                    name="productCode"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t('calculator.product')}</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange} disabled={isLoadingProducts}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder={isLoadingProducts ? t('calculator.loadingProducts') : t('calculator.selectProduct')} />
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
                        <FormLabel>{t('calculator.principalTokens')}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min={1}
                            value={field.value}
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
                        <FormLabel>{t('calculator.tenureYears')}</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            step="0.5"
                            value={field.value}
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
                        <FormLabel>{t('calculator.compounding')}</FormLabel>
                        <Select value={field.value} onValueChange={field.onChange} disabled>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="monthly">{t('calculator.monthly')}</SelectItem>
                            <SelectItem value="quarterly">{t('calculator.quarterly')}</SelectItem>
                            <SelectItem value="yearly">{t('calculator.yearly')}</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="text-xs text-muted-foreground">{t('calculator.compounding')} locked to product: {selectedProduct?.compoundingFrequency || lockedCompLabel}</div>

                  <Button type="submit" disabled={isCalculating} className="w-full md:w-auto">
                    {isCalculating ? 'Calculating…' : t('calculator.calculate')}
                  </Button>
                </form>
              </Form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('calculator.quick.title')}</CardTitle>
              <CardDescription>{t('calculator.quick.subtitle')}</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid gap-3 sm:grid-cols-2">
                {quickPrincipalOptions.map((option) => (
                  <Button
                    key={`${option.principal}-${option.tenure}`}
                    variant="outline"
                    onClick={() => handleQuickCalculate(option.principal, option.tenure)}
                    disabled={isCalculating}
                  >
                    {formatTokens(option.principal)} · {option.tenure}y
                  </Button>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="h-5 w-5" />
                {t('calculator.quickCalculations')}
              </CardTitle>
              <CardDescription>
                {t('calculator.commonFdScenarios')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {interestRates.map((rate, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                  onClick={() => handleQuickCalculate(100, parseInt(rate.tenure))}
                >
                  <div>
                    <p className="font-medium">{formatTokens(100)} · {parseInt(rate.tenure)} {parseInt(rate.tenure) === 1 ? t('common.year') : t('common.years')}</p>
                    <p className="text-sm text-muted-foreground">
                      {t('calculator.interestRate')}: {rate.rate}%
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
                  {t('calculator.calculationResults')}
                </CardTitle>
                <CardDescription>
                  <CardTitle>{t('calculator.section.inputs')}</CardTitle>
                  <CardDescription>{t('calculator.section.inputsDesc')}</CardDescription>
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span>{t('calculator.product')}</span>
                  <span className="font-medium">{selectedProduct?.name || '—'}</span>
                </div>
                <div className="flex justify-between">
                  <span>{t('calculator.principalTokens')}</span>
                  <span className="font-medium">{formatTokens(result.principal)}</span>
                </div>
                <div className="flex justify-between">
                  <span>{t('calculator.result.maturityAmount')}</span>
                  <div className="flex flex-col items-end">
                    <span className="text-3xl font-bold text-primary">{formatTokens(result.maturityAmount)}</span>
                    <span className="text-xs text-muted-foreground">{formatConvertedTokens(result.maturityAmount, preferredCurrency)}</span>
                  </div>
                </div>
                <div className="flex justify-between">
                  <span>{t('calculator.section.result.interest')}</span>
                  <div className="flex flex-col items-end">
                    <span className="text-lg font-semibold">{formatTokens(result.interestEarned)}</span>
                    <span className="text-xs text-muted-foreground">{formatConvertedTokens(result.interestEarned, preferredCurrency)}</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4 pt-4">
                  <div className="space-y-2">
                    <p className="text-sm text-muted-foreground">{t('calculator.result.tenure')}</p>
                    <p className="text-lg font-semibold">{result.tenure} {t('calculator.result.years')}</p>
                  </div>
                  <div className="space-y-2">
                    <p className="text-sm text-muted-foreground">{t('calculator.result.effectiveRate')}</p>
                    <p className="text-lg font-semibold">{result.effectiveRate.toFixed(2)}%</p>
                  </div>
                </div>

                <div className="space-y-2">
                  <p className="text-sm text-muted-foreground">{t('calculator.interestRate')} ({t('calculator.nominal')})</p>
                  <p className="text-lg font-semibold">{result.interestRate.toFixed(2)}%</p>
                </div>

                <div className="pt-4">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Info className="h-4 w-4" />
                    <span>
                      {t('calculator.compounding')} {result.compoundingFrequency} | {t('calculator.totalReturn')}: {((result.interestEarned / result.principal) * 100).toFixed(2)}%
                    </span>
                  </div>
                </div>
              </CardContent>
            </Card>
            ) : (
            <Card>
              <CardHeader>
                <CardTitle>{t('calculator.results')}</CardTitle>
                <CardDescription>
                  {t('calculator.enterFdDetails')}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-center h-48 text-muted-foreground">
                  <div className="text-center">
                    <Calculator className="h-12 w-12 mx-auto mb-4 opacity-50" />
                    <p>{t('calculator.noCalculation')}</p>
                    <p className="text-sm">{t('calculator.fillForm')}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {result && notes.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Info className="h-5 w-5" />
                  {t('calculator.validation.notes')}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="list-disc pl-5 space-y-1">
                  {notes.map((n, i) => (
                    <li key={i} className="text-sm text-muted-foreground">{n}</li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          )}

          {!result && (errorReason || notes.length > 0) && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <AlertTriangle className="h-5 w-5 text-red-500" />
                  {t('calculator.validation.reason')}
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {errorReason && (
                  <p className="text-sm text-red-600">{errorReason}</p>
                )}
                {notes.length > 0 && (
                  <div>
                    <div className="text-sm font-medium mb-1">{t('calculator.validation.notes')}</div>
                    <ul className="list-disc pl-5 space-y-1">
                      {notes.map((n, i) => (
                        <li key={i} className="text-sm text-muted-foreground">{n}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="h-5 w-5" />
                {t('calculator.tips.title')}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-2">
                <h4 className="font-medium">{t('calculator.tips.higherTenure.title')}</h4>
                <p className="text-sm text-muted-foreground">
                  {t('calculator.tips.higherTenure.desc')}
                </p>
              </div>
              <div className="space-y-2">
                <h4 className="font-medium">{t('calculator.tips.compounding.title')}</h4>
                <p className="text-sm text-muted-foreground">
                  {t('calculator.tips.compounding.desc')}
                </p>
              </div>
              <div className="space-y-2">
                <h4 className="font-medium">{t('calculator.tips.tax.title')}</h4>
                <p className="text-sm text-muted-foreground">
                  {t('calculator.tips.tax.desc')}
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
