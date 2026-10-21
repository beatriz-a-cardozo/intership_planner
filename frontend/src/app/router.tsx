import { createBrowserRouter } from 'react-router'
import { FullPageLoader } from '@/components/full-page-loader'
import { AppShell } from '@/layouts/app-shell'
import { AuthLayout } from '@/layouts/auth-layout'
import { isAvailable, NAV_ITEMS } from '@/layouts/navigation'
import { PlannedModulePage } from '@/features/roadmap/planned-module-page'
import { RouteError } from './route-error'
import { OnboardingOnly, RedirectIfAuthenticated, RequireAuth, RequireOnboarding } from './guards'

// Páginas carregadas sob demanda: cada módulo vira um chunk separado.
const page =
  <T extends Record<string, unknown>>(loader: () => Promise<T>, name: keyof T) =>
  async () => {
    const module = await loader()
    return { Component: module[name] as React.ComponentType }
  }

const plannedRoutes = NAV_ITEMS.filter((i) => !isAvailable(i)).map((item) => ({
  path: item.to,
  element: <PlannedModulePage item={item} />,
}))

export const router = createBrowserRouter([
  {
    element: <RedirectIfAuthenticated />,
    HydrateFallback: FullPageLoader,
    errorElement: <RouteError />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: '/login', lazy: page(() => import('@/features/auth/login-page'), 'LoginPage') },
          { path: '/register', lazy: page(() => import('@/features/auth/register-page'), 'RegisterPage') },
        ],
      },
    ],
  },
  {
    element: <RequireAuth />,
    HydrateFallback: FullPageLoader,
    errorElement: <RouteError />,
    children: [
      {
        element: <OnboardingOnly />,
        children: [
          {
            path: '/onboarding',
            lazy: page(() => import('@/features/onboarding/onboarding-page'), 'OnboardingPage'),
          },
        ],
      },
      {
        element: <RequireOnboarding />,
        children: [
          {
            element: <AppShell />,
            children: [
              {
                index: true,
                lazy: page(() => import('@/features/dashboard/dashboard-page'), 'DashboardPage'),
              },
              {
                path: '/objectives',
                lazy: page(() => import('@/features/objectives/objectives-page'), 'ObjectivesPage'),
              },
              {
                path: '/objectives/:id',
                lazy: page(
                  () => import('@/features/objectives/objective-detail-page'),
                  'ObjectiveDetailPage',
                ),
              },
              {
                path: '/expenses',
                lazy: page(() => import('@/features/expenses/expenses-page'), 'ExpensesPage'),
              },
              {
                path: '/finance',
                lazy: page(() => import('@/features/finance/finance-page'), 'FinancePage'),
              },
              {
                path: '/settings',
                lazy: page(() => import('@/features/settings/settings-page'), 'SettingsPage'),
              },
              ...plannedRoutes,
              { path: '*', lazy: page(() => import('@/features/not-found-page'), 'NotFoundPage') },
            ],
          },
        ],
      },
    ],
  },
])
