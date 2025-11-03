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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  ArrowLeft,
  AlertTriangle,
  CheckCircle2,
  Calendar,
  DollarSign,
} from "lucide-react";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { useStablecoinConversion } from "@/hooks/useStablecoinConversion";

interface RedemptionEnquiry {
  accountNo: string;
  customerId: string;
  principalAmount: number;
  interestRate: number;
  tenureMonths: number;
  maturityDate: string;
  currentDate: string;
  isMatured: boolean;
  daysUntilMaturity: number;
  daysOverdue: number | null;
  accruedInterest: number;
  maturityAmount: number;
  penaltyAmount: number;
  netPayableAmount: number;
  penaltyReason: string;
  currentWalletBalance: number;
  hasSufficientBalance: boolean;
  redemptionEligibility: string;
  warnings: string[];
}

export function RedemptionPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [enquiry, setEnquiry] = useState<RedemptionEnquiry | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  const { formatTokens, formatConvertedTokens, preferredCurrency } =
    useStablecoinConversion();

  useEffect(() => {
    fetchRedemptionEnquiry();
  }, [id]);

  const fetchRedemptionEnquiry = async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const response = await api.get(`/api/accounts/redemption/${id}/enquiry`);
      setEnquiry(response.data.data);
    } catch (error: any) {
      console.error("Failed to fetch redemption enquiry:", error);
      toast.error(
        error?.response?.data?.message || "Failed to load redemption details"
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleRedemptionProcess = async () => {
    if (!id) return;
    setIsProcessing(true);
    try {
      const response = await api.post(`/api/accounts/redemption/${id}/process`);
      toast.success(
        response.data.message || "Redemption processed successfully"
      );
      setTimeout(() => {
        navigate(`/accounts/${id}`);
      }, 1500);
    } catch (error: any) {
      console.error("Redemption process failed:", error);
      toast.error(
        error?.response?.data?.message || "Failed to process redemption"
      );
    } finally {
      setIsProcessing(false);
      setShowConfirmDialog(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!enquiry) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(`/accounts/${id}`)}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-bold">Redemption</h1>
        </div>
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">
              Unable to load redemption details
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const isPremature = !enquiry.isMatured;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(`/accounts/${id}`)}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">Redemption</h1>
            <p className="text-xs text-muted-foreground">{enquiry.accountNo}</p>
          </div>
        </div>
        <Badge variant={isPremature ? "destructive" : "default"}>
          {isPremature ? "Premature" : "Matured"}
        </Badge>
      </div>

      {isPremature && (
        <Card className="border-yellow-200 bg-yellow-50">
          <CardContent className="pt-6 flex gap-3">
            <AlertTriangle className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-yellow-900">
                Penalty: {formatTokens(enquiry.penaltyAmount)} CCHD
              </p>
              <p className="text-xs text-yellow-800 mt-1">
                {enquiry.daysUntilMaturity} days remaining
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader className="pb-3">
            <CardDescription className="text-xs">Principal</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">
              {formatTokens(enquiry.principalAmount)}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {formatConvertedTokens(
                enquiry.principalAmount,
                preferredCurrency
              )}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <CardDescription className="text-xs">Interest</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-emerald-600">
              +{formatTokens(enquiry.accruedInterest)}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {enquiry.interestRate.toFixed(2)}% annual
            </p>
          </CardContent>
        </Card>

        {enquiry.penaltyAmount > 0 && (
          <Card>
            <CardHeader className="pb-3">
              <CardDescription className="text-xs">Penalty</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold text-red-600">
                -{formatTokens(enquiry.penaltyAmount)}
              </p>
              <p className="text-xs text-muted-foreground mt-1">
                {enquiry.penaltyReason}
              </p>
            </CardContent>
          </Card>
        )}

        <Card className="border-primary">
          <CardHeader className="pb-3">
            <CardDescription className="text-xs">Total Payout</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold text-primary">
              {formatTokens(enquiry.netPayableAmount)}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {formatConvertedTokens(
                enquiry.netPayableAmount,
                preferredCurrency
              )}
            </p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Maturity Date</span>
            <span>{new Date(enquiry.maturityDate).toLocaleDateString()}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Tenure</span>
            <span>{enquiry.tenureMonths} months</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Days to Maturity</span>
            <span className="font-medium">{enquiry.daysUntilMaturity}</span>
          </div>
        </CardContent>
      </Card>

      {enquiry.warnings && enquiry.warnings.length > 0 && (
        <Card className="border-yellow-200 bg-yellow-50">
          <CardHeader className="pb-3">
            <CardTitle className="text-sm">Warnings</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="text-sm space-y-1 text-yellow-900">
              {enquiry.warnings.map((warning, idx) => (
                <li key={idx}>• {warning}</li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}

      <div className="flex gap-2 justify-end pt-4">
        <Button
          variant="outline"
          onClick={() => navigate(`/accounts/${id}`)}
          disabled={isProcessing}
        >
          Cancel
        </Button>
        <Button
          onClick={() => setShowConfirmDialog(true)}
          disabled={isProcessing}
        >
          {isProcessing ? "Processing..." : "Redeem"}
        </Button>
      </div>

      <AlertDialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Confirm Redemption</AlertDialogTitle>
            <AlertDialogDescription>
              <div className="space-y-2">
                <p>
                  You will receive{" "}
                  <span className="font-bold text-primary">
                    {formatTokens(enquiry.netPayableAmount)} CCHD
                  </span>
                </p>
                {isPremature && (
                  <p className="text-xs text-red-600">
                    Penalty: -{formatTokens(enquiry.penaltyAmount)} CCHD
                  </p>
                )}
              </div>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isProcessing}>
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleRedemptionProcess}
              disabled={isProcessing}
            >
              {isProcessing ? "Processing..." : "Confirm"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
