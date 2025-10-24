"use client"

import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { useAuth } from "@/hooks/use-auth"
import { Button } from "@/components/ui/button"
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Calculator,
  TrendingUp,
  FileText,
  Zap,
  Settings,
  LogOut,
  Menu,
  X,
} from "lucide-react"
import { useState } from "react"
import { clearStoredToken } from "@/lib/auth"

const navigationItems = {
  CUSTOMER: [
    { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
    { label: "My Accounts", href: "/accounts", icon: CreditCard },
    { label: "FD Calculator", href: "/fd-calculator", icon: Calculator },
    { label: "My FDs", href: "/fd", icon: TrendingUp },
    { label: "Reports", href: "/reports", icon: FileText },
    { label: "Settings", href: "/settings", icon: Settings },
  ],
  BANK_OFFICER: [
    { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
    { label: "Customers", href: "/customers", icon: Users },
    { label: "Accounts", href: "/accounts", icon: CreditCard },
    { label: "Products", href: "/product-pricing", icon: TrendingUp },
    { label: "FD Accounts", href: "/fd", icon: TrendingUp },
    { label: "Reports", href: "/reports", icon: FileText },
    { label: "Settings", href: "/settings", icon: Settings },
  ],
  ADMIN: [
    { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
    { label: "Customers", href: "/customers", icon: Users },
    { label: "Accounts", href: "/accounts", icon: CreditCard },
    { label: "Products", href: "/product-pricing", icon: TrendingUp },
    { label: "FD Accounts", href: "/fd", icon: TrendingUp },
    { label: "Reports", href: "/reports", icon: FileText },
    { label: "Batch Monitor", href: "/batches", icon: Zap },
    { label: "Settings", href: "/settings", icon: Settings },
  ],
}

export function Sidebar() {
  const { user, logout } = useAuth()
  const pathname = usePathname()
  const router = useRouter()
  const [isOpen, setIsOpen] = useState(false)

  if (!user) return null

  const items = navigationItems[user.role] || []

  const handleLogout = () => {
    logout()
    clearStoredToken()
    router.push("/login")
  }

  return (
    <>
      {/* Mobile menu button */}
      <div className="md:hidden fixed top-4 left-4 z-50">
        <Button variant="outline" size="icon" onClick={() => setIsOpen(!isOpen)}>
          {isOpen ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
        </Button>
      </div>

      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 h-screen w-64 bg-card border-r border-border transition-transform duration-300 md:translate-x-0 z-40 ${
          isOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex flex-col h-full p-6">
          {/* Logo */}
          <div className="mb-8 mt-2">
            <h1 className="text-2xl font-bold text-primary">BT Console</h1>
            <p className="text-xs text-muted-foreground mt-1">{user.role.replace(/_/g, " ")}</p>
          </div>

          {/* Navigation */}
          <nav className="flex-1 space-y-2">
            {items.map((item) => {
              const Icon = item.icon
              const isActive = pathname === item.href
              return (
                <Link key={item.href} href={item.href}>
                  <Button
                    variant={isActive ? "default" : "ghost"}
                    className="w-full justify-start gap-3"
                    onClick={() => setIsOpen(false)}
                  >
                    <Icon className="h-4 w-4" />
                    {item.label}
                  </Button>
                </Link>
              )
            })}
          </nav>

          {/* User info and logout */}
          <div className="border-t border-border pt-4 space-y-3">
            <div className="px-2">
              <p className="text-sm font-medium truncate">{user.name}</p>
              <p className="text-xs text-muted-foreground truncate">{user.email}</p>
            </div>
            <Button variant="outline" className="w-full justify-start gap-2 bg-transparent" onClick={handleLogout}>
              <LogOut className="h-4 w-4" />
              Logout
            </Button>
          </div>
        </div>
      </aside>

      {/* Mobile overlay */}
      {isOpen && <div className="fixed inset-0 bg-black/50 md:hidden z-30" onClick={() => setIsOpen(false)} />}
    </>
  )
}
