// Role-based access control component

"use client"

import type { ReactNode } from "react"
import type { UserRole } from "@/lib/types"
import { canAccessPage } from "@/lib/rbac"
import { getCurrentUser } from "@/lib/auth"

interface RoleGuardProps {
  children: ReactNode
  allowedRoles: UserRole[]
  fallback?: ReactNode
}

export function RoleGuard({ children, allowedRoles, fallback }: RoleGuardProps) {
  const user = getCurrentUser()

  if (!user || !allowedRoles.includes(user.role)) {
    return fallback || <div className="p-8 text-center text-muted-foreground">Access denied</div>
  }

  return <>{children}</>
}

interface PageGuardProps {
  children: ReactNode
  page: string
  fallback?: ReactNode
}

export function PageGuard({ children, page, fallback }: PageGuardProps) {
  const user = getCurrentUser()

  if (!user || !canAccessPage(user.role, page)) {
    return fallback || <div className="p-8 text-center text-muted-foreground">Access denied</div>
  }

  return <>{children}</>
}
