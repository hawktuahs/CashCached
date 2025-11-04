import { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CreditCard, Eye, EyeOff, Mail } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";
import { AuthNavbar } from "@/components/layout/AuthNavbar";

const loginSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
  password: z.string().min(6, "Password must be at least 6 characters"),
});

const magicLinkSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
});

type LoginFormData = z.infer<typeof loginSchema>;
type MagicLinkFormData = z.infer<typeof magicLinkSchema>;

export function Login() {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const { login, verifyOtp, requestMagicLink, verifyMagicLink } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [otpMode, setOtpMode] = useState(false);
  const [otpUser, setOtpUser] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [magicLinkSent, setMagicLinkSent] = useState(false);
  const [verifyingMagicLink, setVerifyingMagicLink] = useState(false);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const magicLinkForm = useForm<MagicLinkFormData>({
    resolver: zodResolver(magicLinkSchema),
    defaultValues: {
      email: "",
    },
  });

  const magicLinkToken = searchParams.get("token");

  useEffect(() => {
    if (magicLinkToken && !verifyingMagicLink) {
      setVerifyingMagicLink(true);
      const verifyToken = async () => {
        try {
          await verifyMagicLink(magicLinkToken);
          toast.success("Login successful!");
          navigate("/dashboard");
        } catch (error) {
          const errorMessage =
            (error as { response?: { data?: { message?: string } } })?.response
              ?.data?.message ||
            (error as { message?: string })?.message ||
            "Invalid or expired magic link";
          toast.error(errorMessage);
          navigate("/login");
        }
      };
      verifyToken();
    }
  }, [magicLinkToken, verifyMagicLink, navigate, verifyingMagicLink]);

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    try {
      await login(data.email, data.password);
      toast.success("Login successful!");
      navigate("/dashboard");
    } catch (e) {
      const msg = String((e as { message?: string })?.message || "");
      if (msg.startsWith("OTP_REQUIRED:")) {
        const user = msg.split(":")[1] || data.email;
        setOtpUser(user);
        setOtpMode(true);
        setTimeout(() => {
          toast(t("auth.otp.sent"), {
            description: t("auth.otp.sentDesc"),
          });
        }, 0);
      } else {
        const errorMessage =
          (e as { response?: { data?: { message?: string } } })?.response?.data
            ?.message ||
          (e as { message?: string })?.message ||
          "Invalid email or password";
        toast.error(errorMessage);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const onMagicLinkSubmit = async (data: MagicLinkFormData) => {
    setIsLoading(true);
    try {
      await requestMagicLink(data.email);
      setMagicLinkSent(true);
      toast.success("Magic link sent! Check your email.");
    } catch (error) {
      const errorMessage =
        (error as { response?: { data?: { message?: string } } })?.response
          ?.data?.message ||
        (error as { message?: string })?.message ||
        "Failed to send magic link";
      toast.error(errorMessage);
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
    } catch (error) {
      const errorMessage =
        (error as { response?: { data?: { message?: string } } })?.response
          ?.data?.message ||
        (error as { message?: string })?.message ||
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
            {magicLinkToken ? (
              <>
                <CardTitle className="text-2xl font-bold">
                  {t("auth.magicLink.verifying")}
                </CardTitle>
                <CardDescription>
                  {t("auth.magicLink.verifyingMsg")}
                </CardDescription>
              </>
            ) : otpMode ? (
              <>
                <CardTitle className="text-2xl font-bold">
                  {t("auth.otp.title")}
                </CardTitle>
                <CardDescription>
                  {`${t("auth.otp.subtitle")} ${otpUser}`}
                </CardDescription>
              </>
            ) : (
              <>
                <CardTitle className="text-2xl font-bold">
                  {t("auth.login.welcome")}
                </CardTitle>
                <CardDescription>{t("auth.login.subtitle")}</CardDescription>
              </>
            )}
          </CardHeader>
          <CardContent>
            {magicLinkToken ? (
              <div className="text-center space-y-4">
                <Spinner className="h-8 w-8 animate-spin mx-auto" />
                <p className="text-sm text-muted-foreground">
                  {t("auth.magicLink.verifyingMsg2")}
                </p>
              </div>
            ) : otpMode ? (
              <form onSubmit={onVerifyOtp} className="space-y-4">
                <div>
                  <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                    {t("auth.otp.code")}
                  </label>
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
            ) : (
              <Tabs defaultValue="password" className="w-full">
                <TabsList className="grid w-full grid-cols-2 mb-4">
                  <TabsTrigger value="password">
                    {t("auth.tabs.password")}
                  </TabsTrigger>
                  <TabsTrigger value="magic-link">
                    {t("auth.tabs.magicLink")}
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="password">
                  <Form {...form}>
                    <form
                      onSubmit={form.handleSubmit(onSubmit)}
                      className="space-y-4"
                    >
                      <FormField
                        control={form.control}
                        name="email"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel htmlFor="login-email">
                              {t("auth.field.email")}
                            </FormLabel>
                            <FormControl>
                              <Input
                                id="login-email"
                                type="email"
                                placeholder={t("auth.placeholder.email")}
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
                            <FormLabel htmlFor="login-password">
                              {t("auth.login.password")}
                            </FormLabel>
                            <FormControl>
                              <div className="relative">
                                <Input
                                  id="login-password"
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
                      <Button
                        type="submit"
                        className="w-full"
                        disabled={isLoading}
                      >
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
                </TabsContent>

                <TabsContent value="magic-link">
                  {magicLinkSent ? (
                    <div className="text-center space-y-4 py-6">
                      <Mail className="h-12 w-12 mx-auto text-primary" />
                      <div>
                        <p className="font-medium">
                          {t("auth.magicLink.sent")}
                        </p>
                        <p className="text-sm text-muted-foreground mt-2">
                          {t("auth.magicLink.subtitle")}
                        </p>
                      </div>
                      <Button
                        variant="outline"
                        onClick={() => {
                          setMagicLinkSent(false);
                          magicLinkForm.reset();
                        }}
                      >
                        {t("auth.magicLink.sendLink")}
                      </Button>
                    </div>
                  ) : (
                    <Form {...magicLinkForm}>
                      <form
                        onSubmit={magicLinkForm.handleSubmit(onMagicLinkSubmit)}
                        className="space-y-4"
                      >
                        <FormField
                          control={magicLinkForm.control}
                          name="email"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel htmlFor="magiclink-email">
                                {t("auth.field.email")}
                              </FormLabel>
                              <FormControl>
                                <Input
                                  id="magiclink-email"
                                  type="email"
                                  placeholder={t("auth.placeholder.email")}
                                  {...field}
                                  disabled={isLoading}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                        <Button
                          type="submit"
                          className="w-full"
                          disabled={isLoading}
                        >
                          {isLoading ? (
                            <>
                              <Spinner className="h-4 w-4 animate-spin" />
                              {t("auth.magicLink.sending")}
                            </>
                          ) : (
                            <>
                              <Mail className="h-4 w-4 mr-2" />
                              {t("auth.magicLink.sendLink")}
                            </>
                          )}
                        </Button>
                      </form>
                    </Form>
                  )}
                </TabsContent>
              </Tabs>
            )}
            {!magicLinkToken && !otpMode && (
              <div className="mt-6 text-center text-sm">
                {t("auth.login.noAccount")}{" "}
                <Link
                  to="/register"
                  className="font-medium text-primary hover:underline"
                >
                  {t("auth.login.signUp")}
                </Link>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </>
  );
}
