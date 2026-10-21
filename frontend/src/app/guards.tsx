import { Navigate, Outlet, useLocation } from 'react-router'
import { FullPageLoader } from '@/components/full-page-loader'
import { useAuth } from './providers/auth-provider'

export function RequireAuth() {
  const { status } = useAuth()
  const location = useLocation()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'anonymous') return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function RedirectIfAuthenticated() {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'authenticated') return <Navigate to="/" replace />
  return <Outlet />
}
