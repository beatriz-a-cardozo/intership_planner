import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { useAuth } from '@/app/providers/auth-provider'
import { Button } from '@/components/ui/button'
import { describedBy, Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { PasswordInput } from './password-input'
import { loginSchema, type LoginForm } from './schemas'
import { applyServerErrors } from './use-server-errors'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } })

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const onSubmit = handleSubmit(async (values) => {
    setFormError('')
    try {
      await login(values)
      navigate(from, { replace: true })
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['email', 'password']))
    }
  })

  return (
    <>
      <h1 className="font-display text-3xl font-semibold tracking-tight">Que bom te ver de novo</h1>
      <p className="mt-2 text-sm text-muted">Entre para continuar seu planejamento.</p>

      <form onSubmit={onSubmit} noValidate className="mt-8 grid gap-5">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="email" label="E-mail" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            inputMode="email"
            aria-invalid={!!errors.email}
            aria-describedby={describedBy('email', errors.email?.message)}
            {...register('email')}
          />
        </Field>
        <Field id="password" label="Senha" error={errors.password?.message}>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            aria-invalid={!!errors.password}
            aria-describedby={describedBy('password', errors.password?.message)}
            {...register('password')}
          />
        </Field>
        <Button type="submit" size="lg" loading={isSubmitting} className="mt-2 w-full">
          Entrar
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted">
        Ainda não tem conta?{' '}
        <Link to="/register" className="font-medium text-primary hover:underline">
          Criar conta
        </Link>
      </p>
    </>
  )
}
