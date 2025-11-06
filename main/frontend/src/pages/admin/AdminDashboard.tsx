import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { Download, RefreshCw } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Label } from "@/components/ui/label";

import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";
import { api } from "@/lib/api";
import { useStablecoinConversion } from "@/hooks/useStablecoinConversion";
import { ReportsSection } from "@/components/admin/ReportsSection";

interface Account {
  id: string;
  accountNumber: string;
  accountType: string;
  balance: number;
  interestRate: number;
  maturityDate: string;
  status: "ACTIVE" | "MATURED" | "CLOSED";
  createdAt: string;
  productName: string;
  principalAmount: number;
  productCode?: string;
}

const openAccountSchema = z.object({
  customerId: z.string().min(1),
  productCode: z.string().min(1),
  principalAmount: z.number().min(1000),
  interestRate: z.number().min(0.01).max(20),
  tenureMonths: z.number().min(1).max(120),
  branchCode: z.string().regex(/^[A-Z0-9]{3,20}$/),
  remarks: z.string().max(500).optional().or(z.literal("")),
});

type OpenAccountFormData = z.infer<typeof openAccountSchema>;

export function AdminDashboard() {
  const { user } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const { formatTokens, formatConvertedTokens, preferredCurrency } =
    useStablecoinConversion();

  const isStaff = user?.role === "ADMIN" || user?.role === "BANKOFFICER";

  const [customers, setCustomers] = useState<
    Array<{ id: string; name: string; username: string }>
  >([]);
  const [products, setProducts] = useState<
    Array<{ id: number; code: string; name: string }>
  >([]);
  const [selectedCustomer, setSelectedCustomer] = useState<string>("");
  const [customerSearch, setCustomerSearch] = useState("");
  const [selectedProductId, setSelectedProductId] = useState<number | null>(
    null
  );
  const [rules, setRules] = useState<Array<any>>([]);
  const [isSavingRule, setIsSavingRule] = useState(false);
  const [editingRule, setEditingRule] = useState<any | null>(null);
  const [systemTime, setSystemTime] = useState<string>("");
  const [offsetSeconds, setOffsetSeconds] = useState<number>(0);
  const [advanceBy, setAdvanceBy] = useState<string>("3600");
  const emptyRule = {
    productId: 0,
    ruleName: "",
    ruleDescription: "",
    minThreshold: 0,
    maxThreshold: 0,
    interestRate: 0,
    feeAmount: 0,
    discountPercentage: 0,
    priorityOrder: 0,
    isActive: true,
  };

  const refreshSystemTime = async () => {
    try {
      const t = await api.get("/api/accounts/admin/time");
      const d = t?.data?.data ?? t?.data;
      if (d) {
        setSystemTime(String(d.systemTime));
        setOffsetSeconds(Number(d.offsetSeconds ?? 0));
      }
    } catch {
      toast.error("Failed to load system time");
    }
  };

  const setAbsoluteTime = async (iso: string) => {
    try {
      await api.put("/api/accounts/admin/time", { systemTime: iso });
      toast.success("System time updated");
      refreshSystemTime();
    } catch {
      toast.error("Failed to update system time");
    }
  };

  const advanceTime = async (sec: number) => {
    try {
      await api.post("/api/accounts/admin/time/advance", { seconds: sec });
      toast.success("System time advanced");
      refreshSystemTime();
    } catch {
      toast.error("Failed to advance system time");
    }
  };

  const resetTime = async () => {
    try {
      await api.post("/api/accounts/admin/time/reset", {});
      toast.success("System time reset");
      refreshSystemTime();
    } catch {
      toast.error("Failed to reset system time");
    }
  };
  const [ruleForm, setRuleForm] = useState<any>({ ...emptyRule });

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoadingCustomers, setIsLoadingCustomers] = useState(true);
  const [isLoadingAccounts, setIsLoadingAccounts] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [isUpgradeOpen, setIsUpgradeOpen] = useState(false);
  const [upgradeTarget, setUpgradeTarget] = useState<Account | null>(null);
  const [upgradeForm, setUpgradeForm] = useState<{
    productCode: string;
    interestRate: string;
    tenureMonths: string;
  }>({ productCode: "", interestRate: "", tenureMonths: "" });
  const [isUpgrading, setIsUpgrading] = useState(false);

  const [isProductOpen, setIsProductOpen] = useState(false);
  const [isSavingProduct, setIsSavingProduct] = useState(false);
  const [productForm, setProductForm] = useState({
    productCode: "",
    productName: "",
    minInterestRate: "0",
    maxInterestRate: "0",
    minTermMonths: "1",
    maxTermMonths: "12",
    minAmount: "0",
    maxAmount: "0",
    effectiveDate: new Date().toISOString().slice(0, 10),
    compoundingFrequency: "ANNUAL",
  });

  const form = useForm<OpenAccountFormData>({
    resolver: zodResolver(openAccountSchema),
    defaultValues: {
      customerId: "",
      productCode: "",
      principalAmount: 100000,
      interestRate: 7,
      tenureMonths: 12,
      branchCode: "HQ001",
      remarks: "",
    },
  });

  useEffect(() => {
    if (!isStaff) {
      navigate("/dashboard", { replace: true });
      return;
    }
    const load = async () => {
      setIsLoadingCustomers(true);
      try {
        const [custRes, prodRes] = await Promise.all([
          api.get("/api/customer/all"),
          api.get("/api/v1/product"),
        ]);
        const c = Array.isArray(custRes.data) ? custRes.data : [];
        const root = prodRes?.data;
        const p = Array.isArray(root?.data)
          ? root.data
          : Array.isArray(root)
          ? root
          : [];
        const mappedCustomers = c.map((u: any) => ({
          id: String(u.id),
          name: String(u.fullName || u.username || u.email || u.id),
          username: String(u.username || ""),
        }));
        const mappedProducts = p.map((x: any) => ({
          id: Number(x.id ?? 0),
          code: String(x.productCode || ""),
          name: String(x.productName || x.productCode || ""),
        }));
        console.log(
          "AdminDashboard loaded products:",
          mappedProducts.length,
          "items"
        );
        setCustomers(mappedCustomers);
        setProducts(mappedProducts);
        if (mappedCustomers.length > 0)
          setSelectedCustomer(mappedCustomers[0].id);
        if (mappedProducts.length > 0)
          setSelectedProductId(mappedProducts[0].id);
        try {
          const t = await api.get("/api/accounts/admin/time");
          const d = t?.data?.data ?? t?.data;
          if (d) {
            setSystemTime(String(d.systemTime));
            setOffsetSeconds(Number(d.offsetSeconds ?? 0));
          }
        } catch {}
      } finally {
        setIsLoadingCustomers(false);
      }
    };
    load();
  }, [isStaff, navigate]);

  const resetRuleForm = () =>
    setRuleForm({ ...emptyRule, productId: selectedProductId || 0 });

  const saveRule = async () => {
    if (!selectedProductId) return;
    setIsSavingRule(true);
    try {
      const payload = { ...ruleForm, productId: selectedProductId };
      if (editingRule?.id) {
        await api.put(`/api/v1/pricing-rule/${editingRule.id}`, payload);
      } else {
        await api.post("/api/v1/pricing-rule", payload);
      }
      const res = await api.get(
        `/api/v1/pricing-rule/product/${selectedProductId}`
      );
      const list = res?.data?.data ?? res?.data ?? [];
      setRules(Array.isArray(list) ? list : []);
      setEditingRule(null);
      resetRuleForm();
      toast.success("Rule saved");
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to save rule";
      toast.error(String(msg));
    } finally {
      setIsSavingRule(false);
    }
  };

  const editRule = (r: any) => {
    setEditingRule(r);
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
    });
  };

  const deleteRule = async (id: number) => {
    if (!selectedProductId) return;
    try {
      await api.delete(`/api/v1/pricing-rule/${id}`);
      const res = await api.get(
        `/api/v1/pricing-rule/product/${selectedProductId}`
      );
      const list = res?.data?.data ?? res?.data ?? [];
      setRules(Array.isArray(list) ? list : []);
      toast.success("Rule deleted");
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to delete rule";
      toast.error(String(msg));
    }
  };

  const fetchAccounts = async () => {
    if (!selectedCustomer) return;
    setIsLoadingAccounts(true);
    try {
      const res = await api.get(`/api/accounts/customer/${selectedCustomer}`, {
        params: { _t: Date.now() },
        headers: {
          "Cache-Control": "no-cache",
          Pragma: "no-cache",
        },
      });
      const payload = res?.data?.data ?? res?.data ?? [];
      console.log("Admin Dashboard - Raw account data:", payload);

      const accountsArray = Array.isArray(payload) ? payload : [];

      const detailedAccounts = await Promise.all(
        accountsArray.map(async (acc: any) => {
          try {
            const detailRes = await api.get(
              `/api/accounts/${acc.accountNo ?? acc.accountNumber}`,
              {
                params: { _t: Date.now() },
                headers: {
                  "Cache-Control": "no-cache",
                  Pragma: "no-cache",
                },
              }
            );
            const detail = detailRes?.data?.data ?? detailRes?.data;
            return detail || acc;
          } catch (error) {
            console.warn(
              `Failed to fetch details for account ${
                acc.accountNo ?? acc.accountNumber
              }:`,
              error
            );
            return acc;
          }
        })
      );

      console.log("Admin Dashboard - Detailed account data:", detailedAccounts);
      const mapped = mapAccountsFromPayload(detailedAccounts);
      console.log("Admin Dashboard - Mapped accounts:", mapped);
      setAccounts(mapped);
    } catch (error) {
      console.error("Failed to fetch accounts:", error);
      toast.error("Failed to load accounts");
    } finally {
      setIsLoadingAccounts(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, [selectedCustomer]);

  useEffect(() => {
    const handleWalletRefresh = (event: CustomEvent) => {
      const detail = event.detail;
      if (detail?.customerId === selectedCustomer || !detail?.customerId) {
        fetchAccounts();
      }
    };

    window.addEventListener(
      "cashcached:refresh-wallet" as any,
      handleWalletRefresh
    );

    return () => {
      window.removeEventListener(
        "cashcached:refresh-wallet" as any,
        handleWalletRefresh
      );
    };
  }, [selectedCustomer]);

  useEffect(() => {
    const loadRules = async () => {
      if (!selectedProductId) return;
      try {
        const res = await api.get(
          `/api/v1/pricing-rule/product/${selectedProductId}`
        );
        const list = Array.isArray(res?.data) ? res.data : [];
        setRules(list);
      } catch {
        try {
          const code = products.find((p) => p.id === selectedProductId)?.code;
          if (code) {
            const statusRes = await api.get(`/api/v1/product/status/${code}`);
            const status = statusRes?.data;
            const active = Array.isArray(status?.applicablePricingRules)
              ? status.applicablePricingRules
              : [];
            setRules(active);
          } else {
            setRules([]);
          }
        } catch {
          setRules([]);
        }
      }
    };
    loadRules();
  }, [selectedProductId, products]);

  const exportCsv = () => {
    const rows = [
      [
        "Account Number",
        "Product Name",
        "Type",
        "Status",
        "Principal Amount",
        "Balance",
        "Interest Rate",
        "Opened On",
        "Maturity Date",
      ],
      ...accounts.map((a) => [
        a.accountNumber,
        a.productName,
        a.accountType,
        a.status,
        String(a.principalAmount),
        String(a.balance),
        String(a.interestRate),
        new Date(a.createdAt).toISOString(),
        new Date(a.maturityDate).toISOString(),
      ]),
    ];
    const csv = rows
      .map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(","))
      .join("\n");
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "accounts.csv";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const selectedCustomerName = useMemo(
    () => customers.find((c) => c.id === selectedCustomer)?.name || "",
    [customers, selectedCustomer]
  );
  const filteredCustomers = useMemo(() => {
    const q = customerSearch.trim().toLowerCase();
    if (!q) return customers;
    return customers.filter(
      (c) =>
        c.id.toLowerCase().includes(q) ||
        c.name.toLowerCase().includes(q) ||
        c.username.toLowerCase().includes(q)
    );
  }, [customers, customerSearch]);

  const submitOpenAccount = async (data: OpenAccountFormData) => {
    setIsSubmitting(true);
    try {
      const payload = {
        customerId: data.customerId,
        productCode: data.productCode,
        principalAmount: data.principalAmount,
        interestRate: data.interestRate,
        tenureMonths: data.tenureMonths,
        branchCode: data.branchCode,
        remarks: data.remarks || "",
      };
      const res = await api.post("/api/accounts/create", payload);
      if (res?.data?.accountNo || res?.data?.accountNumber)
        toast.success("Account created successfully");
      const refreshed = await api.get(
        `/api/accounts/customer/${selectedCustomer}`
      );
      const payload2 = refreshed?.data?.data ?? refreshed?.data ?? [];
      setAccounts(mapAccountsFromPayload(payload2));
    } catch {
      toast.error("Failed to create account");
    } finally {
      setIsSubmitting(false);
    }
  };

  const closeAccount = async (accountNo: string) => {
    try {
      await api.put(`/api/accounts/${accountNo}/close`, {
        closureReason: "Closed by admin",
      });
      const refreshed = await api.get(
        `/api/accounts/customer/${selectedCustomer}`
      );
      const payload = refreshed?.data?.data ?? refreshed?.data ?? [];
      setAccounts(mapAccountsFromPayload(payload));
      toast.success("Account closed");
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to close account";
      toast.error(String(msg));
    }
  };

  const reopenAccount = async (accountNo: string) => {
    try {
      await api.put(`/api/accounts/${accountNo}/reopen`);
      const refreshed = await api.get(
        `/api/accounts/customer/${selectedCustomer}`
      );
      const payload = refreshed?.data?.data ?? refreshed?.data ?? [];
      setAccounts(mapAccountsFromPayload(payload));
      toast.success("Account reopened");
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to reopen account";
      toast.error(String(msg));
    }
  };

  const openUpgrade = (acc: Account) => {
    setUpgradeTarget(acc);
    setUpgradeForm({
      productCode: acc.productCode || "",
      interestRate: acc.interestRate ? String(acc.interestRate) : "",
      tenureMonths: "",
    });
    setIsUpgradeOpen(true);
  };

  const submitUpgrade = async () => {
    if (!upgradeTarget) return;
    const payload: any = {};
    if (
      upgradeForm.productCode &&
      upgradeForm.productCode !== (upgradeTarget.productCode || "")
    )
      payload.productCode = upgradeForm.productCode;
    if (upgradeForm.interestRate)
      payload.interestRate = Number(upgradeForm.interestRate);
    if (upgradeForm.tenureMonths)
      payload.tenureMonths = Number(upgradeForm.tenureMonths);
    if (Object.keys(payload).length === 0) {
      toast.message("No changes to apply");
      return;
    }
    setIsUpgrading(true);
    try {
      await api.put(
        `/api/accounts/${upgradeTarget.accountNumber}/upgrade`,
        payload
      );
      const refreshed = await api.get(
        `/api/accounts/customer/${selectedCustomer}`
      );
      const data = refreshed?.data?.data ?? refreshed?.data ?? [];
      setAccounts(mapAccountsFromPayload(data));
      setIsUpgradeOpen(false);
      setUpgradeTarget(null);
      toast.success("Account upgraded");
    } catch (e: any) {
      const msg = e?.response?.data?.message || "Failed to upgrade account";
      toast.error(String(msg));
    } finally {
      setIsUpgrading(false);
    }
  };

  const mapAccountsFromPayload = (payload: any): Account[] => {
    const arr = Array.isArray(payload) ? payload : [];
    return arr.map((a: any) => {
      const balance = Number(
        a.currentBalance ??
          a.balance ??
          a.accountBalance ??
          a.maturityAmount ??
          a.principalAmount ??
          0
      );

      console.log(`Account ${a.accountNo ?? a.accountNumber}: raw data =`, {
        currentBalance: a.currentBalance,
        balance: a.balance,
        accountBalance: a.accountBalance,
        maturityAmount: a.maturityAmount,
        principalAmount: a.principalAmount,
        computed: balance,
      });

      return {
        id: String(a.id ?? ""),
        accountNumber: String(a.accountNo ?? a.accountNumber ?? ""),
        accountType: "FIXED_DEPOSIT",
        balance: balance,
        interestRate: Number(a.interestRate ?? 0),
        maturityDate: String(a.maturityDate ?? new Date().toISOString()),
        status: String(a.status ?? "ACTIVE") as any,
        createdAt: String(a.createdAt ?? new Date().toISOString()),
        productName: String(a.productName ?? a.productCode ?? "Product"),
        principalAmount: Number(a.principalAmount ?? 0),
        productCode: String(a.productCode ?? ""),
      };
    });
  };

  const refreshProducts = async () => {
    const prodRes = await api.get("/api/v1/product");
    const root = prodRes?.data;
    const p = Array.isArray(root?.data)
      ? root.data
      : Array.isArray(root)
      ? root
      : [];
    const mapped = p.map((x: any) => ({
      id: Number(x.id ?? 0),
      code: String(x.productCode || ""),
      name: String(x.productName || x.productCode || ""),
    }));
    console.log("refreshProducts loaded:", mapped.length, "items");
    setProducts(mapped);
    return mapped;
  };

  if (!isStaff) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Admin Dashboard</h1>
          <p className="text-muted-foreground">
            Manage customers and their FD accounts
          </p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={exportCsv}
            disabled={accounts.length === 0}
          >
            <Download className="mr-2 h-4 w-4" />
            Export CSV
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Customer</CardTitle>
          <CardDescription>
            Select a customer to manage accounts
          </CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <Input
              placeholder="Search by ID, name, or username"
              value={customerSearch}
              onChange={(e) => setCustomerSearch(e.target.value)}
            />
            {isLoadingCustomers ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <Select
                value={selectedCustomer}
                onValueChange={(v) => {
                  setSelectedCustomer(v);
                  form.setValue("customerId", v);
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select customer" />
                </SelectTrigger>
                <SelectContent>
                  {filteredCustomers.map((c) => (
                    <SelectItem
                      key={c.id}
                      value={c.id}
                    >{`${c.name} · @${c.username} · ID: ${c.id}`}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>
          <div className="grid gap-2">
            <div className="text-sm text-muted-foreground">Selected</div>
            <div className="text-base">{selectedCustomerName || "None"}</div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>System Time</CardTitle>
          <CardDescription>
            Control global time for accrual simulation
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid md:grid-cols-3 gap-4 items-end">
            <div className="space-y-1">
              <div className="text-sm font-medium">Current System Time</div>
              <div className="text-sm font-mono">{systemTime || "—"}</div>
              <div className="text-xs text-muted-foreground">
                Offset: {offsetSeconds}s
              </div>
              <Button
                variant="outline"
                size="sm"
                className="mt-2"
                onClick={refreshSystemTime}
              >
                Refresh
              </Button>
            </div>
            <div className="space-y-1">
              <div className="text-sm font-medium">Set Absolute Time (ISO)</div>
              <Input
                placeholder="2025-12-31T00:00:00Z"
                onKeyDown={(e) => {
                  if (e.key === "Enter")
                    setAbsoluteTime((e.target as HTMLInputElement).value);
                }}
              />
              <div className="text-xs text-muted-foreground">
                Press Enter to apply
              </div>
            </div>
            <div className="space-y-1">
              <div className="text-sm font-medium">Advance By (seconds)</div>
              <div className="flex gap-2">
                <Input
                  value={advanceBy}
                  onChange={(e) => setAdvanceBy(e.target.value)}
                />
                <Button onClick={() => advanceTime(Number(advanceBy || "0"))}>
                  Advance
                </Button>
              </div>
              <div className="flex gap-2 mt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setAdvanceBy("86400")}
                >
                  +1d
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setAdvanceBy("2592000")}
                >
                  +30d
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setAdvanceBy("7776000")}
                >
                  +90d
                </Button>
                <Button variant="destructive" size="sm" onClick={resetTime}>
                  Reset
                </Button>
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
              <Select
                value={selectedProductId ? String(selectedProductId) : ""}
                onValueChange={(v) => setSelectedProductId(Number(v))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent>
                  {products.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="md:col-span-2">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-sm font-medium">Rule Name</label>
                  <Input
                    value={ruleForm.ruleName}
                    onChange={(e) =>
                      setRuleForm({ ...ruleForm, ruleName: e.target.value })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Priority</label>
                  <Input
                    type="number"
                    value={ruleForm.priorityOrder}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        priorityOrder: Number(e.target.value),
                      })
                    }
                  />
                </div>
                <div className="col-span-2">
                  <label className="text-sm font-medium">Description</label>
                  <Input
                    value={ruleForm.ruleDescription}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        ruleDescription: e.target.value,
                      })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Min Threshold</label>
                  <Input
                    type="number"
                    value={ruleForm.minThreshold}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        minThreshold: Number(e.target.value),
                      })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Max Threshold</label>
                  <Input
                    type="number"
                    value={ruleForm.maxThreshold}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        maxThreshold: Number(e.target.value),
                      })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Interest Rate</label>
                  <Input
                    type="number"
                    step="0.01"
                    value={ruleForm.interestRate}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        interestRate: Number(e.target.value),
                      })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Fee Amount</label>
                  <Input
                    type="number"
                    value={ruleForm.feeAmount}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        feeAmount: Number(e.target.value),
                      })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Discount %</label>
                  <Input
                    type="number"
                    step="0.01"
                    value={ruleForm.discountPercentage}
                    onChange={(e) =>
                      setRuleForm({
                        ...ruleForm,
                        discountPercentage: Number(e.target.value),
                      })
                    }
                  />
                </div>
                <div>
                  <label className="text-sm font-medium">Status</label>
                  <Select
                    value={ruleForm.isActive ? "true" : "false"}
                    onValueChange={(v) =>
                      setRuleForm({ ...ruleForm, isActive: v === "true" })
                    }
                  >
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
                  <Button
                    variant="outline"
                    onClick={() => {
                      setEditingRule(null);
                      resetRuleForm();
                    }}
                  >
                    Cancel
                  </Button>
                )}
                <Button
                  onClick={saveRule}
                  disabled={!selectedProductId || isSavingRule}
                >
                  {editingRule ? "Update Rule" : "Create Rule"}
                </Button>
              </div>
            </div>
          </div>

          <div className="space-y-2">
            {rules.length === 0 ? (
              <div className="text-sm text-muted-foreground">
                No rules yet for this product
              </div>
            ) : (
              <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
                {rules.map((r: any) => (
                  <Card key={r.id}>
                    <CardHeader>
                      <CardTitle className="text-base">{r.ruleName}</CardTitle>
                      <CardDescription>
                        Priority: {r.priorityOrder} •{" "}
                        {r.isActive ? "Active" : "Inactive"}
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-2">
                      <div className="text-sm">{r.ruleDescription}</div>
                      <div className="text-xs text-muted-foreground">
                        Min: {r.minThreshold} | Max: {r.maxThreshold}
                      </div>
                      <div className="text-xs text-muted-foreground">
                        Rate: {r.interestRate}% | Fee: {r.feeAmount} | Discount:{" "}
                        {r.discountPercentage}%
                      </div>
                      <div className="flex justify-end gap-2 pt-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => editRule(r)}
                        >
                          Edit
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={() => deleteRule(r.id)}
                        >
                          Delete
                        </Button>
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
          <CardDescription>
            Create a Fixed Deposit account for the selected customer
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form
              onSubmit={form.handleSubmit(submitOpenAccount)}
              className="grid gap-4 md:grid-cols-2"
            >
              <FormField
                control={form.control}
                name="customerId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Customer</FormLabel>
                    <Select
                      value={field.value || selectedCustomer}
                      onValueChange={field.onChange}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select customer" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {filteredCustomers.map((c) => (
                          <SelectItem
                            key={c.id}
                            value={c.id}
                          >{`${c.name} · @${c.username} · ID: ${c.id}`}</SelectItem>
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
                        {products.map((p) => (
                          <SelectItem key={p.code} value={p.code}>
                            {p.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button
                      type="button"
                      variant="outline"
                      className="mt-2"
                      onClick={() => setIsProductOpen(true)}
                    >
                      Create Product
                    </Button>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="principalAmount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t("calculator.principalTokens")}</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
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
                name="interestRate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Interest Rate (% p.a.)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        step="0.01"
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
                name="tenureMonths"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tenure (Months)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
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
                <Button type="submit" disabled={isSubmitting}>
                  Create Account
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Accounts</CardTitle>
              <CardDescription>
                Accounts for the selected customer
              </CardDescription>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchAccounts()}
              disabled={isLoadingAccounts || !selectedCustomer}
            >
              <RefreshCw
                className={`mr-2 h-4 w-4 ${
                  isLoadingAccounts ? "animate-spin" : ""
                }`}
              />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {isLoadingAccounts ? (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {[...Array(6)].map((_, i) => (
                <Skeleton key={i} className="h-48 w-full" />
              ))}
            </div>
          ) : accounts.length === 0 ? (
            <div className="text-sm text-muted-foreground">
              No accounts found
            </div>
          ) : (
            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
              {accounts.map((acc) => (
                <Card key={acc.id}>
                  <CardHeader>
                    <CardTitle className="text-lg">{acc.productName}</CardTitle>
                    <CardDescription>
                      Account: {acc.accountNumber}
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Status</div>
                      <Badge>{acc.status}</Badge>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Principal</div>
                      <div className="text-right text-sm">
                        <div>{formatTokens(acc.principalAmount)}</div>
                        <div className="text-xs text-muted-foreground">
                          {formatConvertedTokens(
                            acc.principalAmount,
                            preferredCurrency
                          )}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Rate (% p.a.)</div>
                      <div className="text-sm">{acc.interestRate}</div>
                    </div>
                    <div className="flex items-center justify-between">
                      <div className="text-sm">Balance</div>
                      <div className="text-right text-sm">
                        <div>{formatTokens(acc.balance)}</div>
                        <div className="text-xs text-muted-foreground">
                          {formatConvertedTokens(
                            acc.balance,
                            preferredCurrency
                          )}
                        </div>
                      </div>
                    </div>
                    <Separator />
                    <div className="flex justify-end gap-2">
                      {acc.status === "CLOSED" ? (
                        <Button
                          size="sm"
                          onClick={() => reopenAccount(acc.accountNumber)}
                        >
                          Reopen
                        </Button>
                      ) : (
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => closeAccount(acc.accountNumber)}
                        >
                          Close
                        </Button>
                      )}
                      <Button variant="outline" size="sm" disabled>
                        Change Number
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => openUpgrade(acc)}
                      >
                        Upgrade
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ReportsSection />

      <Sheet open={isUpgradeOpen} onOpenChange={setIsUpgradeOpen}>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>Upgrade Account</SheetTitle>
          </SheetHeader>
          <div className="p-4 space-y-3">
            <div className="space-y-1">
              <Label>Product Code</Label>
              <Select
                value={upgradeForm.productCode}
                onValueChange={(v) =>
                  setUpgradeForm((s) => ({ ...s, productCode: v }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent>
                  {products.map((p) => (
                    <SelectItem key={p.code} value={p.code}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Interest Rate (% p.a.)</Label>
              <Input
                type="number"
                step="0.01"
                value={upgradeForm.interestRate}
                onChange={(e) =>
                  setUpgradeForm((s) => ({
                    ...s,
                    interestRate: e.target.value,
                  }))
                }
              />
            </div>
            <div className="space-y-1">
              <Label>Tenure (Months)</Label>
              <Input
                type="number"
                value={upgradeForm.tenureMonths}
                onChange={(e) =>
                  setUpgradeForm((s) => ({
                    ...s,
                    tenureMonths: e.target.value,
                  }))
                }
              />
            </div>
          </div>
          <SheetFooter>
            <Button variant="outline" onClick={() => setIsUpgradeOpen(false)}>
              Cancel
            </Button>
            <Button onClick={submitUpgrade} disabled={isUpgrading}>
              Apply Upgrade
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>
      <Sheet open={isProductOpen} onOpenChange={setIsProductOpen}>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>Create Product</SheetTitle>
          </SheetHeader>
          <div className="p-4 space-y-3">
            <div className="space-y-1">
              <Label>Product Code</Label>
              <Input
                value={productForm.productCode}
                onChange={(e) =>
                  setProductForm((s) => ({
                    ...s,
                    productCode: e.target.value.toUpperCase(),
                  }))
                }
              />
            </div>
            <div className="space-y-1">
              <Label>Product Name</Label>
              <Input
                value={productForm.productName}
                onChange={(e) =>
                  setProductForm((s) => ({ ...s, productName: e.target.value }))
                }
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Min Interest Rate (%)</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={productForm.minInterestRate}
                  onChange={(e) =>
                    setProductForm((s) => ({
                      ...s,
                      minInterestRate: e.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-1">
                <Label>Max Interest Rate (%)</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={productForm.maxInterestRate}
                  onChange={(e) =>
                    setProductForm((s) => ({
                      ...s,
                      maxInterestRate: e.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-1">
                <Label>Min Term (Months)</Label>
                <Input
                  type="number"
                  value={productForm.minTermMonths}
                  onChange={(e) =>
                    setProductForm((s) => ({
                      ...s,
                      minTermMonths: e.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-1">
                <Label>Max Term (Months)</Label>
                <Input
                  type="number"
                  value={productForm.maxTermMonths}
                  onChange={(e) =>
                    setProductForm((s) => ({
                      ...s,
                      maxTermMonths: e.target.value,
                    }))
                  }
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label>Min Amount</Label>
                <Input
                  type="number"
                  value={productForm.minAmount}
                  onChange={(e) =>
                    setProductForm((s) => ({ ...s, minAmount: e.target.value }))
                  }
                />
              </div>
              <div className="space-y-1">
                <Label>Max Amount</Label>
                <Input
                  type="number"
                  value={productForm.maxAmount}
                  onChange={(e) =>
                    setProductForm((s) => ({ ...s, maxAmount: e.target.value }))
                  }
                />
              </div>
            </div>
            <div className="space-y-1">
              <Label>Compounding Frequency</Label>
              <Select
                value={productForm.compoundingFrequency}
                onValueChange={(v) =>
                  setProductForm((s) => ({ ...s, compoundingFrequency: v }))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select frequency" />
                </SelectTrigger>
                <SelectContent>
                  {[
                    "ANNUAL",
                    "SEMI_ANNUAL",
                    "QUARTERLY",
                    "MONTHLY",
                    "DAILY",
                    "SIMPLE",
                  ].map((opt) => (
                    <SelectItem key={opt} value={opt}>
                      {opt.replace("_", " ")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label>Effective Date</Label>
              <Input
                type="date"
                value={productForm.effectiveDate}
                onChange={(e) =>
                  setProductForm((s) => ({
                    ...s,
                    effectiveDate: e.target.value,
                  }))
                }
              />
            </div>
          </div>
          <SheetFooter>
            <Button variant="outline" onClick={() => setIsProductOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={async () => {
                const minR = Number(productForm.minInterestRate);
                const maxR = Number(productForm.maxInterestRate);
                const minT = Number(productForm.minTermMonths);
                const maxT = Number(productForm.maxTermMonths);
                if (minR > maxR || minT > maxT) {
                  toast.error("Minimums must be ≤ maximums");
                  return;
                }
                setIsSavingProduct(true);
                try {
                  const payload = {
                    productCode: productForm.productCode,
                    productName: productForm.productName,
                    productType: "FIXED_DEPOSIT",
                    description: "",
                    minInterestRate: minR,
                    maxInterestRate: maxR,
                    minTermMonths: minT,
                    maxTermMonths: maxT,
                    minAmount: Number(productForm.minAmount),
                    maxAmount: Number(productForm.maxAmount),
                    currency: "INR",
                    compoundingFrequency: productForm.compoundingFrequency,
                    effectiveDate: productForm.effectiveDate,
                    requiresApproval: false,
                  } as any;
                  const res = await api.post("/api/v1/product", payload);
                  const created = res?.data;
                  const newList = await refreshProducts();
                  const newProd = newList.find(
                    (p: (typeof newList)[number]) =>
                      p.code === (created?.productCode || payload.productCode)
                  );
                  if (newProd) setSelectedProductId(newProd.id);
                  setIsProductOpen(false);
                  toast.success("Product created");
                } catch (e: any) {
                  const msg =
                    e?.response?.data?.message || "Failed to create product";
                  toast.error(String(msg));
                } finally {
                  setIsSavingProduct(false);
                }
              }}
              disabled={isSavingProduct}
            >
              Create
            </Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>
    </div>
  );
}

export default AdminDashboard;
