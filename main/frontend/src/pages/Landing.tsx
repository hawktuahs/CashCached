import { Link } from "react-router";
import { Button } from "../components/ui/button";
import { Card } from "../components/ui/card";
import { useAuth } from "../context/AuthContext";
import { useI18n } from "../context/I18nContext";
import { AuthNavbar } from "../components/layout/AuthNavbar";
import {
  ArrowRight,
  TrendingUp,
  Bot,
  Lock,
  Zap,
  Shield,
  CreditCard,
} from "lucide-react";

export function Landing() {
  const { isAuthenticated } = useAuth();
  const { t } = useI18n();

  return (
    <div className="min-h-screen bg-linear-to-b from-slate-50 via-white to-slate-50 text-slate-900">
      <AuthNavbar />

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              <div className="space-y-4">
                <h1 className="text-5xl sm:text-6xl font-bold text-slate-900 leading-tight">
                  {t("landing.hero.title")}
                </h1>
                <p className="text-xl text-slate-600">
                  {t("landing.hero.subtitle")}
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-4">
                {isAuthenticated ? (
                  <Link to="/dashboard">
                    <Button
                      size="lg"
                      className="w-full sm:w-auto bg-slate-900 hover:bg-slate-800 text-white group"
                    >
                      {t("landing.nav.dashboard")}
                      <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </Button>
                  </Link>
                ) : (
                  <Link to="/register">
                    <Button
                      size="lg"
                      className="w-full sm:w-auto bg-slate-900 hover:bg-slate-800 text-white group"
                    >
                      {t("landing.hero.openAccount")}
                      <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </Button>
                  </Link>
                )}
                <Button
                  size="lg"
                  variant="outline"
                  className="w-full sm:w-auto border-slate-300 text-slate-900 hover:bg-slate-50"
                >
                  {t("landing.hero.learnMore")}
                </Button>
              </div>
              <div className="grid grid-cols-3 gap-4 pt-4">
                <div className="border-l-2 border-slate-300 pl-4">
                  <p className="text-2xl font-bold text-slate-900">100%</p>
                  <p className="text-sm text-slate-600">
                    {t("landing.hero.stat1.label")}
                  </p>
                </div>
                <div className="border-l-2 border-slate-300 pl-4">
                  <p className="text-2xl font-bold text-slate-900">24/7</p>
                  <p className="text-sm text-slate-600">
                    {t("landing.hero.stat2.label")}
                  </p>
                </div>
                <div className="border-l-2 border-slate-300 pl-4">
                  <p className="text-2xl font-bold text-slate-900">0%</p>
                  <p className="text-sm text-slate-600">
                    {t("landing.hero.stat3.label")}
                  </p>
                </div>
              </div>
            </div>
            <div className="relative">
              <div className="absolute inset-0 bg-linear-to-tr from-slate-200 to-slate-100 rounded-2xl transform rotate-3 opacity-50" />
              <div className="relative bg-linear-to-br from-slate-100 to-slate-50 rounded-2xl p-8 border border-slate-200 shadow-xl">
                <div className="space-y-6">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-slate-800 rounded-lg flex items-center justify-center">
                      <TrendingUp className="w-6 h-6 text-slate-100" />
                    </div>
                    <div>
                      <p className="text-sm text-slate-600">
                        {t("landing.hero.currentRate")}
                      </p>
                      <p className="text-2xl font-bold text-slate-900">8.5%</p>
                    </div>
                  </div>
                  <div className="bg-white rounded-lg p-4 border border-slate-200">
                    <p className="text-xs text-slate-600 mb-2">
                      {t("landing.hero.investmentOverview")}
                    </p>
                    <div className="space-y-3">
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-700">
                          {t("landing.hero.principal")}
                        </span>
                        <span className="font-semibold text-slate-900">
                          ₹50,000
                        </span>
                      </div>
                      <div className="h-1 bg-slate-200 rounded-full overflow-hidden">
                        <div className="h-full w-3/4 bg-slate-700 rounded-full" />
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-slate-700">
                          {t("landing.hero.maturity")}
                        </span>
                        <span className="font-semibold text-slate-900">
                          ₹54,250
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-white border-y border-slate-200">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-slate-900 mb-4">
              {t("landing.features.title")}
            </h2>
            <p className="text-xl text-slate-600 max-w-2xl mx-auto">
              {t("landing.features.subtitle")}
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            <FeatureCard
              icon={<Shield className="w-6 h-6" />}
              title={t("landing.features.security")}
              description={t("landing.features.security.desc")}
            />
            <FeatureCard
              icon={<Bot className="w-6 h-6" />}
              title={t("landing.features.ai")}
              description={t("landing.features.ai.desc")}
            />
            <FeatureCard
              icon={<TrendingUp className="w-6 h-6" />}
              title={t("landing.features.calculation")}
              description={t("landing.features.calculation.desc")}
            />
            <FeatureCard
              icon={<Zap className="w-6 h-6" />}
              title={t("landing.features.instant")}
              description={t("landing.features.instant.desc")}
            />
            <FeatureCard
              icon={<Lock className="w-6 h-6" />}
              title={t("landing.features.security2")}
              description={t("landing.features.security2.desc")}
            />
            <FeatureCard
              icon={<CreditCard className="w-6 h-6" />}
              title={t("landing.features.transparent")}
              description={t("landing.features.transparent.desc")}
            />
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-slate-900 mb-4">
              {t("landing.steps.title")}
            </h2>
            <p className="text-xl text-slate-600">
              {t("landing.steps.subtitle")}
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            <StepCard
              number="1"
              title={t("landing.steps.1.title")}
              description={t("landing.steps.1.desc")}
            />
            <StepCard
              number="2"
              title={t("landing.steps.2.title")}
              description={t("landing.steps.2.desc")}
            />
            <StepCard
              number="3"
              title={t("landing.steps.3.title")}
              description={t("landing.steps.3.desc")}
            />
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-slate-900 text-slate-100">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 text-center">
            <div>
              <p className="text-4xl font-bold mb-2">50K+</p>
              <p className="text-slate-400">{t("landing.stats.users")}</p>
            </div>
            <div>
              <p className="text-4xl font-bold mb-2">₹500Cr+</p>
              <p className="text-slate-400">{t("landing.stats.aum")}</p>
            </div>
            <div>
              <p className="text-4xl font-bold mb-2">99.9%</p>
              <p className="text-slate-400">{t("landing.stats.uptime")}</p>
            </div>
            <div>
              <p className="text-4xl font-bold mb-2">4.9/5</p>
              <p className="text-slate-400">{t("landing.stats.rating")}</p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <Card className="bg-linear-to-r from-slate-50 to-slate-100 border-slate-200 p-12 text-center space-y-6">
            <h2 className="text-4xl font-bold text-slate-900">
              {isAuthenticated
                ? t("landing.cta.title.auth")
                : t("landing.cta.title.guest")}
            </h2>
            <p className="text-lg text-slate-700">
              {isAuthenticated
                ? t("landing.cta.subtitle.auth")
                : t("landing.cta.subtitle.guest")}
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
              {isAuthenticated ? (
                <Link to="/dashboard">
                  <Button
                    size="lg"
                    className="w-full sm:w-auto bg-slate-900 hover:bg-slate-800 text-white"
                  >
                    {t("landing.nav.dashboard")}
                  </Button>
                </Link>
              ) : (
                <>
                  <Link to="/register">
                    <Button
                      size="lg"
                      className="w-full sm:w-auto bg-slate-900 hover:bg-slate-800 text-white"
                    >
                      {t("landing.cta.start")}
                    </Button>
                  </Link>
                  <Button
                    size="lg"
                    variant="outline"
                    className="w-full sm:w-auto border-slate-300 text-slate-900 hover:bg-slate-50"
                  >
                    {t("landing.cta.demo")}
                  </Button>
                </>
              )}
            </div>
          </Card>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-400 py-12 px-4 sm:px-6 lg:px-8 border-t border-slate-800">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 bg-slate-700 rounded-lg flex items-center justify-center">
                  <CreditCard className="w-5 h-5 text-slate-200" />
                </div>
                <span className="font-bold text-slate-100">
                  {t("app.name")}
                </span>
              </div>
              <p className="text-sm text-slate-500">
                {t("landing.footer.tagline")}
              </p>
            </div>
            <div>
              <h4 className="font-semibold text-slate-100 mb-4">
                {t("landing.footer.product")}
              </h4>
              <ul className="space-y-2 text-sm">
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.features")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.pricing")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.security")}
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-slate-100 mb-4">
                {t("landing.footer.company")}
              </h4>
              <ul className="space-y-2 text-sm">
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.about")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.blog")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.careers")}
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-slate-100 mb-4">
                {t("landing.footer.legal")}
              </h4>
              <ul className="space-y-2 text-sm">
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.privacy")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.terms")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-slate-300 transition">
                    {t("landing.footer.contact")}
                  </a>
                </li>
              </ul>
            </div>
          </div>
          <div className="border-t border-slate-800 pt-8">
            <p className="text-center text-sm text-slate-500">
              {t("landing.footer.copyright")}
            </p>
          </div>
        </div>
      </footer>
    </div>
  );
}

interface FeatureCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
}

function FeatureCard({ icon, title, description }: FeatureCardProps) {
  return (
    <Card className="p-6 border border-slate-200 hover:border-slate-300 hover:shadow-lg transition-all group">
      <div className="w-12 h-12 bg-slate-900 text-slate-100 rounded-lg flex items-center justify-center mb-4 group-hover:bg-slate-800 transition">
        {icon}
      </div>
      <h3 className="text-lg font-semibold text-slate-900 mb-2">{title}</h3>
      <p className="text-slate-600">{description}</p>
    </Card>
  );
}

interface StepCardProps {
  number: string;
  title: string;
  description: string;
}

function StepCard({ number, title, description }: StepCardProps) {
  return (
    <div className="relative">
      <div className="absolute -top-8 -left-4 w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center text-2xl font-bold text-slate-900 border-4 border-white">
        {number}
      </div>
      <Card className="pt-12 p-6 border border-slate-200">
        <h3 className="text-lg font-semibold text-slate-900 mb-2">{title}</h3>
        <p className="text-slate-600">{description}</p>
      </Card>
    </div>
  );
}
