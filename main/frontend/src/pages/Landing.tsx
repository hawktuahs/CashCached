import { Link } from "react-router";
import { Button } from "../components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "../components/ui/card";
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
    <div className="min-h-screen bg-background">
      <AuthNavbar />

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              <div className="space-y-4">
                <h1 className="text-5xl sm:text-6xl font-bold text-foreground leading-tight">
                  {t("landing.hero.title")}
                </h1>
                <p className="text-xl text-muted-foreground">
                  {t("landing.hero.subtitle")}
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-4">
                {isAuthenticated ? (
                  <Link to="/dashboard">
                    <Button size="lg" className="w-full sm:w-auto group">
                      {t("landing.nav.dashboard")}
                      <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </Button>
                  </Link>
                ) : (
                  <Link to="/register">
                    <Button size="lg" className="w-full sm:w-auto group">
                      {t("landing.hero.openAccount")}
                      <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
                    </Button>
                  </Link>
                )}
                <Button
                  size="lg"
                  variant="outline"
                  className="w-full sm:w-auto"
                >
                  {t("landing.hero.learnMore")}
                </Button>
              </div>
              <div className="grid grid-cols-3 gap-4 pt-4">
                <div className="border-l-2 border-border pl-4">
                  <p className="text-2xl font-bold text-foreground">100%</p>
                  <p className="text-sm text-muted-foreground">
                    {t("landing.hero.stat1.label")}
                  </p>
                </div>
                <div className="border-l-2 border-border pl-4">
                  <p className="text-2xl font-bold text-foreground">24/7</p>
                  <p className="text-sm text-muted-foreground">
                    {t("landing.hero.stat2.label")}
                  </p>
                </div>
                <div className="border-l-2 border-border pl-4">
                  <p className="text-2xl font-bold text-foreground">0%</p>
                  <p className="text-sm text-muted-foreground">
                    {t("landing.hero.stat3.label")}
                  </p>
                </div>
              </div>
            </div>
            <div className="relative">
              <div className="absolute inset-0 bg-accent rounded-2xl transform rotate-3 opacity-10" />
              <Card className="relative">
                <CardContent className="p-8">
                  <div className="space-y-6">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-primary rounded-lg flex items-center justify-center">
                        <TrendingUp className="w-6 h-6 text-primary-foreground" />
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">
                          {t("landing.hero.currentRate")}
                        </p>
                        <p className="text-2xl font-bold text-foreground">
                          8.5%
                        </p>
                      </div>
                    </div>
                    <Card className="bg-secondary/20">
                      <CardContent className="p-4 space-y-3">
                        <p className="text-xs text-muted-foreground">
                          {t("landing.hero.investmentOverview")}
                        </p>
                        <div className="flex justify-between text-sm">
                          <span className="text-foreground">
                            {t("landing.hero.principal")}
                          </span>
                          <span className="font-semibold text-foreground">
                            ₹50,000
                          </span>
                        </div>
                        <div className="h-1 bg-muted rounded-full overflow-hidden">
                          <div className="h-full w-3/4 bg-primary rounded-full" />
                        </div>
                        <div className="flex justify-between text-sm">
                          <span className="text-foreground">
                            {t("landing.hero.maturity")}
                          </span>
                          <span className="font-semibold text-foreground">
                            ₹54,250
                          </span>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-secondary/50 border-y border-border">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-4xl font-bold text-foreground mb-4">
              {t("landing.features.title")}
            </h2>
            <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
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
            <h2 className="text-4xl font-bold text-foreground mb-4">
              {t("landing.steps.title")}
            </h2>
            <p className="text-xl text-muted-foreground">
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
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-primary text-primary-foreground">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 text-center">
            <div>
              <p className="text-4xl font-bold mb-2">50K+</p>
              <p className="text-primary-foreground/80">
                {t("landing.stats.users")}
              </p>
            </div>
            <div>
              <p className="text-4xl font-bold mb-2">₹500Cr+</p>
              <p className="text-primary-foreground/80">
                {t("landing.stats.aum")}
              </p>
            </div>
            <div>
              <p className="text-4xl font-bold mb-2">99.9%</p>
              <p className="text-primary-foreground/80">
                {t("landing.stats.uptime")}
              </p>
            </div>
            <div>
              <p className="text-4xl font-bold mb-2">4.9/5</p>
              <p className="text-primary-foreground/80">
                {t("landing.stats.rating")}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <Card className="bg-secondary/50 border-border">
            <CardHeader className="text-center space-y-4">
              <CardTitle className="text-4xl text-foreground">
                {isAuthenticated
                  ? t("landing.cta.title.auth")
                  : t("landing.cta.title.guest")}
              </CardTitle>
              <CardDescription className="text-lg text-foreground/80">
                {isAuthenticated
                  ? t("landing.cta.subtitle.auth")
                  : t("landing.cta.subtitle.guest")}
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col sm:flex-row gap-4 justify-center">
              {isAuthenticated ? (
                <Link to="/dashboard">
                  <Button size="lg" className="w-full sm:w-auto">
                    {t("landing.nav.dashboard")}
                  </Button>
                </Link>
              ) : (
                <>
                  <Link to="/register">
                    <Button size="lg" className="w-full sm:w-auto">
                      {t("landing.cta.start")}
                    </Button>
                  </Link>
                  <Button
                    size="lg"
                    variant="outline"
                    className="w-full sm:w-auto"
                  >
                    {t("landing.cta.demo")}
                  </Button>
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-secondary border-t border-border py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="grid md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
                  <CreditCard className="w-5 h-5 text-primary-foreground" />
                </div>
                <span className="font-bold text-foreground">
                  {t("app.name")}
                </span>
              </div>
              <p className="text-sm text-muted-foreground">
                {t("landing.footer.tagline")}
              </p>
            </div>
            <div>
              <h4 className="font-semibold text-foreground mb-4">
                {t("landing.footer.product")}
              </h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.features")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.pricing")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.security")}
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-foreground mb-4">
                {t("landing.footer.company")}
              </h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.about")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.blog")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.careers")}
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-foreground mb-4">
                {t("landing.footer.legal")}
              </h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.privacy")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.terms")}
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground transition">
                    {t("landing.footer.contact")}
                  </a>
                </li>
              </ul>
            </div>
          </div>
          <div className="border-t border-border pt-8">
            <p className="text-center text-sm text-muted-foreground">
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
    <Card className="border-border hover:border-primary/50 hover:shadow-lg transition-all group">
      <CardHeader className="pb-3">
        <div className="w-12 h-12 bg-primary text-primary-foreground rounded-lg flex items-center justify-center mb-3 group-hover:shadow-lg transition">
          {icon}
        </div>
        <CardTitle className="text-lg text-foreground">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-muted-foreground">{description}</p>
      </CardContent>
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
      <div className="absolute -top-8 -left-4 w-16 h-16 bg-primary text-primary-foreground rounded-full flex items-center justify-center text-2xl font-bold border-4 border-background">
        {number}
      </div>
      <Card className="pt-12 border-border">
        <CardHeader>
          <CardTitle className="text-lg text-foreground">{title}</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">{description}</p>
        </CardContent>
      </Card>
    </div>
  );
}
