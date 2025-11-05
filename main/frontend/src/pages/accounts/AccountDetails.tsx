import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Input } from "@/components/ui/input";
import {
  CreditCard,
  ArrowLeft,
  Calendar,
  DollarSign,
  Download,
  Share2,
  Activity,
  FileText,
  Coins,
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
  currentBalance: number;
  accruedInterest: number;
  interestRate: number;
  baseInterestRate: number;
  maturityDate: string;
  status: "ACTIVE" | "MATURED" | "CLOSED";
  createdAt: string;
  productName: string;
  principalAmount: number;
  customerId: string;
  customerName: string;
  customerEmail: string;
  activePricingRuleId?: string;
  activePricingRuleName?: string;
  pricingRuleAppliedAt?: string;
}

interface Transaction {
  id: string;
  type: "DEPOSIT" | "WITHDRAWAL" | "INTEREST" | "MATURITY" | "PENALTY";
  tokens: number;
  convertedDisplay: string;
  description: string;
  timestamp: string;
  balanceTokens: number;
}

export function AccountDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useI18n();
  const [account, setAccount] = useState<Account | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingTransactions, setIsLoadingTransactions] = useState(false);
  const isAdmin = user?.role === "ADMIN" || user?.role === "BANKOFFICER";
  const [walletTokens, setWalletTokens] = useState<number>(0);
  const [isWalletLoading, setIsWalletLoading] = useState(false);
  const [depositTokens, setDepositTokens] = useState<string>("");
  const [withdrawTokens, setWithdrawTokens] = useState<string>("");
  const [isPosting, setIsPosting] = useState(false);
  const { preferredCurrency, formatTokens, formatConvertedTokens } =
    useStablecoinConversion();

  const parseTokens = (value: string) => {
    const normalized = value.trim();
    if (!normalized) return 0;
    const num = Number(normalized);
    if (!Number.isFinite(num)) return 0;
    return Math.floor(num);
  };

  const formatPercent = (value: number) => {
    const normalized = Number.isFinite(value) ? value : 0;
    return `${normalized.toFixed(2)}%`;
  };

  const fetchWalletBalance = async (customerIdParam: string) => {
    if (!customerIdParam) {
      setWalletTokens(0);
      return null;
    }
    setIsWalletLoading(true);
    try {
      const response = await api.get(
        `/api/financials/stablecoin/balance/${customerIdParam}`
      );
      const payload = response?.data?.data ?? response?.data;
      const rawBalance = Number(payload?.balance ?? 0);
      const tokens = Number.isFinite(rawBalance) ? rawBalance : 0;
      const rawTarget = Number(payload?.targetValue ?? rawBalance);
      const targetValue = Number.isFinite(rawTarget) ? rawTarget : tokens;
      const currency = String(
        payload?.targetCurrency ??
          payload?.baseCurrency ??
          preferredCurrency ??
          "KWD"
      );
      setWalletTokens(tokens);
      return {
        customerId: customerIdParam,
        balance: tokens,
        targetValue,
        currency,
      };
    } catch (error) {
      console.error("Failed to load wallet balance", error);
      toast.error("Unable to load CashCached wallet balance");
      setWalletTokens(0);
      return null;
    } finally {
      setIsWalletLoading(false);
    }
  };

  const loadAccount = async (
    accountId: string,
    options: { silent?: boolean } = {}
  ) => {
    if (!options.silent) {
      setIsLoading(true);
    }
    try {
      const response = await api.get(`/api/accounts/${accountId}`);
      const payload = response?.data?.data ?? response?.data;
      const a = payload || {};
      const mapped: Account = {
        id: String(a.id ?? ""),
        accountNumber: String(
          a.accountNo ?? a.accountNumber ?? accountId ?? ""
        ),
        accountType: String(a.accountType ?? "FIXED_DEPOSIT"),
        currentBalance: Number(a.currentBalance ?? a.balance ?? 0),
        accruedInterest: Number(a.accruedInterest ?? 0),
        interestRate: Number(a.interestRate ?? 0),
        baseInterestRate: Number(a.baseInterestRate ?? a.interestRate ?? 0),
        maturityDate: String(a.maturityDate ?? new Date().toISOString()),
        status: String(a.status ?? "ACTIVE") as any,
        createdAt: String(a.createdAt ?? new Date().toISOString()),
        productName: String(a.productName ?? a.productCode ?? "Product"),
        principalAmount: Number(a.principalAmount ?? 0),
        customerId: String(a.customerId ?? ""),
        customerName: String(a.customerName ?? ""),
        customerEmail: String(a.customerEmail ?? ""),
        activePricingRuleId: a.activePricingRuleId
          ? String(a.activePricingRuleId)
          : undefined,
        activePricingRuleName: a.activePricingRuleName
          ? String(a.activePricingRuleName)
          : undefined,
        pricingRuleAppliedAt: a.pricingRuleAppliedAt
          ? String(a.pricingRuleAppliedAt)
          : undefined,
      };
      setAccount(mapped);
      fetchWalletBalance(mapped.customerId);
    } catch (error) {
      console.error("Failed to fetch account details:", error);
      toast.error("Failed to load account details");
      navigate("/accounts");
    } finally {
      if (!options.silent) {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    if (id) {
      loadAccount(id);
    }
  }, [id]);

  const fetchTransactions = async () => {
    setIsLoadingTransactions(true);
    try {
      const response = await api.get(`/api/accounts/${id}/transactions`);
      const list = response?.data?.data ?? response?.data ?? [];
      const mapped = (Array.isArray(list) ? list : []).map((t: any) => {
        const tokens = Number(t.amount ?? 0);
        const balanceTokens = Number(t.balanceAfter ?? t.balance ?? 0);
        return {
          id: String(t.id ?? ""),
          type: ((): Transaction["type"] => {
            const v = String(t.transactionType ?? t.type ?? "").toUpperCase();
            if (v === "WITHDRAWAL") return "WITHDRAWAL";
            if (v === "INTEREST_CREDIT" || v === "INTEREST") return "INTEREST";
            if (v === "MATURITY_PAYOUT" || v === "MATURITY") return "MATURITY";
            if (v === "PENALTY_DEBIT" || v === "PENALTY") return "PENALTY";
            return "DEPOSIT";
          })(),
          tokens,
          convertedDisplay: formatConvertedTokens(tokens, preferredCurrency),
          description: String(t.description ?? ""),
          timestamp: String(
            t.createdAt ?? t.timestamp ?? new Date().toISOString()
          ),
          balanceTokens,
        } satisfies Transaction;
      });
      setTransactions(mapped);
    } catch (error) {
      console.error("Failed to fetch transactions:", error);
      toast.error("Failed to load transactions");
    } finally {
      setIsLoadingTransactions(false);
    }
  };

  useEffect(() => {
    if (id) fetchTransactions();
  }, [id]);

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

  const getTransactionTypeColor = (type: string) => {
    switch (type) {
      case "DEPOSIT":
      case "INTEREST":
        return "text-green-600";
      case "WITHDRAWAL":
      case "PENALTY":
        return "text-red-600";
      case "MATURITY":
        return "text-blue-600";
      default:
        return "text-gray-600";
    }
  };

  const getTransactionIcon = (type: string) => {
    switch (type) {
      case "DEPOSIT":
        return "↗";
      case "WITHDRAWAL":
        return "↙";
      case "INTEREST":
        return "💰";
      case "MATURITY":
        return "🎯";
      case "PENALTY":
        return "⚠";
      default:
        return "📄";
    }
  };

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
    );
  }

  if (!account) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate("/accounts")}
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Accounts
          </Button>
        </div>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <CreditCard className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">
              {t("details.notFound.title")}
            </h3>
            <p className="text-muted-foreground text-center">
              {t("details.notFound.desc")}
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate("/accounts")}
        >
          <ArrowLeft className="h-4 w-4" />
          {t("details.back")}
        </Button>
        <div className="flex-1">
          <h1 className="text-3xl font-bold tracking-tight">
            {t("details.title")}
          </h1>
          <p className="text-muted-foreground">
            {account.accountNumber} - {account.productName}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm">
            <Download className="h-4 w-4" />
            {t("action.download")}
          </Button>
          <Button variant="outline" size="sm">
            <Share2 className="h-4 w-4" />
            {t("action.share")}
          </Button>
        </div>
      </div>

      {user?.role === "CUSTOMER" && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Coins className="h-5 w-5" />
              CashCached Wallet
            </CardTitle>
            <CardDescription>
              Available tokens for this customer
            </CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-muted-foreground">
                Wallet Balance (CCHD)
              </p>
              <p className="text-2xl font-bold">
                {isWalletLoading
                  ? "Loading…"
                  : walletTokens.toLocaleString(undefined, {
                      maximumFractionDigits: 0,
                    })}
              </p>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">
                {preferredCurrency} Equivalent
              </p>
              <p className="text-xl font-semibold">
                {isWalletLoading
                  ? "—"
                  : formatConvertedTokens(walletTokens, preferredCurrency)}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {account.status === "ACTIVE" && (
        <Card className="border-primary/20">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <DollarSign className="h-5 w-5" />
              Redemption
            </CardTitle>
            <CardDescription>
              Redeem your Fixed Deposit (maturity or premature closure)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              variant="outline"
              className="w-full"
              onClick={() =>
                navigate(`/accounts/${id}/redemption`, {
                  state: { account },
                })
              }
            >
              View Redemption Details
            </Button>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview">
            {t("details.tabs.overview")}
          </TabsTrigger>
          <TabsTrigger value="transactions">
            {t("details.tabs.transactions")}
          </TabsTrigger>
          <TabsTrigger value="documents">
            {t("details.tabs.documents")}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-5 w-5" />
                {t("details.info")}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.accountNumber")}
                  </p>
                  <p className="text-lg font-semibold">
                    {account.accountNumber}
                  </p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.accountType")}
                  </p>
                  <p className="text-lg font-semibold">
                    {(account.accountType || "").replace("_", " ") ||
                      "FIXED DEPOSIT"}
                  </p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.productName")}
                  </p>
                  <p className="text-lg font-semibold">{account.productName}</p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.status")}
                  </p>
                  <Badge variant={getStatusBadgeVariant(account.status)}>
                    {account.status}
                  </Badge>
                </div>
              </div>
              {user?.role === "CUSTOMER" && account.status === "ACTIVE" && (
                <div className="grid grid-cols-2 gap-4 pt-2">
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">
                      Deposit (tokens)
                    </p>
                    <div className="flex gap-2">
                      <Input
                        type="number"
                        min="1"
                        step="1"
                        placeholder="10"
                        value={depositTokens}
                        onChange={(e) => setDepositTokens(e.target.value)}
                      />
                      <Button
                        size="sm"
                        disabled={
                          isPosting ||
                          walletTokens <= 0 ||
                          parseTokens(depositTokens) <= 0 ||
                          parseTokens(depositTokens) > walletTokens
                        }
                        onClick={async () => {
                          const tokens = parseTokens(depositTokens);
                          if (!tokens || !id) return;
                          setIsPosting(true);
                          try {
                            await api.post(
                              `/api/accounts/${id}/wallet/deposit`,
                              {
                                accountNo: account.accountNumber,
                                amount: tokens,
                                description: "CashCached deposit",
                                reference: `wallet-${Date.now()}`,
                              },
                              {
                                headers: {
                                  "X-User-Id": String(
                                    account.customerId || user?.id || ""
                                  ),
                                },
                              }
                            );
                            toast.success("Deposit recorded");
                            setDepositTokens("");
                            const snapshot = await fetchWalletBalance(
                              account.customerId
                            );
                            if (snapshot) {
                              window.dispatchEvent(
                                new CustomEvent("cashcached:refresh-wallet", {
                                  detail: snapshot,
                                })
                              );
                            }
                            fetchTransactions();
                            loadAccount(id, { silent: true });
                          } catch (error: any) {
                            toast.error(
                              error?.response?.data?.message || "Deposit failed"
                            );
                          } finally {
                            setIsPosting(false);
                          }
                        }}
                      >
                        Deposit
                      </Button>
                    </div>
                  </div>
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-muted-foreground">
                      Withdraw (tokens)
                    </p>
                    <div className="flex gap-2">
                      <Input
                        type="number"
                        min="1"
                        step="1"
                        placeholder="5"
                        value={withdrawTokens}
                        onChange={(e) => setWithdrawTokens(e.target.value)}
                      />
                      <Button
                        size="sm"
                        variant="destructive"
                        disabled={isPosting || parseTokens(withdrawTokens) <= 0}
                        onClick={async () => {
                          const tokens = parseTokens(withdrawTokens);
                          if (!tokens || !id) return;
                          setIsPosting(true);
                          try {
                            await api.post(
                              `/api/accounts/${id}/wallet/withdraw`,
                              {
                                accountNo: account.accountNumber,
                                amount: tokens,
                                description: "CashCached withdrawal",
                                reference: `wallet-${Date.now()}`,
                              },
                              {
                                headers: {
                                  "X-User-Id": String(
                                    account.customerId || user?.id || ""
                                  ),
                                },
                              }
                            );
                            toast.success("Withdrawal recorded");
                            setWithdrawTokens("");
                            const snapshot = await fetchWalletBalance(
                              account.customerId
                            );
                            if (snapshot) {
                              window.dispatchEvent(
                                new CustomEvent("cashcached:refresh-wallet", {
                                  detail: snapshot,
                                })
                              );
                            }
                            fetchTransactions();
                            loadAccount(id, { silent: true });
                          } catch (error: any) {
                            toast.error(
                              error?.response?.data?.message ||
                                "Withdrawal failed"
                            );
                          } finally {
                            setIsPosting(false);
                          }
                        }}
                      >
                        Withdraw
                      </Button>
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
                {t("details.financial")}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.principal")}
                  </p>
                  <div className="flex flex-col">
                    <p className="text-xl font-semibold">
                      {formatTokens(account.principalAmount)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatConvertedTokens(
                        account.principalAmount,
                        preferredCurrency
                      )}
                    </p>
                  </div>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.interestRate")}
                  </p>
                  <p className="text-xl font-semibold text-green-600">
                    {account.interestRate}% p.a.
                  </p>
                </div>
                <div className="space-y-2">
                  <p className="text-sm font-medium text-muted-foreground">
                    {t("details.interestEarned")}
                  </p>
                  <p className="text-xl font-semibold text-green-600">
                    {formatTokens(account.accruedInterest)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {formatConvertedTokens(
                      account.accruedInterest,
                      preferredCurrency
                    )}
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
                {t("details.txn.title")}
              </CardTitle>
              <CardDescription>{t("details.txn.subtitle")}</CardDescription>
            </CardHeader>
            <CardContent>
              <Button
                onClick={fetchTransactions}
                disabled={isLoadingTransactions}
                className="mb-4"
              >
                {isLoadingTransactions ? "Loading..." : t("details.txn.load")}
              </Button>

              {transactions.length === 0 ? (
                <div className="text-center py-8">
                  <Activity className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">
                    {t("details.txn.empty")}
                  </p>
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
                        <p
                          className={`font-semibold ${getTransactionTypeColor(
                            transaction.type
                          )}`}
                        >
                          {transaction.tokens >= 0 ? "+" : "-"}
                          {Math.abs(transaction.tokens).toLocaleString(
                            undefined,
                            {
                              maximumFractionDigits: 0,
                            }
                          )}{" "}
                          CCHD
                        </p>
                        <p className="text-sm text-muted-foreground">
                          {transaction.convertedDisplay}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          Account balance after:{" "}
                          {transaction.balanceTokens.toLocaleString(undefined, {
                            maximumFractionDigits: 0,
                          })}{" "}
                          CCHD
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
              {t("details.dates")}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                {t("details.opened")}
              </p>
              <p className="text-sm">
                {new Date(account.createdAt).toLocaleDateString()}
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                {t("details.maturityDate")}
              </p>
              <p className="text-sm">
                {new Date(account.maturityDate).toLocaleDateString()}
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm font-medium text-muted-foreground">
                {t("details.daysToMaturity")}
              </p>
              <p className="text-sm">
                {Math.ceil(
                  (new Date(account.maturityDate).getTime() -
                    new Date().getTime()) /
                    (1000 * 60 * 60 * 24)
                )}{" "}
                days
              </p>
            </div>
          </CardContent>
        </Card>

        {isAdmin && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CreditCard className="h-5 w-5" />
                {t("details.customerInfo")}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">
                  Customer Name
                </p>
                <p className="text-sm">{account.customerName}</p>
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium text-muted-foreground">
                  Email
                </p>
                <p className="text-sm">{account.customerEmail}</p>
              </div>
              <div className="space-y-1">
                <div className="text-sm text-muted-foreground">
                  Interest Rate
                </div>
                <div className="text-2xl font-semibold">
                  {formatPercent(account.interestRate)}
                </div>
                {account.baseInterestRate !== undefined &&
                  account.baseInterestRate !== account.interestRate && (
                    <div className="text-xs text-muted-foreground">
                      Base {formatPercent(account.baseInterestRate)}
                    </div>
                  )}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
