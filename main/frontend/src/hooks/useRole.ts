import { useAuth } from '@/context/AuthContext'

export function useRole() {
  const { user } = useAuth()

  const hasRole = (role: string | string[]) => {
    if (!user) return false
    if (Array.isArray(role)) {
      return role.includes(user.role)
    }
    return user.role === role
  }

  const isCustomer = () => hasRole('CUSTOMER')
  const isBankOfficer = () => hasRole('BANKOFFICER')
  const isAdmin = () => hasRole('ADMIN')
  const isStaff = () => hasRole(['BANKOFFICER', 'ADMIN'])

  const canManageProducts = () => isStaff()
  const canViewAllAccounts = () => isStaff()
  const canManageUsers = () => isAdmin()

  return {
    user,
    hasRole,
    isCustomer,
    isBankOfficer,
    isAdmin,
    isStaff,
    canManageProducts,
    canViewAllAccounts,
    canManageUsers,
  }
}
