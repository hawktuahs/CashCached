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
    <nav className="fixed top-0 w-full bg-white/80 backdrop-blur-md border-b border-slate-200 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2">
          <div className="w-10 h-10 bg-linear-to-br from-slate-800 to-slate-600 rounded-lg flex items-center justify-center">
            <CreditCard className="w-6 h-6 text-slate-100" />
          </div>
          <span className="text-2xl font-bold text-slate-900">
            {t("app.name")}
          </span>
        </Link>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 border-l border-slate-300 pl-4">
            <button
              onClick={() => setLang("en")}
              className={`px-3 py-1 rounded text-sm font-medium transition ${
                lang === "en"
                  ? "bg-slate-900 text-white"
                  : "text-slate-700 hover:text-slate-900"
              }`}
            >
              EN
            </button>
            <button
              onClick={() => setLang("ja")}
              className={`px-3 py-1 rounded text-sm font-medium transition ${
                lang === "ja"
                  ? "bg-slate-900 text-white"
                  : "text-slate-700 hover:text-slate-900"
              }`}
            >
              JA
            </button>
          </div>
          {showAuthButtons && (
            <>
              {isAuthenticated ? (
                <Link to="/dashboard">
                  <Button className="bg-slate-900 hover:bg-slate-800 text-white">
                    {t("landing.nav.dashboard")}
                  </Button>
                </Link>
              ) : (
                <>
                  <Link to="/login">
                    <Button
                      variant="ghost"
                      className="text-slate-700 hover:text-slate-900"
                    >
                      {t("landing.nav.signIn")}
                    </Button>
                  </Link>
                  <Link to="/register">
                    <Button className="bg-slate-900 hover:bg-slate-800 text-white">
                      {t("landing.nav.getStarted")}
                    </Button>
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
