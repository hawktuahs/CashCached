import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router";
import { AuthProvider } from "./context/AuthContext";
import { I18nProvider } from "./context/I18nContext";
import { Toaster } from "sonner";
import { ThemeProvider } from "next-themes";

import { RequireAuth } from "./components/auth/RequireAuth";
import { ProtectedAuthRoute } from "./components/auth/ProtectedAuthRoute";
import { AppLayout } from "./components/layout/AppLayout";
import { Landing } from "./pages/Landing";
import { Login } from "./pages/auth/Login";
import { Register } from "./pages/auth/Register";
import { Dashboard } from "./pages/Dashboard";
import { CustomerProfile } from "./pages/customer/CustomerProfile";
import { ProductList } from "./pages/products/ProductList";
import { ProductForm } from "./pages/products/ProductForm";
import { FdCalculator } from "./pages/fd/FdCalculator";
import { AccountsList } from "./pages/accounts/AccountsList";
import { AccountDetails } from "./pages/accounts/AccountDetails";
import { AdminDashboard } from "./pages/admin/AdminDashboard";
import { CashCachedDashboard } from "./pages/financials/CashCachedDashboard";
import { ErrorBoundary } from "./components/ErrorBoundary";

function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider
        attribute="class"
        defaultTheme="light"
        enableSystem={false}
      >
        <I18nProvider>
          <AuthProvider>
            <Router>
              <div className="min-h-screen bg-white text-slate-900">
                <Routes>
                  <Route path="/" element={<Landing />} />
                  <Route
                    path="/login"
                    element={
                      <ProtectedAuthRoute>
                        <Login />
                      </ProtectedAuthRoute>
                    }
                  />
                  <Route
                    path="/register"
                    element={
                      <ProtectedAuthRoute>
                        <Register />
                      </ProtectedAuthRoute>
                    }
                  />
                  <Route
                    path="/*"
                    element={
                      <RequireAuth>
                        <AppLayout>
                          <Routes>
                            <Route
                              path="/"
                              element={<Navigate to="/dashboard" replace />}
                            />
                            <Route path="/dashboard" element={<Dashboard />} />
                            <Route
                              path="/profile"
                              element={<CustomerProfile />}
                            />
                            <Route path="/products" element={<ProductList />} />
                            <Route
                              path="/products/new"
                              element={<ProductForm />}
                            />
                            <Route
                              path="/products/:id/edit"
                              element={<ProductForm />}
                            />
                            <Route
                              path="/fd-calculator"
                              element={<FdCalculator />}
                            />
                            <Route
                              path="/accounts"
                              element={<AccountsList />}
                            />
                            <Route
                              path="/accounts/:id"
                              element={<AccountDetails />}
                            />
                            <Route
                              path="/financials/stablecoin"
                              element={<CashCachedDashboard />}
                            />
                            <Route path="/admin" element={<AdminDashboard />} />
                          </Routes>
                        </AppLayout>
                      </RequireAuth>
                    }
                  />
                </Routes>
                <Toaster position="top-right" richColors />
              </div>
            </Router>
          </AuthProvider>
        </I18nProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

export default App;
