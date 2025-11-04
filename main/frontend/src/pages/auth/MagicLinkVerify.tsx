import { useEffect, useState, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { toast } from "sonner";
import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";
import { AuthNavbar } from "@/components/layout/AuthNavbar";
import { Spinner } from "@/components/ui/spinner";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { CreditCard } from "lucide-react";

export function MagicLinkVerify() {
  const [searchParams] = useSearchParams();
  const [isLoading, setIsLoading] = useState(true);
  const { verifyMagicLink } = useAuth();
  const navigate = useNavigate();
  const { t } = useI18n();
  const hasVerifiedRef = useRef(false);

  const token = searchParams.get("token");

  useEffect(() => {
    if (hasVerifiedRef.current) {
      return;
    }

    const verify = async () => {
      if (!token) {
        toast.error(t("auth.magicLink.noToken"));
        navigate("/login");
        return;
      }

      try {
        hasVerifiedRef.current = true;
        await verifyMagicLink(token);
        toast.success(t("auth.login.submit"));
        navigate("/dashboard");
      } catch (error) {
        hasVerifiedRef.current = false;
        const errorMessage =
          (error as { response?: { data?: { message?: string } } })?.response
            ?.data?.message ||
          (error as { message?: string })?.message ||
          t("auth.magicLink.invalidOrExpired");
        toast.error(errorMessage);
        navigate("/login");
      } finally {
        setIsLoading(false);
      }
    };

    verify();
  }, [token, verifyMagicLink, navigate, t]);

  return (
    <>
      <AuthNavbar showAuthButtons={false} />
      <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-primary/5 via-background to-secondary/5 p-4 pt-24">
        <Card className="w-full max-w-md">
          <CardHeader className="space-y-1 text-center">
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-primary text-primary-foreground">
              <CreditCard className="h-6 w-6" />
            </div>
            <CardTitle className="text-2xl font-bold">
              {t("auth.magicLink.verifying")}
            </CardTitle>
            <CardDescription>
              {t("auth.magicLink.verifyingMsg")}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Spinner className="h-8 w-8 animate-spin text-primary" />
            {isLoading && (
              <p className="mt-4 text-sm text-muted-foreground">
                {t("auth.magicLink.verifyingMsg2")}
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  );
}
