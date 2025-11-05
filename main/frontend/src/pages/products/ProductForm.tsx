import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
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
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { Package, Save, ArrowLeft, Loader2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api";

const productSchema = z
  .object({
    name: z.string().min(2, "Product name must be at least 2 characters"),
    description: z
      .string()
      .min(10, "Description must be at least 10 characters"),
    minInterestRate: z.number().min(0, "Minimum rate must be ≥ 0"),
    maxInterestRate: z.number().min(0, "Maximum rate must be ≥ 0"),
    minTermMonths: z.number().min(1, "Minimum tenure must be at least 1 month"),
    maxTermMonths: z.number().min(1, "Maximum tenure must be at least 1 month"),
    minAmount: z.number().min(1, "Minimum amount must be at least 1 token"),
    maxAmount: z.number().min(1, "Maximum amount must be at least 1 token"),
    compoundingFrequency: z.string().min(1, "Select compounding frequency"),
    category: z.string().min(1, "Please select a category"),
    isActive: z.boolean(),
    prematurePenaltyRate: z
      .number()
      .min(0, "Penalty rate must be ≥ 0")
      .max(1, "Penalty rate cannot exceed 1 (100%)"),
    prematurePenaltyGraceDays: z
      .number()
      .min(0, "Grace days must be ≥ 0")
      .max(365, "Grace days cannot exceed 365"),
  })
  .refine((d) => d.minInterestRate <= d.maxInterestRate, {
    message: "Min rate must be ≤ max rate",
    path: ["maxInterestRate"],
  })
  .refine((d) => d.minTermMonths <= d.maxTermMonths, {
    message: "Min tenure must be ≤ max tenure",
    path: ["maxTermMonths"],
  });

type ProductFormData = z.infer<typeof productSchema>;

const categories = [
  "Fixed Deposit",
  "Recurring Deposit",
  "Savings Account",
  "Current Account",
  "Personal Loan",
  "Home Loan",
  "Car Loan",
  "Credit Card",
  "Debit Card",
];

const categoryToProductType: Record<string, string> = {
  "Fixed Deposit": "FIXED_DEPOSIT",
  "Recurring Deposit": "RECURRING_DEPOSIT",
  "Savings Account": "SAVINGS_ACCOUNT",
  "Current Account": "CURRENT_ACCOUNT",
  "Personal Loan": "PERSONAL_LOAN",
  "Home Loan": "HOME_LOAN",
  "Car Loan": "CAR_LOAN",
  "Credit Card": "CREDIT_CARD",
  "Debit Card": "DEBIT_CARD",
};

export function ProductForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [isLoading, setIsLoading] = useState(!!id);
  const [isSaving, setIsSaving] = useState(false);

  const isEdit = !!id;
  const isAdmin = user?.role === "ADMIN" || user?.role === "BANKOFFICER";

  const form = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: "",
      description: "",
      minInterestRate: 5,
      maxInterestRate: 8,
      minAmount: 10000,
      maxAmount: 1000000,
      minTermMonths: 6,
      maxTermMonths: 60,
      compoundingFrequency: "ANNUAL",
      category: "",
      isActive: true,
      prematurePenaltyRate: 0,
      prematurePenaltyGraceDays: 0,
    },
  });

  useEffect(() => {
    if (!isAdmin) {
      toast.error("You do not have permission to access this page");
      navigate("/products");
      return;
    }

    if (isEdit) {
      const fetchProduct = async () => {
        try {
          const response = await api.get(`/api/v1/product/${id}`);
          const product = (response?.data?.data ?? response?.data) as any;
          form.reset({
            name: product.productName || "",
            description: product.description || "",
            minInterestRate: Number(product.minInterestRate ?? 0),
            maxInterestRate: Number(product.maxInterestRate ?? 0),
            minAmount: Number(product.minAmount ?? 0),
            maxAmount: Number(product.maxAmount ?? 0),
            minTermMonths: Number(product.minTermMonths ?? 1),
            maxTermMonths: Number(product.maxTermMonths ?? 1),
            compoundingFrequency: String(
              product.compoundingFrequency || "ANNUAL"
            ),
            category: (product.productType || "").toString().replace("_", " "),
            isActive: (product.status || "ACTIVE") === "ACTIVE",
            prematurePenaltyRate: Number(product.prematurePenaltyRate ?? 0),
            prematurePenaltyGraceDays: Number(
              product.prematurePenaltyGraceDays ?? 0
            ),
          });
        } catch (error) {
          console.error("Failed to fetch product:", error);
          toast.error("Failed to load product data");
          navigate("/products");
        } finally {
          setIsLoading(false);
        }
      };

      fetchProduct();
    }
  }, [id, isEdit, isAdmin, navigate, form]);

  const onSubmit = async (data: ProductFormData) => {
    if (data.minAmount >= data.maxAmount) {
      toast.error("Maximum amount must be greater than minimum amount");
      return;
    }

    setIsSaving(true);
    try {
      const now = new Date().toISOString().slice(0, 10);
      const rawBase = data.name
        .trim()
        .toUpperCase()
        .replace(/\s+/g, "-")
        .replace(/[^A-Z0-9-]/g, "");
      const suffix = Math.random().toString(36).slice(2, 6).toUpperCase();
      const baseLimited = (rawBase || "FD").slice(0, 15);
      const codeCompliant = `${baseLimited}-${suffix}`.slice(0, 20);
      const productType =
        categoryToProductType[data.category] || "FIXED_DEPOSIT";
      const payload = {
        productCode: codeCompliant,
        productName: data.name,
        productType,
        description: data.description,
        minInterestRate: data.minInterestRate,
        maxInterestRate: data.maxInterestRate,
        minTermMonths: data.minTermMonths,
        maxTermMonths: data.maxTermMonths,
        minAmount: data.minAmount,
        maxAmount: data.maxAmount,
        currency: "INR",
        compoundingFrequency: data.compoundingFrequency,
        status: data.isActive ? "ACTIVE" : "INACTIVE",
        effectiveDate: now,
        expiryDate: null,
        regulatoryCode: "",
        requiresApproval: false,
        prematurePenaltyRate: data.prematurePenaltyRate,
        prematurePenaltyGraceDays: data.prematurePenaltyGraceDays,
      };

      if (isEdit) {
        await api.put(`/api/v1/product/${id}`, payload);
        toast.success("Product updated successfully");
      } else {
        await api.post("/api/v1/product", payload);
        toast.success("Product created successfully");
      }
      navigate("/products");
    } catch (error) {
      toast.error(
        isEdit ? "Failed to update product" : "Failed to create product"
      );
    } finally {
      setIsSaving(false);
    }
  };

  if (!isAdmin) {
    return null;
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Skeleton className="h-10 w-10" />
          <Skeleton className="h-8 w-48" />
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-4 w-64" />
          </CardHeader>
          <CardContent className="space-y-4">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-10 w-full" />
              </div>
            ))}
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
          onClick={() => navigate("/products")}
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Products
        </Button>
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {isEdit ? "Edit Product" : "Create New Product"}
          </h1>
          <p className="text-muted-foreground">
            {isEdit
              ? "Update the product information below"
              : "Fill in the details to create a new banking product"}
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Package className="h-5 w-5" />
            Product Information
          </CardTitle>
          <CardDescription>
            Enter the product details and configuration
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  control={form.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Product Name</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="e.g., Premium Fixed Deposit"
                          {...field}
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="category"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Category</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        value={field.value || undefined}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select a category" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {categories.map((category) => (
                            <SelectItem key={category} value={category}>
                              {category}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  control={form.control}
                  name="prematurePenaltyRate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Premature Penalty Rate (0 - 1)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          step="0.001"
                          min="0"
                          max="1"
                          placeholder="0.02"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="prematurePenaltyGraceDays"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Penalty Grace Period (days)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min="0"
                          max="365"
                          placeholder="30"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Description</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Describe the product features and benefits..."
                        className="min-h-[100px]"
                        {...field}
                        disabled={isSaving}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="grid gap-4 md:grid-cols-3">
                <FormField
                  control={form.control}
                  name="minInterestRate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Min Interest Rate (% p.a.)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          step="0.1"
                          placeholder="5.0"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="maxInterestRate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Max Interest Rate (% p.a.)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          step="0.1"
                          placeholder="8.0"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="isActive"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                      <div className="space-y-0.5">
                        <FormLabel className="text-base">
                          Active Status
                        </FormLabel>
                        <div className="text-sm text-muted-foreground">
                          Enable or disable this product
                        </div>
                      </div>
                      <FormControl>
                        <Switch
                          checked={field.value}
                          onCheckedChange={field.onChange}
                          disabled={isSaving}
                        />
                      </FormControl>
                    </FormItem>
                  )}
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  control={form.control}
                  name="compoundingFrequency"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Compounding Frequency</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        value={field.value || undefined}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select frequency" />
                          </SelectTrigger>
                        </FormControl>
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
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="minTermMonths"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Min Tenure (Months)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="6"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="maxTermMonths"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Max Tenure (Months)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="60"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <FormField
                  control={form.control}
                  name="minAmount"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Minimum Amount (CCHD)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="10000"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="maxAmount"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Maximum Amount (CCHD)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          placeholder="1000000"
                          {...field}
                          onChange={(e) =>
                            field.onChange(Number(e.target.value))
                          }
                          disabled={isSaving}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              <div className="flex justify-end gap-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate("/products")}
                  disabled={isSaving}
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={isSaving}>
                  {isSaving ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      {isEdit ? "Updating..." : "Creating..."}
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4" />
                      {isEdit ? "Update Product" : "Create Product"}
                    </>
                  )}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}
