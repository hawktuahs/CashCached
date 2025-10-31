import { useCallback, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from "@/components/ui/sidebar";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  LayoutDashboard,
  User,
  Package,
  Calculator,
  CreditCard,
  LogOut,
  Settings,
  Menu,
  X,
  Coins,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";
import { api } from "@/lib/api";

const navigation = [
  {
    title: "Overview",
    items: [
      {
        title: "Dashboard",
        url: "/dashboard",
        icon: LayoutDashboard,
      },
    ],
  },
  {
    title: "Banking",
    items: [
      {
        title: "My Profile",
        url: "/profile",
        icon: User,
      },
      {
        title: "Products & Pricing",
        url: "/products",
        icon: Package,
      },
      {
        title: "FD Calculator",
        url: "/fd-calculator",
        icon: Calculator,
      },
      {
        title: "My Accounts",
        url: "/accounts",
        icon: CreditCard,
      },
    ],
  },
];

type WalletRefreshDetail = {
  customerId?: string;
  balance?: number;
  targetValue?: number;
  currency?: string;
};

function AppSidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();
  const isStaff = user?.role === "ADMIN" || user?.role === "BANKOFFICER";
  const isCustomer = user?.role === "CUSTOMER";
  const { t, lang, setLang } = useI18n();

  const [walletTokens, setWalletTokens] = useState<number>(0);
  const [walletTargetValue, setWalletTargetValue] = useState<number>(0);
  const [walletCurrency, setWalletCurrency] = useState<string>("KWD");
  const [isWalletLoading, setIsWalletLoading] = useState(false);

  const loadWallet = useCallback(async () => {
    if (!isCustomer || !user?.id) {
      setWalletTokens(0);
      setWalletTargetValue(0);
      return;
    }
    setIsWalletLoading(true);
    try {
      const response = await api.get(
        `/api/financials/stablecoin/balance/${user.id}`
      );
      const payload = response?.data?.data ?? response?.data;
      const balance = Number(payload?.balance ?? 0);
      const targetValue = Number(payload?.targetValue ?? 0);
      const currency = String(
        payload?.targetCurrency ?? payload?.baseCurrency ?? "KWD"
      );
      setWalletTokens(Number.isFinite(balance) ? balance : 0);
      setWalletTargetValue(
        Number.isFinite(targetValue) ? targetValue : balance
      );
      setWalletCurrency(currency);
    } catch {
      setWalletTokens(0);
      setWalletTargetValue(0);
    } finally {
      setIsWalletLoading(false);
    }
  }, [isCustomer, user?.id]);

  useEffect(() => {
    loadWallet();
  }, [loadWallet]);

  useEffect(() => {
    if (!isCustomer || !user?.id) {
      return;
    }
    const handler = (event: Event) => {
      const custom = event as CustomEvent<WalletRefreshDetail>;
      const detail = custom.detail;
      if (detail?.customerId && detail.customerId !== user.id) {
        loadWallet();
        return;
      }
      if (detail && typeof detail.balance === "number") {
        setWalletTokens(detail.balance);
        if (typeof detail.targetValue === "number") {
          setWalletTargetValue(detail.targetValue);
        }
        if (detail.currency) {
          setWalletCurrency(detail.currency);
        }
        return;
      }
      loadWallet();
    };
    window.addEventListener(
      "cashcached:refresh-wallet",
      handler as EventListener
    );
    return () => {
      window.removeEventListener(
        "cashcached:refresh-wallet",
        handler as EventListener
      );
    };
  }, [isCustomer, loadWallet, user?.id]);

  const formatConverted = (value: number, currency: string) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      maximumFractionDigits: 2,
    }).format(value);

  return (
    <Sidebar>
      <SidebarHeader className="border-b border-sidebar-border">
        <div className="flex items-center gap-2 px-4 py-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <CreditCard className="h-4 w-4" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-semibold">CashCached</span>
            <span className="text-xs text-muted-foreground">
              {t("brand.subtitle")}
            </span>
          </div>
        </div>
      </SidebarHeader>

      <SidebarContent>
        {isCustomer && (
          <SidebarGroup>
            <SidebarGroupLabel>CashCached</SidebarGroupLabel>
            <SidebarGroupContent>
              <div className="space-y-3 rounded-lg border border-sidebar-border bg-muted/20 p-3">
                <div className="flex items-center justify-between">
                  <div className="text-sm font-medium">
                    {t("sidebar.wallet.title")}
                  </div>
                  <Coins className="h-4 w-4" />
                </div>
                <div>
                  <div className="text-2xl font-semibold">
                    {isWalletLoading
                      ? "—"
                      : walletTokens.toLocaleString(undefined, {
                          maximumFractionDigits: 0,
                        })}{" "}
                    CCHD
                  </div>
                  <div className="text-xs text-muted-foreground">
                    {isWalletLoading
                      ? t("sidebar.wallet.loading")
                      : formatConverted(walletTargetValue, walletCurrency)}
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="default"
                    size="sm"
                    className="flex-1"
                    onClick={() => navigate("/accounts")}
                  >
                    {t("sidebar.wallet.depositWithdraw")}
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1"
                    onClick={() => navigate("/financials/stablecoin")}
                  >
                    {t("sidebar.wallet.history")}
                  </Button>
                </div>
              </div>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
        {navigation.map((group) => (
          <SidebarGroup key={group.title}>
            <SidebarGroupLabel>
              {group.title === "Overview"
                ? t("nav.overview")
                : group.title === "Banking"
                ? t("nav.banking")
                : group.title}
            </SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {group.items.map((item) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      onClick={() => navigate(item.url)}
                      isActive={location.pathname === item.url}
                      className="w-full justify-start"
                    >
                      <item.icon className="h-4 w-4 shrink-0" />
                      <span className="truncate max-w-[160px]">
                        {item.title === "Dashboard"
                          ? t("nav.dashboard")
                          : item.title === "My Profile"
                          ? t("nav.profile")
                          : item.title === "Products & Pricing"
                          ? t("nav.products")
                          : item.title === "FD Calculator"
                          ? t("nav.calculator")
                          : item.title === "My Accounts"
                          ? t("nav.accounts")
                          : item.title}
                      </span>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
        {isStaff && (
          <SidebarGroup>
            <SidebarGroupLabel>{t("nav.admin")}</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    onClick={() => navigate("/admin")}
                    isActive={location.pathname === "/admin"}
                    className="w-full justify-start"
                  >
                    <LayoutDashboard className="h-4 w-4" />
                    <span>{t("nav.admin.dashboard")}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    onClick={() => navigate("/financials/stablecoin")}
                    isActive={location.pathname === "/financials/stablecoin"}
                    className="w-full justify-start"
                  >
                    <Coins className="h-4 w-4" />
                    <span>CashCached</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        )}
      </SidebarContent>

      <SidebarFooter className="border-t border-sidebar-border p-4">
        <div className="flex items-center gap-3 min-w-0 flex-nowrap justify-between w-full overflow-hidden">
          <div className="flex items-center gap-3 min-w-0">
            <Avatar className="h-8 w-8">
              <AvatarImage src="" alt={user?.firstName} />
              <AvatarFallback>
                {user?.firstName?.[0]}
                {user?.lastName?.[0]}
              </AvatarFallback>
            </Avatar>
            <div className="flex flex-1 flex-col gap-1 min-w-0 pr-20">
              <div className="flex items-center gap-2 min-w-0">
                <span
                  className="text-sm font-medium truncate max-w-[110px]"
                  title={`${user?.firstName || ""} ${user?.lastName || ""}`}
                >
                  {user?.firstName} {user?.lastName}
                </span>
              </div>
              <span
                className="text-xs text-muted-foreground truncate max-w-[200px] hidden lg:block"
                title={user?.email || ""}
              >
                {user?.email}
              </span>
            </div>
          </div>
          <div className="flex items-center gap-1 shrink-0 whitespace-nowrap">
            <Button
              variant={lang === "en" ? "default" : "outline"}
              size="sm"
              onClick={() => setLang("en")}
            >
              EN
            </Button>
            <Button
              variant={lang === "ja" ? "default" : "outline"}
              size="sm"
              onClick={() => setLang("ja")}
            >
              日本語
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="shrink-0">
                  <Settings className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel>My Account</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => navigate("/profile")}>
                  <User className="mr-2 h-4 w-4" />
                  {t("nav.profile")}
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={logout}>
                  <LogOut className="mr-2 h-4 w-4" />
                  {t("action.logout")}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </SidebarFooter>
    </Sidebar>
  );
}

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <SidebarProvider>
      <div className="flex h-screen w-full">
        <AppSidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <header className="flex h-16 items-center gap-4 border-b bg-white shadow-sm px-4 lg:hidden">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            >
              {isMobileMenuOpen ? (
                <X className="h-5 w-5" />
              ) : (
                <Menu className="h-5 w-5" />
              )}
            </Button>
            <div className="flex items-center gap-2">
              <div className="flex h-6 w-6 items-center justify-center rounded bg-primary text-primary-foreground">
                <CreditCard className="h-3 w-3" />
              </div>
              <span className="font-semibold">CashCached</span>
            </div>
          </header>
          <main className="flex-1 overflow-auto p-4 lg:p-6">{children}</main>
        </div>
      </div>
    </SidebarProvider>
  );
}
