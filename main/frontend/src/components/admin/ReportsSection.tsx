import { useState } from "react";
import { toast } from "sonner";
import { Download, Loader2 } from "lucide-react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";

import { api } from "@/lib/api";

export function ReportsSection() {
  const [activeTab, setActiveTab] = useState("products");
  const [isDownloading, setIsDownloading] = useState(false);

  const [productFilters, setProductFilters] = useState({
    productType: "",
    currency: "",
    status: "",
    effectiveDateFrom: "",
    effectiveDateTo: "",
    requiresApproval: "",
  });

  const [customerFilters, setCustomerFilters] = useState({
    role: "",
    active: "",
    createdDateFrom: "",
    createdDateTo: "",
  });

  const [accountFilters, setAccountFilters] = useState({
    customerId: "",
    productCode: "",
    status: "",
    createdDateFrom: "",
    createdDateTo: "",
    maturityDateFrom: "",
    maturityDateTo: "",
  });

  const generateProductReport = async () => {
    setIsDownloading(true);
    try {
      const params = new URLSearchParams();

      if (productFilters.productType && productFilters.productType !== "all")
        params.append("productType", productFilters.productType);
      if (productFilters.currency && productFilters.currency !== "all")
        params.append("currency", productFilters.currency);
      if (productFilters.status && productFilters.status !== "all")
        params.append("status", productFilters.status);
      if (productFilters.effectiveDateFrom)
        params.append("effectiveDateFrom", productFilters.effectiveDateFrom);
      if (productFilters.effectiveDateTo)
        params.append("effectiveDateTo", productFilters.effectiveDateTo);
      if (
        productFilters.requiresApproval &&
        productFilters.requiresApproval !== "all"
      )
        params.append("requiresApproval", productFilters.requiresApproval);

      const response = await api.get(
        `/api/v1/product/reports/export-csv?${params.toString()}`,
        {
          responseType: "blob",
        }
      );

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;

      const contentDisposition = response.headers["content-disposition"];
      let filename = "products-report.csv";
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
        if (filenameMatch) filename = filenameMatch[1];
      }

      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success("Product report downloaded");
    } catch (error: any) {
      const message =
        error?.response?.data?.message || "Failed to download product report";
      toast.error(message);
    } finally {
      setIsDownloading(false);
    }
  };

  const generateCustomerReport = async () => {
    setIsDownloading(true);
    try {
      const params = new URLSearchParams();

      if (customerFilters.role && customerFilters.role !== "all")
        params.append("role", customerFilters.role);
      if (customerFilters.active && customerFilters.active !== "all")
        params.append("active", customerFilters.active);
      if (customerFilters.createdDateFrom)
        params.append("createdDateFrom", customerFilters.createdDateFrom);
      if (customerFilters.createdDateTo)
        params.append("createdDateTo", customerFilters.createdDateTo);

      const response = await api.get(
        `/api/v1/customer/reports/export-csv?${params.toString()}`,
        {
          responseType: "blob",
        }
      );

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;

      const contentDisposition = response.headers["content-disposition"];
      let filename = "customers-report.csv";
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
        if (filenameMatch) filename = filenameMatch[1];
      }

      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success("Customer report downloaded");
    } catch (error: any) {
      const message =
        error?.response?.data?.message || "Failed to download customer report";
      toast.error(message);
    } finally {
      setIsDownloading(false);
    }
  };

  const generateAccountReport = async () => {
    setIsDownloading(true);
    try {
      const params = new URLSearchParams();

      if (accountFilters.customerId)
        params.append("customerId", accountFilters.customerId);
      if (accountFilters.productCode)
        params.append("productCode", accountFilters.productCode);
      if (accountFilters.status && accountFilters.status !== "all")
        params.append("status", accountFilters.status);
      if (accountFilters.createdDateFrom)
        params.append("createdDateFrom", accountFilters.createdDateFrom);
      if (accountFilters.createdDateTo)
        params.append("createdDateTo", accountFilters.createdDateTo);
      if (accountFilters.maturityDateFrom)
        params.append("maturityDateFrom", accountFilters.maturityDateFrom);
      if (accountFilters.maturityDateTo)
        params.append("maturityDateTo", accountFilters.maturityDateTo);

      const response = await api.get(
        `/api/v1/accounts/reports/export-csv?${params.toString()}`,
        {
          responseType: "blob",
        }
      );

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;

      const contentDisposition = response.headers["content-disposition"];
      let filename = "accounts-report.csv";
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
        if (filenameMatch) filename = filenameMatch[1];
      }

      link.setAttribute("download", filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);

      toast.success("Account report downloaded");
    } catch (error: any) {
      const message =
        error?.response?.data?.message || "Failed to download account report";
      toast.error(message);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Reports</CardTitle>
        <CardDescription>Generate and export system reports</CardDescription>
      </CardHeader>
      <CardContent>
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="products">Products</TabsTrigger>
            <TabsTrigger value="customers">Customers</TabsTrigger>
            <TabsTrigger value="accounts">Accounts</TabsTrigger>
          </TabsList>

          <TabsContent value="products" className="space-y-4">
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Product Type</label>
                  <Select
                    value={productFilters.productType || "all"}
                    onValueChange={(value) =>
                      setProductFilters((prev) => ({
                        ...prev,
                        productType: value === "all" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All types" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All types</SelectItem>
                      <SelectItem value="FIXED_DEPOSIT">
                        Fixed Deposit
                      </SelectItem>
                      <SelectItem value="SAVINGS">Savings</SelectItem>
                      <SelectItem value="CURRENT">Current</SelectItem>
                      <SelectItem value="RECURRING">Recurring</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Currency</label>
                  <Select
                    value={productFilters.currency || "all"}
                    onValueChange={(value) =>
                      setProductFilters((prev) => ({
                        ...prev,
                        currency: value === "all" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All currencies" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All currencies</SelectItem>
                      <SelectItem value="USD">USD</SelectItem>
                      <SelectItem value="EUR">EUR</SelectItem>
                      <SelectItem value="INR">INR</SelectItem>
                      <SelectItem value="GBP">GBP</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select
                    value={productFilters.status || "all"}
                    onValueChange={(value) =>
                      setProductFilters((prev) => ({
                        ...prev,
                        status: value === "all" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All statuses</SelectItem>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="INACTIVE">Inactive</SelectItem>
                      <SelectItem value="ARCHIVED">Archived</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Effective Date From
                  </label>
                  <Input
                    type="date"
                    value={productFilters.effectiveDateFrom}
                    onChange={(e) =>
                      setProductFilters((prev) => ({
                        ...prev,
                        effectiveDateFrom: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Effective Date To
                  </label>
                  <Input
                    type="date"
                    value={productFilters.effectiveDateTo}
                    onChange={(e) =>
                      setProductFilters((prev) => ({
                        ...prev,
                        effectiveDateTo: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Requires Approval
                  </label>
                  <Select
                    value={productFilters.requiresApproval || "any"}
                    onValueChange={(value) =>
                      setProductFilters((prev) => ({
                        ...prev,
                        requiresApproval: value === "any" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Any" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="any">Any</SelectItem>
                      <SelectItem value="true">Yes</SelectItem>
                      <SelectItem value="false">No</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  onClick={() =>
                    setProductFilters({
                      productType: "",
                      currency: "",
                      status: "",
                      effectiveDateFrom: "",
                      effectiveDateTo: "",
                      requiresApproval: "",
                    })
                  }
                >
                  Clear Filters
                </Button>
                <Button
                  onClick={generateProductReport}
                  disabled={isDownloading}
                >
                  {isDownloading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Downloading...
                    </>
                  ) : (
                    <>
                      <Download className="h-4 w-4" />
                      Export CSV
                    </>
                  )}
                </Button>
              </div>

              <div className="bg-muted p-3 rounded-md text-sm text-muted-foreground">
                <p>
                  The CSV export includes all product details including rates,
                  terms, amounts, regulatory information, and metadata.
                </p>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="customers" className="space-y-4">
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Role</label>
                  <Select
                    value={customerFilters.role || "all"}
                    onValueChange={(value) =>
                      setCustomerFilters((prev) => ({
                        ...prev,
                        role: value === "all" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All roles" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All roles</SelectItem>
                      <SelectItem value="CUSTOMER">Customer</SelectItem>
                      <SelectItem value="ADMIN">Admin</SelectItem>
                      <SelectItem value="BANKOFFICER">Bank Officer</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Active Status</label>
                  <Select
                    value={customerFilters.active || "all"}
                    onValueChange={(value) =>
                      setCustomerFilters((prev) => ({
                        ...prev,
                        active: value === "all" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All statuses</SelectItem>
                      <SelectItem value="true">Active</SelectItem>
                      <SelectItem value="false">Inactive</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Created Date From
                  </label>
                  <Input
                    type="date"
                    value={customerFilters.createdDateFrom}
                    onChange={(e) =>
                      setCustomerFilters((prev) => ({
                        ...prev,
                        createdDateFrom: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Created Date To</label>
                  <Input
                    type="date"
                    value={customerFilters.createdDateTo}
                    onChange={(e) =>
                      setCustomerFilters((prev) => ({
                        ...prev,
                        createdDateTo: e.target.value,
                      }))
                    }
                  />
                </div>
              </div>

              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  onClick={() =>
                    setCustomerFilters({
                      role: "",
                      active: "",
                      createdDateFrom: "",
                      createdDateTo: "",
                    })
                  }
                >
                  Clear Filters
                </Button>
                <Button
                  onClick={generateCustomerReport}
                  disabled={isDownloading}
                >
                  {isDownloading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Downloading...
                    </>
                  ) : (
                    <>
                      <Download className="h-4 w-4" />
                      Export CSV
                    </>
                  )}
                </Button>
              </div>

              <div className="bg-muted p-3 rounded-md text-sm text-muted-foreground">
                <p>
                  The CSV export includes all customer details including
                  username, email, phone, role, currency preference, and
                  metadata.
                </p>
              </div>
            </div>
          </TabsContent>

          <TabsContent value="accounts" className="space-y-4">
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Customer ID</label>
                  <Input
                    placeholder="Filter by customer ID"
                    value={accountFilters.customerId}
                    onChange={(e) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        customerId: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Product Code</label>
                  <Input
                    placeholder="Filter by product code"
                    value={accountFilters.productCode}
                    onChange={(e) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        productCode: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Status</label>
                  <Select
                    value={accountFilters.status || "all"}
                    onValueChange={(value) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        status: value === "all" ? "" : value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All statuses" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All statuses</SelectItem>
                      <SelectItem value="ACTIVE">Active</SelectItem>
                      <SelectItem value="CLOSED">Closed</SelectItem>
                      <SelectItem value="SUSPENDED">Suspended</SelectItem>
                      <SelectItem value="MATURED">Matured</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Created Date From
                  </label>
                  <Input
                    type="date"
                    value={accountFilters.createdDateFrom}
                    onChange={(e) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        createdDateFrom: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">Created Date To</label>
                  <Input
                    type="date"
                    value={accountFilters.createdDateTo}
                    onChange={(e) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        createdDateTo: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Maturity Date From
                  </label>
                  <Input
                    type="date"
                    value={accountFilters.maturityDateFrom}
                    onChange={(e) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        maturityDateFrom: e.target.value,
                      }))
                    }
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">
                    Maturity Date To
                  </label>
                  <Input
                    type="date"
                    value={accountFilters.maturityDateTo}
                    onChange={(e) =>
                      setAccountFilters((prev) => ({
                        ...prev,
                        maturityDateTo: e.target.value,
                      }))
                    }
                  />
                </div>
              </div>

              <div className="flex gap-2 justify-end">
                <Button
                  variant="outline"
                  onClick={() =>
                    setAccountFilters({
                      customerId: "",
                      productCode: "",
                      status: "",
                      createdDateFrom: "",
                      createdDateTo: "",
                      maturityDateFrom: "",
                      maturityDateTo: "",
                    })
                  }
                >
                  Clear Filters
                </Button>
                <Button
                  onClick={generateAccountReport}
                  disabled={isDownloading}
                >
                  {isDownloading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Downloading...
                    </>
                  ) : (
                    <>
                      <Download className="h-4 w-4" />
                      Export CSV
                    </>
                  )}
                </Button>
              </div>

              <div className="bg-muted p-3 rounded-md text-sm text-muted-foreground">
                <p>
                  The CSV export includes all account details including account
                  number, customer ID, principal amount, interest rates, tenure,
                  maturity date, status, and pricing rule information.
                </p>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </CardContent>
    </Card>
  );
}
