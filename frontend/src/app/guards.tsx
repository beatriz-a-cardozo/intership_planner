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

/** Enquanto o onboarding não for concluído, todas as telas do app levam a ele. */
export function RequireOnboarding() {
  const { user } = useAuth()
  if (user && !user.onboardingCompleted) return <Navigate to="/onboarding" replace />
  return <Outlet />
}

/** O onboarding só existe uma vez: concluído, volta para o dashboard. */
export function OnboardingOnly() {
  const { user } = useAuth()
  if (user?.onboardingCompleted) return <Navigate to="/" replace />
  return <Outlet />
}

export function RedirectIfAuthenticated() {
  const { status } = useAuth()
  if (status === 'loading') return <FullPageLoader />
  if (status === 'authenticated') return <Navigate to="/" replace />
  return <Outlet />
}
