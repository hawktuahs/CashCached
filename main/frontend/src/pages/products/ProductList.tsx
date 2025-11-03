import { useState, useEffect } from "react";
import { Link, useLocation } from "react-router";
import { useI18n } from "@/context/I18nContext";
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
  Package,
  Plus,
  Search,
  Filter,
  TrendingUp,
  Clock,
  DollarSign,
  Edit,
  Trash2,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api";
import { toast } from "sonner";

interface Product {
  id: string;
  code: string;
  name: string;
  description: string;
  interestRate: number;
  minAmount: number;
  maxAmount: number;
  tenure: number;
  category: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export function ProductList() {
  const { t } = useI18n();
  const { user } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [sortBy, setSortBy] = useState("name");
  const location = useLocation();

  const isAdmin = user?.role === "ADMIN" || user?.role === "BANKOFFICER";

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await api.get("/api/v1/product");
        const root = response?.data;
        const items = Array.isArray(root?.data)
          ? root.data
          : Array.isArray(root)
          ? root
          : [];
        const mapped: Product[] = items.map((p: any) => ({
          id: String(p.id ?? p.productCode ?? crypto.randomUUID()),
          code: String(p.productCode ?? ""),
          name: String(p.productName ?? p.name ?? ""),
          description: String(p.description ?? ""),
          interestRate:
            p.minInterestRate != null && p.maxInterestRate != null
              ? (Number(p.minInterestRate) + Number(p.maxInterestRate)) / 2
              : Number(
                  p.maxInterestRate ?? p.minInterestRate ?? p.interestRate ?? 0
                ),
          minAmount: Number(p.minAmount ?? 0),
          maxAmount: Number(p.maxAmount ?? 0),
          tenure: Math.max(
            1,
            Math.round(
              ((p.maxTermMonths ?? p.minTermMonths ?? 12) as number) / 12
            )
          ),
          category: String(p.productType ?? p.category ?? ""),
          isActive: String(p.status ?? "ACTIVE") === "ACTIVE",
          createdAt: String(p.createdAt ?? new Date().toISOString()),
          updatedAt: String(p.updatedAt ?? new Date().toISOString()),
        }));
        setProducts(mapped);
      } catch (error) {
        console.error("Failed to fetch products:", error);
        toast.error(t("products.toast.loadFailed"));
      } finally {
        setIsLoading(false);
      }
    };

    fetchProducts();
  }, [location.key]);

  const handleDeleteProduct = async (productId: string) => {
    if (!confirm(t("products.confirm.delete"))) return;

    try {
      await api.delete(`/api/v1/product/${productId}`);
      setProducts(products.filter((p) => p.id !== productId));
      toast.success(t("products.toast.deleted"));
    } catch (error) {
      toast.error(t("products.toast.deleteFailed"));
    }
  };

  const filteredProducts = products
    .filter((product) => {
      const name = (product.name || "").toLowerCase();
      const desc = (product.description || "").toLowerCase();
      const matchesSearch =
        name.includes(searchTerm.toLowerCase()) ||
        desc.includes(searchTerm.toLowerCase());
      const matchesCategory =
        categoryFilter === "all" || (product.category || "") === categoryFilter;
      return matchesSearch && matchesCategory;
    })
    .sort((a, b) => {
      switch (sortBy) {
        case "name":
          return a.name.localeCompare(b.name);
        case "rate":
          return b.interestRate - a.interestRate;
        case "amount":
          return a.minAmount - b.minAmount;
        case "tenure":
          return a.tenure - b.tenure;
        default:
          return 0;
      }
    });

  const categories = Array.from(new Set(products.map((p) => p.category)));

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 0,
    }).format(amount);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">
              {t("products.title")}
            </h1>
            <p className="text-muted-foreground">&nbsp;</p>
          </div>
          {isAdmin && (
            <Link to="/products/new">
              <Button>
                <Plus className="h-4 w-4" />
                {t("products.addProduct")}
              </Button>
            </Link>
          )}
        </div>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex flex-1 items-center gap-2">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder={t("common.search")}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9"
              />
            </div>
            <Select value={categoryFilter} onValueChange={setCategoryFilter}>
              <SelectTrigger className="w-40">
                <Filter className="h-4 w-4" />
                <SelectValue placeholder={t("common.category")} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t("common.allCategories")}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder={t("common.sortBy")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">{t("products.sort.name")}</SelectItem>
              <SelectItem value="rate">{t("products.sort.rate")}</SelectItem>
              <SelectItem value="amount">
                {t("products.sort.amount")}
              </SelectItem>
              <SelectItem value="tenure">
                {t("products.sort.tenure")}
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {[...Array(6)].map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="space-y-2 flex-1">
                    <Skeleton className="h-6 w-3/4" />
                    <Skeleton className="h-4 w-full" />
                    <Skeleton className="h-4 w-2/3" />
                  </div>
                  <Skeleton className="h-6 w-16" />
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-6 w-20" />
                  </div>
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-6 w-20" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-4 w-full" />
                </div>
                <Skeleton className="h-4 w-24" />
                <div className="pt-2 grid grid-cols-2 gap-2">
                  <Skeleton className="h-10 w-full" />
                  {isAdmin && <Skeleton className="h-10 w-full" />}
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
            {t("products.title")}
          </h1>
          <p className="text-muted-foreground">&nbsp;</p>
        </div>
        {isAdmin && (
          <Link to="/products/new">
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              {t("products.addProduct")}
            </Button>
          </Link>
        )}
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 items-center gap-2">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder={t("common.search")}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-9"
            />
          </div>
          <Select value={categoryFilter} onValueChange={setCategoryFilter}>
            <SelectTrigger className="w-40">
              <Filter className="h-4 w-4" />
              <SelectValue placeholder={t("common.category")} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t("common.allCategories")}</SelectItem>
              {categories.map((category) => (
                <SelectItem key={category} value={category}>
                  {category}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <Select value={sortBy} onValueChange={setSortBy}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder={t("common.sortBy")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="name">{t("products.sort.name")}</SelectItem>
            <SelectItem value="rate">{t("products.sort.rate")}</SelectItem>
            <SelectItem value="amount">{t("products.sort.amount")}</SelectItem>
            <SelectItem value="tenure">{t("products.sort.tenure")}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {filteredProducts.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Package className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">
              {t("products.empty.title")}
            </h3>
            <p className="text-muted-foreground text-center">
              {searchTerm || categoryFilter !== "all"
                ? t("products.empty.adjustFilters")
                : t("products.empty.none")}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {filteredProducts.map((product) => (
            <Card key={product.id} className="relative">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <CardTitle className="text-lg">{product.name}</CardTitle>
                    <CardDescription className="line-clamp-2">
                      {product.description}
                    </CardDescription>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={product.isActive ? "default" : "secondary"}>
                      {product.isActive
                        ? t("status.active")
                        : t("status.inactive")}
                    </Badge>
                    {isAdmin && (
                      <div className="flex gap-1">
                        <Link
                          to={`/products/${product.code || product.id}/edit`}
                        >
                          <Button variant="ghost" size="sm">
                            <Edit className="h-4 w-4" />
                          </Button>
                        </Link>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() =>
                            handleDeleteProduct(product.code || product.id)
                          }
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <TrendingUp className="h-4 w-4" />
                      {t("calculator.interestRate")}
                    </div>
                    <p className="text-xl font-bold text-green-600">
                      {product.interestRate}% p.a.
                    </p>
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Clock className="h-4 w-4" />
                      {t("calculator.result.tenure")}
                    </div>
                    <p className="text-lg font-semibold">
                      {product.tenure}{" "}
                      {product.tenure === 1
                        ? t("common.year")
                        : t("common.years")}
                    </p>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <DollarSign className="h-4 w-4" />
                    {t("products.investmentRange")}
                  </div>
                  <p className="text-sm">
                    {formatCurrency(product.minAmount)} -{" "}
                    {formatCurrency(product.maxAmount)}
                  </p>
                </div>

                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Package className="h-4 w-4" />
                  {product.category}
                </div>

                <div className="pt-2 grid grid-cols-2 gap-2">
                  <Link
                    to={`/fd-calculator?product=${encodeURIComponent(
                      product.code || product.id
                    )}`}
                  >
                    <Button className="w-full" variant="outline">
                      {t("products.calculate")}
                    </Button>
                  </Link>
                  {isAdmin && (
                    <Link to={`/products/${product.code || product.id}/edit`}>
                      <Button className="w-full" variant="secondary">
                        {t("products.edit")}
                      </Button>
                    </Link>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
