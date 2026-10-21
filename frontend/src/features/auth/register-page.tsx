import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '@/app/providers/auth-provider'
import { Button } from '@/components/ui/button'
import { describedBy, Field } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { PasswordInput } from './password-input'
import { registerSchema, type RegisterForm } from './schemas'
import { applyServerErrors } from './use-server-errors'

const PASSWORD_HINT = 'Mínimo de 8 caracteres, com letras e números.'

export function RegisterPage() {
  const { register: registerAccount } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState('')
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '', confirmPassword: '' },
  })

  const onSubmit = handleSubmit(async ({ displayName, email, password }) => {
    setFormError('')
    try {
      await registerAccount({ displayName, email, password })
      navigate('/', { replace: true })
    } catch (error) {
      setFormError(applyServerErrors(error, setError, ['displayName', 'email', 'password']))
    }
  })

  return (
    <>
      <h1 className="font-display text-3xl font-semibold tracking-tight">Crie sua conta</h1>
      <p className="mt-2 text-sm text-muted">Comece a planejar seu objetivo em poucos minutos.</p>

      <form onSubmit={onSubmit} noValidate className="mt-8 grid gap-5">
        {formError && (
          <div role="alert" className="rounded-xl bg-danger-soft px-4 py-3 text-sm text-danger">
            {formError}
          </div>
        )}
        <Field id="displayName" label="Nome" error={errors.displayName?.message}>
          <Input
            id="displayName"
            autoComplete="name"
            aria-invalid={!!errors.displayName}
            aria-describedby={describedBy('displayName', errors.displayName?.message)}
            {...register('displayName')}
          />
        </Field>
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
        <Field id="password" label="Senha" error={errors.password?.message} hint={PASSWORD_HINT}>
          <PasswordInput
            id="password"
            autoComplete="new-password"
            aria-invalid={!!errors.password}
            aria-describedby={describedBy('password', errors.password?.message, PASSWORD_HINT)}
            {...register('password')}
          />
        </Field>
        <Field id="confirmPassword" label="Confirmar senha" error={errors.confirmPassword?.message}>
          <PasswordInput
            id="confirmPassword"
            autoComplete="new-password"
            aria-invalid={!!errors.confirmPassword}
            aria-describedby={describedBy('confirmPassword', errors.confirmPassword?.message)}
            {...register('confirmPassword')}
          />
        </Field>
        <Button type="submit" size="lg" loading={isSubmitting} className="mt-2 w-full">
          Criar conta
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted">
        Já tem conta?{' '}
        <Link to="/login" className="font-medium text-primary hover:underline">
          Entrar
        </Link>
      </p>
    </>
  )
}
