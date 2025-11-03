import { useState, useEffect } from "react";
import { Link } from "react-router";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ChevronLeft, ChevronRight, RefreshCw, Search, X } from "lucide-react";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { useI18n } from "@/context/I18nContext";
import { useAuth } from "@/context/AuthContext";

interface Account {
  accountNo: string;
  customerId: string;
  productCode: string;
  principalAmount: number;
  interestRate: number;
  tenureMonths: number;
  maturityAmount: number;
  maturityDate: string;
  branchCode: string;
  status: "ACTIVE" | "CLOSED" | "MATURED" | "SUSPENDED";
  createdAt: string;
}

interface PagedResponse {
  content: Account[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export function AdminAccountsList() {
  const { t } = useI18n();
  const { user } = useAuth();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [filters, setFilters] = useState({
    customerId: "",
    productCode: "",
    status: "all",
    branchCode: "",
  });

  const [appliedFilters, setAppliedFilters] = useState(filters);

  useEffect(() => {
    if (user?.role !== "ADMIN" && user?.role !== "BANKOFFICER") {
      return;
    }
    fetchAccounts();
  }, [page, size, appliedFilters, user]);

  const fetchAccounts = async () => {
    try {
      setIsLoading(true);
      const response = await api.post("/api/accounts/search", {
        customerId: appliedFilters.customerId || null,
        productCode: appliedFilters.productCode || null,
        status:
          appliedFilters.status === "all"
            ? null
            : appliedFilters.status || null,
        branchCode: appliedFilters.branchCode || null,
        page,
        size,
        sortBy: "createdAt",
        sortDirection: "DESC",
      });

      const data: PagedResponse = response.data.data;
      setAccounts(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (error) {
      console.error("Failed to fetch accounts:", error);
      toast.error("Failed to load accounts");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = () => {
    setAppliedFilters(filters);
    setPage(0);
  };

  const handleReset = () => {
    setFilters({
      customerId: "",
      productCode: "",
      status: "all",
      branchCode: "",
    });
    setAppliedFilters({
      customerId: "",
      productCode: "",
      status: "all",
      branchCode: "",
    });
    setPage(0);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return "bg-green-500/10 text-green-500 hover:bg-green-500/20";
      case "CLOSED":
        return "bg-gray-500/10 text-gray-500 hover:bg-gray-500/20";
      case "MATURED":
        return "bg-blue-500/10 text-blue-500 hover:bg-blue-500/20";
      case "SUSPENDED":
        return "bg-red-500/10 text-red-500 hover:bg-red-500/20";
      default:
        return "";
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">
          {t("admin.accounts.title")}
        </h1>
        <p className="text-muted-foreground mt-2">
          {t("admin.accounts.description")}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{t("admin.accounts.filters")}</CardTitle>
          <CardDescription>
            {t("admin.accounts.filtersDescription")}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label htmlFor="customerId">
                {t("admin.accounts.customerId")}
              </Label>
              <Input
                id="customerId"
                placeholder={t("admin.accounts.customerIdPlaceholder")}
                value={filters.customerId}
                onChange={(e) =>
                  setFilters({ ...filters, customerId: e.target.value })
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="productCode">
                {t("admin.accounts.productCode")}
              </Label>
              <Input
                id="productCode"
                placeholder={t("admin.accounts.productCodePlaceholder")}
                value={filters.productCode}
                onChange={(e) =>
                  setFilters({ ...filters, productCode: e.target.value })
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">{t("admin.accounts.status")}</Label>
              <Select
                value={filters.status}
                onValueChange={(value) =>
                  setFilters({ ...filters, status: value })
                }
              >
                <SelectTrigger id="status">
                  <SelectValue
                    placeholder={t("admin.accounts.statusPlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">
                    {t("admin.accounts.allStatus")}
                  </SelectItem>
                  <SelectItem value="ACTIVE">
                    {t("admin.accounts.active")}
                  </SelectItem>
                  <SelectItem value="CLOSED">
                    {t("admin.accounts.closed")}
                  </SelectItem>
                  <SelectItem value="MATURED">
                    {t("admin.accounts.matured")}
                  </SelectItem>
                  <SelectItem value="SUSPENDED">
                    {t("admin.accounts.suspended")}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="branchCode">
                {t("admin.accounts.branchCode")}
              </Label>
              <Input
                id="branchCode"
                placeholder={t("admin.accounts.branchCodePlaceholder")}
                value={filters.branchCode}
                onChange={(e) =>
                  setFilters({ ...filters, branchCode: e.target.value })
                }
              />
            </div>
          </div>

          <div className="flex gap-2 mt-4">
            <Button onClick={handleSearch} disabled={isLoading}>
              <Search className="h-4 w-4" />
              {t("action.search")}
            </Button>
            <Button
              variant="outline"
              onClick={handleReset}
              disabled={isLoading}
            >
              <X className="h-4 w-4" />
              {t("action.reset")}
            </Button>
            <Button
              variant="outline"
              onClick={fetchAccounts}
              disabled={isLoading}
            >
              <RefreshCw className="h-4 w-4" />
              {t("action.refresh")}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>{t("admin.accounts.list")}</CardTitle>
              <CardDescription>
                {!isLoading ? (
                  `${t("admin.accounts.showing")
                    .replace(
                      "{{from}}",
                      (totalElements > 0 ? page * size + 1 : 0).toString()
                    )
                    .replace(
                      "{{to}}",
                      Math.min((page + 1) * size, totalElements).toString()
                    )
                    .replace("{{total}}", totalElements.toString())}`
                ) : (
                  <Skeleton className="h-4 w-48" />
                )}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Label htmlFor="pageSize">{t("admin.accounts.pageSize")}</Label>
              <Select
                value={size.toString()}
                onValueChange={(value) => {
                  setSize(parseInt(value));
                  setPage(0);
                }}
              >
                <SelectTrigger id="pageSize" className="w-20">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="10">10</SelectItem>
                  <SelectItem value="25">25</SelectItem>
                  <SelectItem value="50">50</SelectItem>
                  <SelectItem value="100">100</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("admin.accounts.accountNo")}</TableHead>
                  <TableHead>{t("admin.accounts.customerId")}</TableHead>
                  <TableHead>{t("admin.accounts.productCode")}</TableHead>
                  <TableHead>{t("admin.accounts.principal")}</TableHead>
                  <TableHead>{t("admin.accounts.interestRate")}</TableHead>
                  <TableHead>{t("admin.accounts.tenure")}</TableHead>
                  <TableHead>{t("admin.accounts.branchCode")}</TableHead>
                  <TableHead>{t("admin.accounts.status")}</TableHead>
                  <TableHead className="text-right">
                    {t("admin.accounts.actions")}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  [...Array(size)].map((_, i) => (
                    <TableRow key={i}>
                      <TableCell>
                        <Skeleton className="h-4 w-32" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-4 w-24" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-4 w-20" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-4 w-24" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-4 w-16" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-4 w-16" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-4 w-20" />
                      </TableCell>
                      <TableCell>
                        <Skeleton className="h-5 w-20" />
                      </TableCell>
                      <TableCell className="text-right">
                        <Skeleton className="h-8 w-8 ml-auto" />
                      </TableCell>
                    </TableRow>
                  ))
                ) : accounts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} className="text-center py-8">
                      {t("admin.accounts.noAccounts")}
                    </TableCell>
                  </TableRow>
                ) : (
                  accounts.map((account) => (
                    <TableRow key={account.accountNo}>
                      <TableCell className="font-medium">
                        {account.accountNo}
                      </TableCell>
                      <TableCell>{account.customerId}</TableCell>
                      <TableCell>{account.productCode}</TableCell>
                      <TableCell>
                        {account.principalAmount.toLocaleString()}
                      </TableCell>
                      <TableCell>{account.interestRate}%</TableCell>
                      <TableCell>{account.tenureMonths}mo</TableCell>
                      <TableCell>{account.branchCode}</TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(account.status)}>
                          {account.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <Link to={`/accounts/${account.accountNo}`}>
                          <Button variant="outline" size="sm">
                            {t("action.details")}
                          </Button>
                        </Link>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-muted-foreground">
                {`${t("admin.accounts.page")
                  .replace("{{current}}", (page + 1).toString())
                  .replace("{{total}}", totalPages.toString())}`}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0 || isLoading}
                >
                  <ChevronLeft className="h-4 w-4" />
                  {t("action.previous")}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1 || isLoading}
                >
                  {t("action.next")}
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
