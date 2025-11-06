import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { api } from "@/lib/api";

interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  username?: string;
  role: "CUSTOMER" | "BANKOFFICER" | "ADMIN";
  preferredCurrency?: string;
  address?: string;
  aadhaarNumber?: string;
  panNumber?: string;
  dateOfBirth?: string;
  classification?: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  register: (userData: RegisterData) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
  isAuthenticated: boolean;
  verifyOtp: (username: string, code: string) => Promise<void>;
  requestMagicLink: (email: string) => Promise<void>;
  verifyMagicLink: (token: string) => Promise<void>;
  refreshProfile: () => Promise<void>;
  preferredCurrency: string;
  invalidateSession: () => void;
}

interface RegisterData {
  email: string;
  password: string;
  username?: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address?: string;
  dateOfBirth?: string;
  aadhaarNumber?: string;
  panNumber?: string;
  preferredCurrency?: string;
  role: "CUSTOMER" | "BANKOFFICER" | "ADMIN";
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(
    localStorage.getItem("token")
  );
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user && !!token;

  const hydrateProfile = async () => {
    try {
      const response = await api.get("/api/customer/profile");
      const payload = response?.data || response;
      setUser((current) => {
        if (!current) {
          return null;
        }
        const resolvedId = payload?.id ?? payload?.customerId ?? current.id;
        const fullName = String(payload?.fullName ?? "").trim();
        const [first, ...rest] = fullName
          ? fullName.split(" ")
          : [current.firstName, current.lastName];
        return {
          ...current,
          id: resolvedId ? String(resolvedId) : current.id,
          email: payload?.email || current.email || "",
          firstName: first || current.firstName,
          lastName: rest.length > 0 ? rest.join(" ") : current.lastName,
          preferredCurrency:
            payload?.preferredCurrency || current.preferredCurrency || "KWD",
        };
      });
    } catch {
      // Session hydration failed, user will be cleared on refresh
    }
  };

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem("token");
      const storedRole = localStorage.getItem("userRole");
      if (storedToken) {
        try {
          setToken(storedToken);
          api.defaults.headers.common[
            "Authorization"
          ] = `Bearer ${storedToken}`;
          setUser({
            id: "",
            email: "",
            firstName: "",
            lastName: "",
            role:
              (storedRole as "CUSTOMER" | "BANKOFFICER" | "ADMIN") ||
              "CUSTOMER",
            preferredCurrency: "KWD",
          });
          await hydrateProfile();
        } catch {
          localStorage.removeItem("token");
          localStorage.removeItem("userRole");
          setToken(null);
        }
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const response = await api.post("/api/auth/login", { email, password });
    if (response?.data?.twoFactorRequired) {
      throw new Error(`OTP_REQUIRED:${email}`);
    }
    const { token: sessionId, role } = response.data;
    localStorage.setItem("token", sessionId);
    localStorage.setItem("userRole", role || "CUSTOMER");
    setToken(sessionId);
    api.defaults.headers.common["Authorization"] = `Bearer ${sessionId}`;
    setUser({
      id: "",
      email: email,
      firstName: "",
      lastName: "",
      role: (role as "CUSTOMER" | "BANKOFFICER" | "ADMIN") || "CUSTOMER",
      preferredCurrency: "KWD",
    });
    await hydrateProfile();
  };

  const verifyOtp = async (email: string, code: string) => {
    const response = await api.post("/api/auth/verify-otp", { email, code });
    const { token: sessionId, role } = response.data;
    localStorage.setItem("token", sessionId);
    localStorage.setItem("userRole", role || "CUSTOMER");
    setToken(sessionId);
    api.defaults.headers.common["Authorization"] = `Bearer ${sessionId}`;
    setUser({
      id: "",
      email: email,
      firstName: "",
      lastName: "",
      role: (role as "CUSTOMER" | "BANKOFFICER" | "ADMIN") || "CUSTOMER",
    });
    await hydrateProfile();
  };

  const requestMagicLink = async (email: string) => {
    await api.post("/api/auth/magic-link/request", { email });
  };

  const verifyMagicLink = async (token: string) => {
    const response = await api.post(
      "/api/auth/magic-link/verify",
      {},
      { params: { token } }
    );
    const { token: sessionId, email, role } = response.data;
    localStorage.setItem("token", sessionId);
    localStorage.setItem("userRole", role || "CUSTOMER");
    setToken(sessionId);
    api.defaults.headers.common["Authorization"] = `Bearer ${sessionId}`;
    setUser({
      id: "",
      email: email || "",
      firstName: "",
      lastName: "",
      role: (role as "CUSTOMER" | "BANKOFFICER" | "ADMIN") || "CUSTOMER",
      preferredCurrency: "KWD",
    });
    await hydrateProfile();
  };

  const register = async (userData: RegisterData) => {
    try {
      const payload = {
        password: userData.password,
        fullName: `${userData.firstName} ${userData.lastName}`,
        email: userData.email,
        username: userData.username,
        phoneNumber: userData.phoneNumber,
        address: userData.address,
        dateOfBirth: userData.dateOfBirth,
        aadhaarNumber: userData.aadhaarNumber,
        panNumber: userData.panNumber,
        preferredCurrency: userData.preferredCurrency || "KWD",
        role: userData.role,
      };
      const response = await api.post("/api/auth/register", payload);
      const { token: sessionId, role: responseRole } = response.data;

      localStorage.setItem("token", sessionId);
      localStorage.setItem("userRole", responseRole || "CUSTOMER");
      setToken(sessionId);
      api.defaults.headers.common["Authorization"] = `Bearer ${sessionId}`;

      setUser({
        id: "",
        email: userData.email,
        firstName: userData.firstName,
        lastName: userData.lastName,
        role:
          (responseRole as "CUSTOMER" | "BANKOFFICER" | "ADMIN") ||
          userData.role,
        preferredCurrency: userData.preferredCurrency || "KWD",
        address: userData.address,
        aadhaarNumber: userData.aadhaarNumber,
        panNumber: userData.panNumber,
        dateOfBirth: userData.dateOfBirth,
        username: userData.username,
      });
    } catch {
      throw new Error("Registration failed");
    }
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    setToken(null);
    setUser(null);
    delete api.defaults.headers.common["Authorization"];
  };

  const invalidateSession = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userRole");
    setToken(null);
    setUser(null);
    delete api.defaults.headers.common["Authorization"];
    window.location.href = "/login";
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        login,
        register,
        logout,
        invalidateSession,
        isLoading,
        isAuthenticated,
        verifyOtp,
        requestMagicLink,
        verifyMagicLink,
        refreshProfile: hydrateProfile,
        preferredCurrency: user?.preferredCurrency || "KWD",
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
