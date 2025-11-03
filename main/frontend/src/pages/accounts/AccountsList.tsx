import { useState, useEffect } from "react";
import { Link } from "react-router";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  CreditCard,
  Search,
  Filter,
  TrendingUp,
  Calendar,
  DollarSign,
  Eye,
  MoreHorizontal,
  Download,
  RefreshCw,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { useI18n } from "@/context/I18nContext";
import { useStablecoinConversion } from "@/hooks/useStablecoinConversion";

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
}

export function AccountsList() {
  const { user } = useAuth();
  const { t } = useI18n();
  const {
    preferredCurrency,
    formatTokens: formatTokenAmount,
    formatConvertedTokens,
  } = useStablecoinConversion();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [customerId, setCustomerId] = useState<string | null>(null);

  const isAdmin = user?.role === "ADMIN" || user?.role === "BANKOFFICER";

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

  const reloadAccounts = async () => {
    try {
      setIsLoading(true);
      const id = customerId;
      if (!id) throw new Error("Missing customer id");
      const response = await api.get(`/api/accounts/customer/${id}`);
      const payload =
        response?.data && Array.isArray(response.data.data)
          ? response.data.data
          : response.data;
      const mapped: Account[] = (Array.isArray(payload) ? payload : []).map(
        (a: any) => ({
          id: String(a.id ?? ""),
          accountNumber: String(a.accountNo ?? a.accountNumber ?? ""),
          accountType: "FIXED_DEPOSIT",
          balance: Number(
            a.currentBalance ?? a.balance ?? a.maturityAmount ?? 0
          ),
          interestRate: Number(a.interestRate ?? 0),
          maturityDate: String(a.maturityDate ?? new Date().toISOString()),
          status: String(a.status ?? "ACTIVE") as any,
          createdAt: String(a.createdAt ?? new Date().toISOString()),
          productName: String(a.productName ?? a.productCode ?? "Product"),
          principalAmount: Number(a.principalAmount ?? 0),
        })
      );
      setAccounts(mapped);
    } catch (error) {
      console.error("Failed to fetch accounts:", error);
      toast.error("Failed to load accounts");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await api.get("/api/customer/profile");
        if (res?.data?.id) setCustomerId(String(res.data.id));
      } catch (e) {
        // leave as null; reload will show error
      }
    };
    fetchProfile();
  }, []);

  useEffect(() => {
    if (customerId) reloadAccounts();
  }, [customerId]);

  useEffect(() => {
    if (!customerId) return;
    const id = setInterval(() => {
      reloadAccounts();
    }, 10000);
    const onVis = () => {
      if (!document.hidden) reloadAccounts();
    };
    document.addEventListener("visibilitychange", onVis);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", onVis);
    };
  }, [customerId]);

  const filteredAccounts = accounts.filter((account) => {
    const acctNum = account.accountNumber || "";
    const prod = (account.productName || "").toLowerCase();
    const q = searchTerm.toLowerCase();
    const matchesSearch = acctNum.includes(searchTerm) || prod.includes(q);
    const matchesStatus =
      statusFilter === "all" || account.status === statusFilter;
    const matchesType =
      typeFilter === "all" || account.accountType === typeFilter;
    return matchesSearch && matchesStatus && matchesType;
  });

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return "default";
      case "MATURED":
        return "secondary";
      case "CLOSED":
        return "destructive";
      default:
        return "outline";
    }
  };

  const getAccountTypeColor = () => {
    return "bg-emerald-50 text-emerald-700";
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              {isAdmin ? t("accounts.all") : t("accounts.mine")}
            </h1>
            <p className="text-muted-foreground">
              {isAdmin
                ? t("accounts.subtitle.admin")
                : t("accounts.subtitle.customer")}
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" disabled>
              <RefreshCw className="h-4 w-4" />
              {t("action.refresh")}
            </Button>
            {!isAdmin && (
              <Button disabled>
                <Download className="h-4 w-4" />
                {t("action.exportCsv")}
              </Button>
            )}
          </div>
        </div>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-1 items-center gap-2">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t("accounts.searchPlaceholder")}
                disabled
                className="pl-9"
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-32" disabled>
                <Filter className="mr-2 h-4 w-4" />
                <SelectValue placeholder={t("accounts.filter.status")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">
                  {t("accounts.filter.allStatus")}
                </SelectItem>
              </SelectContent>
            </Select>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger className="w-40" disabled>
                <SelectValue placeholder={t("accounts.filter.type")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">
                  {t("accounts.filter.allTypes")}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(6)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="space-y-2 flex-1">
                    <div className="flex items-center gap-2">
                      <Skeleton className="h-10 w-10 rounded-lg" />
                      <Skeleton className="h-6 w-32" />
                    </div>
                    <Skeleton className="h-4 w-48" />
                  </div>
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-6 w-16" />
                    <Skeleton className="h-8 w-8" />
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-28" />
                    <Skeleton className="h-6 w-24" />
                    <Skeleton className="h-3 w-20" />
                  </div>
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-6 w-16" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-4 w-28" />
                </div>
                <div className="space-y-2">
                  <Skeleton className="h-4 w-28" />
                  <Skeleton className="h-4 w-24" />
                </div>
                <div className="pt-2">
                  <Skeleton className="h-10 w-full" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {isAdmin ? t("accounts.all") : t("accounts.mine")}
          </h1>
          <p className="text-muted-foreground">
            {isAdmin
              ? t("accounts.subtitle.admin")
              : t("accounts.subtitle.customer")}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={reloadAccounts}>
            <RefreshCw className="h-4 w-4" />
            {t("action.refresh")}
          </Button>
          {!isAdmin && (
            <Button onClick={exportCsv} disabled={accounts.length === 0}>
              <Download className="h-4 w-4" />
              {t("action.exportCsv")}
            </Button>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t("accounts.searchPlaceholder")}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-32">
              <Filter className="h-4 w-4" />
              <SelectValue placeholder={t("accounts.filter.status")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">
                {t("accounts.filter.allStatus")}
              </SelectItem>
              <SelectItem value="ACTIVE">{t("status.active")}</SelectItem>
              <SelectItem value="MATURED">{t("status.matured")}</SelectItem>
              <SelectItem value="CLOSED">{t("status.closed")}</SelectItem>
            </SelectContent>
          </Select>
          <Select value={typeFilter} onValueChange={setTypeFilter}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder={t("accounts.filter.type")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">
                {t("accounts.filter.allTypes")}
              </SelectItem>
              <SelectItem value="FIXED_DEPOSIT">{t("type.fd")}</SelectItem>
              <SelectItem value="RECURRING_DEPOSIT">{t("type.rd")}</SelectItem>
              <SelectItem value="SAVINGS">{t("type.savings")}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {filteredAccounts.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">
              {t("accounts.empty.title")}
            </h3>
            <p className="text-muted-foreground text-center">
              {searchTerm || statusFilter !== "all" || typeFilter !== "all"
                ? t("accounts.empty.adjustFilters")
                : isAdmin
                ? t("accounts.empty.noneAdmin")
                : t("accounts.empty.noneCustomer")}
            </p>
            {!isAdmin && accounts.length > 0 && (
              <Button className="mt-4" onClick={exportCsv}>
                <Download className="h-4 w-4" />
                {t("action.exportCsv")}
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
                      <div
                        className={`rounded-lg p-2 ${getAccountTypeColor()}`}
                      >
                        <CreditCard className="h-4 w-4" />
                      </div>
                      {account.productName}
                    </CardTitle>
                    <CardDescription>
                      {t("accounts.card.account")}: {account.accountNumber}
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
                      {t("accounts.card.currentBalance")}
                    </div>
                    <div className="flex flex-col text-xl font-bold">
                      <span>{formatTokenAmount(account.balance)}</span>
                      <span className="text-sm text-muted-foreground">
                        {formatConvertedTokens(
                          account.balance,
                          preferredCurrency
                        )}
                      </span>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <TrendingUp className="h-4 w-4" />
                      {t("accounts.card.interestRate")}
                    </div>
                    <p className="text-lg font-semibold text-green-600">
                      {account.interestRate}% p.a.
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <DollarSign className="h-4 w-4" />
                    {t("accounts.card.principal")}
                  </div>
                  <div className="flex flex-col text-sm">
                    <span>{formatTokenAmount(account.principalAmount)}</span>
                    <span className="text-xs text-muted-foreground">
                      {formatConvertedTokens(
                        account.principalAmount,
                        preferredCurrency
                      )}
                    </span>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-4 w-4" />
                    {t("accounts.card.maturityDate")}
                  </div>
                  <p className="text-sm">
                    {new Date(account.maturityDate).toLocaleDateString()}
                  </p>
                </div>

                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Calendar className="h-4 w-4" />
                  {t("accounts.card.opened")}:{" "}
                  {new Date(account.createdAt).toLocaleDateString()}
                </div>

                <div className="pt-2">
                  <Link to={`/accounts/${account.accountNumber}`}>
                    <Button className="w-full" variant="outline">
                      <Eye className="h-4 w-4" />
                      {t("action.viewDetails")}
                    </Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
