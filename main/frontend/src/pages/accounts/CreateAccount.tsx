import { useState, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Loader2, Info, CheckCircle2, AlertCircle } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { useI18n } from "@/context/I18nContext";

interface Product {
  id: number;
  productCode: string;
  productName: string;
  minAmount: number;
  maxAmount: number;
  minTermMonths: number;
  maxTermMonths: number;
  minInterestRate: number;
  maxInterestRate: number;
  status: "ACTIVE" | "INACTIVE" | "SUSPENDED" | "DISCONTINUED";
}

interface Customer {
  id: string;
  name: string;
  email: string;
  role: string;
}

interface AccountCreationResponse {
  success: boolean;
  message: string;
  data: {
    accountNo: string;
    customerId: string;
    productCode: string;
    principalAmount: number;
    interestRate: number;
    tenureMonths: number;
    maturityAmount: number;
    branchCode: string;
    status: string;
  };
}

export function CreateAccount() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useI18n();
  const [activeTab, setActiveTab] = useState("v1");
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [_, setSelectedCustomer] = useState<Customer | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingProducts, setIsLoadingProducts] = useState(true);
  const [isLoadingCustomers, setIsLoadingCustomers] = useState(true);

  const isStaff = user?.role === "ADMIN" || user?.role === "BANKOFFICER";

  const [formDataV1, setFormDataV1] = useState({
    customerId: "",
    productCode: "",
    principalAmount: "",
    branchCode: "BR001",
    remarks: "",
  });

  const [formDataV2, setFormDataV2] = useState({
    customerId: "",
    productCode: "",
    principalAmount: "",
    customInterestRate: "",
    customTenureMonths: "",
    branchCode: "BR001",
    remarks: "",
  });

  useEffect(() => {
    if (!isStaff) {
      navigate("/accounts");
      toast.error("Only staff members can open accounts for customers");
      return;
    }
    fetchProducts();
    fetchCustomers();
  }, [isStaff, navigate]);

  const fetchProducts = async () => {
    try {
      setIsLoadingProducts(true);
      const response = await api.get("/api/v1/product");
      const activeProducts = response.data.data.filter(
        (p: Product) => p.status === "ACTIVE"
      );
      setProducts(activeProducts);
    } catch (error) {
      console.error("Failed to fetch products:", error);
      toast.error("Failed to load products");
    } finally {
      setIsLoadingProducts(false);
    }
  };

  const fetchCustomers = async () => {
    try {
      setIsLoadingCustomers(true);
      const response = await api.get("/api/customer/all");
      const customerData = response.data.filter(
        (c: Customer) => c.role === "CUSTOMER"
      );
      setCustomers(customerData);
    } catch (error) {
      console.error("Failed to fetch customers:", error);
      toast.error("Failed to load customers");
    } finally {
      setIsLoadingCustomers(false);
    }
  };

  const handleCustomerSelect = (customerId: string) => {
    const customer = customers.find((c) => c.id === customerId);
    setSelectedCustomer(customer || null);
    setFormDataV1({ ...formDataV1, customerId });
    setFormDataV2({ ...formDataV2, customerId });
  };

  const handleProductSelect = (productCode: string, version: "v1" | "v2") => {
    const product = products.find((p) => p.productCode === productCode);
    setSelectedProduct(product || null);

    if (version === "v1") {
      setFormDataV1({ ...formDataV1, productCode });
    } else {
      setFormDataV2({
        ...formDataV2,
        productCode,
        customInterestRate: product?.minInterestRate.toString() || "",
        customTenureMonths: product?.minTermMonths.toString() || "",
      });
    }
  };

  const handleSubmitV1 = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const response = await api.post<AccountCreationResponse>(
        "/api/v1/accounts",
        {
          customerId: formDataV1.customerId,
          productCode: formDataV1.productCode,
          principalAmount: parseFloat(formDataV1.principalAmount),
          branchCode: formDataV1.branchCode,
          remarks: formDataV1.remarks || undefined,
        }
      );

      toast.success(t("accounts.create.successDefault"));
      navigate(`/accounts/${response.data.data.accountNo}`);
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || t("accounts.create.error");
      toast.error(errorMessage);
      console.error("Account creation failed:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmitV2 = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const payload: any = {
        customerId: formDataV2.customerId,
        productCode: formDataV2.productCode,
        principalAmount: parseFloat(formDataV2.principalAmount),
        branchCode: formDataV2.branchCode,
        remarks: formDataV2.remarks || undefined,
      };

      if (formDataV2.customInterestRate) {
        payload.customInterestRate = parseFloat(formDataV2.customInterestRate);
      }

      if (formDataV2.customTenureMonths) {
        payload.customTenureMonths = parseInt(formDataV2.customTenureMonths);
      }

      const response = await api.post<AccountCreationResponse>(
        "/api/v2/accounts",
        payload
      );

      toast.success(t("accounts.create.successCustom"));
      navigate(`/accounts/${response.data.data.accountNo}`);
    } catch (error: any) {
      const errorMessage =
        error.response?.data?.message || t("accounts.create.error");
      toast.error(errorMessage);
      console.error("Account creation failed:", error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container mx-auto py-8 max-w-4xl">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">
          {t("accounts.create.title")}
        </h1>
        <p className="text-muted-foreground mt-2">
          {t("accounts.create.description")}
        </p>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>{t("accounts.create.selectCustomer")}</CardTitle>
          <CardDescription>
            {t("accounts.create.selectCustomerDescription")}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <Label htmlFor="customer-select">
              {t("accounts.create.customer")}
            </Label>
            <Select
              value={formDataV1.customerId}
              onValueChange={handleCustomerSelect}
              required
            >
              <SelectTrigger id="customer-select">
                <SelectValue
                  placeholder={t("accounts.create.selectCustomerPlaceholder")}
                />
              </SelectTrigger>
              <SelectContent>
                {isLoadingCustomers ? (
                  <div className="p-2 text-sm text-muted-foreground">
                    {t("accounts.create.loadingCustomers")}
                  </div>
                ) : customers.length === 0 ? (
                  <div className="p-2 text-sm text-muted-foreground">
                    {t("accounts.create.noCustomers")}
                  </div>
                ) : (
                  customers.map((customer) => (
                    <SelectItem key={customer.id} value={customer.id}>
                      {customer.name} - {customer.email}
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {formDataV1.customerId && (
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="v1">
              {t("accounts.create.standardTab")}
              <span className="ml-2 text-xs bg-primary/10 px-2 py-0.5 rounded">
                {t("accounts.create.productDefaults")}
              </span>
            </TabsTrigger>
            <TabsTrigger value="v2">
              {t("accounts.create.customTab")}
              <span className="ml-2 text-xs bg-primary/10 px-2 py-0.5 rounded">
                {t("accounts.create.flexible")}
              </span>
            </TabsTrigger>
          </TabsList>

          <TabsContent value="v1">
            <Card>
              <CardHeader>
                <CardTitle>{t("accounts.create.standardTitle")}</CardTitle>
                <CardDescription>
                  {t("accounts.create.standardDescription")}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmitV1} className="space-y-6">
                  <div className="space-y-2">
                    <Label htmlFor="productCode-v1">
                      {t("accounts.create.product")}
                    </Label>
                    <Select
                      value={formDataV1.productCode}
                      onValueChange={(value) =>
                        handleProductSelect(value, "v1")
                      }
                      required
                    >
                      <SelectTrigger id="productCode-v1">
                        <SelectValue
                          placeholder={t("accounts.create.selectProduct")}
                        />
                      </SelectTrigger>
                      <SelectContent>
                        {isLoadingProducts ? (
                          <div className="p-2 text-sm text-muted-foreground">
                            {t("accounts.create.loadingProducts")}
                          </div>
                        ) : (
                          products.map((product) => (
                            <SelectItem
                              key={product.productCode}
                              value={product.productCode}
                            >
                              {product.productName} ({product.minInterestRate}%
                              - {product.minTermMonths}mo)
                            </SelectItem>
                          ))
                        )}
                      </SelectContent>
                    </Select>
                  </div>

                  {selectedProduct && activeTab === "v1" && (
                    <Alert>
                      <Info className="h-4 w-4" />
                      <AlertDescription>
                        <strong>
                          {t("accounts.create.productDefaultsLabel")}
                        </strong>{" "}
                        {t("accounts.create.interestRate")}:{" "}
                        {selectedProduct.minInterestRate}% |{" "}
                        {t("accounts.create.tenure")}:{" "}
                        {selectedProduct.minTermMonths} {t("common.months")} |{" "}
                        {t("accounts.create.range")}:{" "}
                        {selectedProduct.minAmount.toFixed(0)} -{" "}
                        {selectedProduct.maxAmount.toFixed(0)}{" "}
                        {t("common.tokens")}
                      </AlertDescription>
                    </Alert>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="principalAmount-v1">
                      {t("accounts.create.principalAmount")}
                    </Label>
                    <Input
                      id="principalAmount-v1"
                      type="number"
                      required
                      min="1"
                      step="1"
                      value={formDataV1.principalAmount}
                      onChange={(e) =>
                        setFormDataV1({
                          ...formDataV1,
                          principalAmount: e.target.value,
                        })
                      }
                      placeholder={t(
                        "accounts.create.principalAmountPlaceholder"
                      )}
                    />
                    {selectedProduct && (
                      <p className="text-xs text-muted-foreground">
                        {t("common.min")}:{" "}
                        {selectedProduct.minAmount.toFixed(0)} |{" "}
                        {t("common.max")}:{" "}
                        {selectedProduct.maxAmount.toFixed(0)}{" "}
                        {t("common.tokens")}
                      </p>
                    )}
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="branchCode-v1">
                      {t("accounts.create.branchCode")}
                    </Label>
                    <Input
                      id="branchCode-v1"
                      type="text"
                      required
                      pattern="[A-Z0-9]{3,20}"
                      value={formDataV1.branchCode}
                      onChange={(e) =>
                        setFormDataV1({
                          ...formDataV1,
                          branchCode: e.target.value.toUpperCase(),
                        })
                      }
                      placeholder="BR001"
                    />
                    <p className="text-xs text-muted-foreground">
                      {t("accounts.create.branchCodeHint")}
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="remarks-v1">
                      {t("accounts.create.remarks")}
                    </Label>
                    <Textarea
                      id="remarks-v1"
                      maxLength={500}
                      value={formDataV1.remarks}
                      onChange={(e) =>
                        setFormDataV1({
                          ...formDataV1,
                          remarks: e.target.value,
                        })
                      }
                      placeholder={t("accounts.create.remarksPlaceholder")}
                    />
                  </div>

                  <div className="flex gap-4">
                    <Button
                      type="submit"
                      className="flex-1"
                      disabled={isLoading || !selectedProduct}
                    >
                      {isLoading ? (
                        <>
                          <Loader2 className="h-4 w-4 animate-spin" />
                          {t("accounts.create.creating")}
                        </>
                      ) : (
                        <>
                          <CheckCircle2 className="h-4 w-4" />
                          {t("accounts.create.createStandard")}
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="v2">
            <Card>
              <CardHeader>
                <CardTitle>{t("accounts.create.customTitle")}</CardTitle>
                <CardDescription>
                  {t("accounts.create.customDescription")}
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSubmitV2} className="space-y-6">
                  <div className="space-y-2">
                    <Label htmlFor="productCode-v2">
                      {t("accounts.create.product")}
                    </Label>
                    <Select
                      value={formDataV2.productCode}
                      onValueChange={(value) =>
                        handleProductSelect(value, "v2")
                      }
                      required
                    >
                      <SelectTrigger id="productCode-v2">
                        <SelectValue
                          placeholder={t("accounts.create.selectProduct")}
                        />
                      </SelectTrigger>
                      <SelectContent>
                        {isLoadingProducts ? (
                          <div className="p-2 text-sm text-muted-foreground">
                            {t("accounts.create.loadingProducts")}
                          </div>
                        ) : (
                          products.map((product) => (
                            <SelectItem
                              key={product.productCode}
                              value={product.productCode}
                            >
                              {product.productName}
                            </SelectItem>
                          ))
                        )}
                      </SelectContent>
                    </Select>
                  </div>

                  {selectedProduct && activeTab === "v2" && (
                    <Alert>
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>
                        <strong>
                          {t("accounts.create.productLimitsLabel")}
                        </strong>{" "}
                        {t("accounts.create.interestRate")}:{" "}
                        {selectedProduct.minInterestRate}% -{" "}
                        {selectedProduct.maxInterestRate}% |{" "}
                        {t("accounts.create.tenure")}:{" "}
                        {selectedProduct.minTermMonths} -{" "}
                        {selectedProduct.maxTermMonths} {t("common.months")} |{" "}
                        {t("accounts.create.amount")}:{" "}
                        {selectedProduct.minAmount.toFixed(0)} -{" "}
                        {selectedProduct.maxAmount.toFixed(0)}{" "}
                        {t("common.tokens")}
                      </AlertDescription>
                    </Alert>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="principalAmount-v2">
                      {t("accounts.create.principalAmount")}
                    </Label>
                    <Input
                      id="principalAmount-v2"
                      type="number"
                      required
                      min="1"
                      step="1"
                      value={formDataV2.principalAmount}
                      onChange={(e) =>
                        setFormDataV2({
                          ...formDataV2,
                          principalAmount: e.target.value,
                        })
                      }
                      placeholder={t(
                        "accounts.create.principalAmountPlaceholder"
                      )}
                    />
                    {selectedProduct && (
                      <p className="text-xs text-muted-foreground">
                        {t("common.min")}:{" "}
                        {selectedProduct.minAmount.toFixed(0)} |{" "}
                        {t("common.max")}:{" "}
                        {selectedProduct.maxAmount.toFixed(0)}{" "}
                        {t("common.tokens")}
                      </p>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="customInterestRate-v2">
                        {t("accounts.create.customInterestRate")}
                      </Label>
                      <Input
                        id="customInterestRate-v2"
                        type="number"
                        step="0.01"
                        min={selectedProduct?.minInterestRate || 0}
                        max={selectedProduct?.maxInterestRate || 100}
                        value={formDataV2.customInterestRate}
                        onChange={(e) =>
                          setFormDataV2({
                            ...formDataV2,
                            customInterestRate: e.target.value,
                          })
                        }
                        placeholder={t("common.optional")}
                      />
                      {selectedProduct && (
                        <p className="text-xs text-muted-foreground">
                          {t("common.range")}: {selectedProduct.minInterestRate}
                          % - {selectedProduct.maxInterestRate}%
                        </p>
                      )}
                    </div>

                    <div className="space-y-2">
                      <Label htmlFor="customTenureMonths-v2">
                        {t("accounts.create.customTenure")}
                      </Label>
                      <Input
                        id="customTenureMonths-v2"
                        type="number"
                        step="1"
                        min={selectedProduct?.minTermMonths || 1}
                        max={selectedProduct?.maxTermMonths || 120}
                        value={formDataV2.customTenureMonths}
                        onChange={(e) =>
                          setFormDataV2({
                            ...formDataV2,
                            customTenureMonths: e.target.value,
                          })
                        }
                        placeholder={t("common.optional")}
                      />
                      {selectedProduct && (
                        <p className="text-xs text-muted-foreground">
                          {t("common.range")}: {selectedProduct.minTermMonths} -{" "}
                          {selectedProduct.maxTermMonths} {t("common.months")}
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="branchCode-v2">
                      {t("accounts.create.branchCode")}
                    </Label>
                    <Input
                      id="branchCode-v2"
                      type="text"
                      required
                      pattern="[A-Z0-9]{3,20}"
                      value={formDataV2.branchCode}
                      onChange={(e) =>
                        setFormDataV2({
                          ...formDataV2,
                          branchCode: e.target.value.toUpperCase(),
                        })
                      }
                      placeholder="BR001"
                    />
                    <p className="text-xs text-muted-foreground">
                      {t("accounts.create.branchCodeHint")}
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="remarks-v2">
                      {t("accounts.create.remarks")}
                    </Label>
                    <Textarea
                      id="remarks-v2"
                      maxLength={500}
                      value={formDataV2.remarks}
                      onChange={(e) =>
                        setFormDataV2({
                          ...formDataV2,
                          remarks: e.target.value,
                        })
                      }
                      placeholder={t("accounts.create.remarksPlaceholder")}
                    />
                  </div>

                  <div className="flex gap-4">
                    <Button
                      type="submit"
                      className="flex-1"
                      disabled={isLoading || !selectedProduct}
                    >
                      {isLoading ? (
                        <>
                          <Loader2 className="h-4 w-4 animate-spin" />
                          {t("accounts.create.creating")}
                        </>
                      ) : (
                        <>
                          <CheckCircle2 className="h-4 w-4" />
                          {t("accounts.create.createCustom")}
                        </>
                      )}
                    </Button>
                  </div>
                </form>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      )}
    </div>
  );
}
