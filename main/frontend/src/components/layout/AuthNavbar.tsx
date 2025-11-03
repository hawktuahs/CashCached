import { Link } from "react-router";
import { Button } from "@/components/ui/button";
import { CreditCard } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useI18n } from "@/context/I18nContext";

interface AuthNavbarProps {
  showAuthButtons?: boolean;
}

export function AuthNavbar({ showAuthButtons = true }: AuthNavbarProps) {
  const { isAuthenticated } = useAuth();
  const { t, lang, setLang } = useI18n();

  return (
    <nav className="fixed top-0 w-full bg-background/80 backdrop-blur-md border-b border-border z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <div className="w-10 h-10 bg-primary rounded-lg flex items-center justify-center">
            <CreditCard className="w-6 h-6 text-primary-foreground" />
          </div>
          <span className="text-2xl font-bold text-foreground">
            {t("app.name")}
          </span>
        </Link>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 border-l border-border pl-4">
            <button
              onClick={() => setLang("en")}
              className={`px-3 py-1 rounded text-sm font-medium transition ${
                lang === "en"
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              EN
            </button>
            <button
              onClick={() => setLang("ja")}
              className={`px-3 py-1 rounded text-sm font-medium transition ${
                lang === "ja"
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:text-foreground"
              }`}
            >
              JA
            </button>
          </div>
          {showAuthButtons && (
            <>
              {isAuthenticated ? (
                <Link to="/dashboard">
                  <Button>{t("landing.nav.dashboard")}</Button>
                </Link>
              ) : (
                <>
                  <Link to="/login">
                    <Button variant="ghost">{t("landing.nav.signIn")}</Button>
                  </Link>
                  <Link to="/register">
                    <Button>{t("landing.nav.getStarted")}</Button>
                  </Link>
                </>
              )}
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
