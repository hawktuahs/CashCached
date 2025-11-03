import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { CreditCard, Eye, EyeOff } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";
import { AuthNavbar } from "@/components/layout/AuthNavbar";

const loginSchema = z.object({
  username: z.string().min(3, "Username must be at least 3 characters"),
  password: z.string().min(6, "Password must be at least 6 characters"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function Login() {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const { login, verifyOtp } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [otpMode, setOtpMode] = useState(false);
  const [otpUser, setOtpUser] = useState("");
  const [otpCode, setOtpCode] = useState("");

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    try {
      await login(data.username, data.password);
      toast.success("Login successful!");
      navigate("/dashboard");
    } catch (e: any) {
      const msg = String(e?.message || "");
      if (msg.startsWith("OTP_REQUIRED:")) {
        const user = msg.split(":")[1] || data.username;
        setOtpUser(user);
        setOtpMode(true);
        toast(t("auth.otp.sent"), {
          description: t("auth.otp.sentDesc"),
        });
      } else {
        toast.error("Invalid username or password");
      }
    } finally {
      setIsLoading(false);
    }
  };

  const onVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpCode || !otpUser) return;
    setIsLoading(true);
    try {
      await verifyOtp(otpUser, otpCode);
      toast.success("Login successful!");
      navigate("/dashboard");
    } catch (error: any) {
      const errorMessage =
        error?.response?.data?.message ||
        error?.message ||
        "Invalid or expired OTP";
      toast.error(errorMessage);
      setOtpCode("");
    } finally {
      setIsLoading(false);
    }
  };

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
              {!otpMode ? t("auth.login.welcome") : t("auth.otp.title")}
            </CardTitle>
            <CardDescription>
              {!otpMode
                ? t("auth.login.subtitle")
                : `${t("auth.otp.subtitle")} ${otpUser}`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {!otpMode ? (
              <Form {...form}>
                <form
                  onSubmit={form.handleSubmit(onSubmit)}
                  className="space-y-4"
                >
                  <FormField
                    control={form.control}
                    name="username"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("auth.login.username")}</FormLabel>
                        <FormControl>
                          <Input
                            placeholder={t("auth.placeholder.username")}
                            {...field}
                            disabled={isLoading}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="password"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{t("auth.login.password")}</FormLabel>
                        <FormControl>
                          <div className="relative">
                            <Input
                              type={showPassword ? "text" : "password"}
                              placeholder={t("auth.placeholder.password")}
                              {...field}
                              disabled={isLoading}
                            />
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                              onClick={() => setShowPassword(!showPassword)}
                              disabled={isLoading}
                              aria-label={
                                showPassword
                                  ? t("auth.hidePassword")
                                  : t("auth.showPassword")
                              }
                            >
                              {showPassword ? (
                                <EyeOff className="h-4 w-4" />
                              ) : (
                                <Eye className="h-4 w-4" />
                              )}
                            </Button>
                          </div>
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                      <>
                        <Spinner className="h-4 w-4 animate-spin" />
                        {t("auth.login.signingIn")}
                      </>
                    ) : (
                      t("auth.login.submit")
                    )}
                  </Button>
                </form>
              </Form>
            ) : (
              <form onSubmit={onVerifyOtp} className="space-y-4">
                <div>
                  <FormLabel>{t("auth.otp.code")}</FormLabel>
                  <Input
                    placeholder={t("auth.otp.placeholder")}
                    value={otpCode}
                    onChange={(e) =>
                      setOtpCode(
                        e.target.value.replace(/[^0-9]/g, "").slice(0, 6)
                      )
                    }
                    disabled={isLoading}
                    className="mt-2"
                  />
                </div>
                <div className="flex gap-2">
                  <Button
                    type="submit"
                    className="flex-1"
                    disabled={isLoading || otpCode.length !== 6}
                  >
                    {isLoading ? (
                      <>
                        <Spinner className="h-4 w-4 animate-spin" />
                        {t("auth.otp.verifying")}
                      </>
                    ) : (
                      t("auth.otp.verify")
                    )}
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => setOtpMode(false)}
                    disabled={isLoading}
                  >
                    {t("auth.otp.backToLogin")}
                  </Button>
                </div>
              </form>
            )}
            <div className="mt-6 text-center text-sm">
              {t("auth.login.noAccount")}{" "}
              <Link
                to="/register"
                className="font-medium text-primary hover:underline"
              >
                {t("auth.login.signUp")}
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
