import { z } from 'zod'

// Validação de formulário para feedback imediato; o backend revalida tudo (Bean Validation).
export const loginSchema = z.object({
  email: z.email('Informe um e-mail válido.'),
  password: z.string().min(1, 'Informe sua senha.'),
})

export const registerSchema = z
  .object({
    displayName: z.string().trim().min(2, 'Informe seu nome.').max(80, 'Máximo de 80 caracteres.'),
    email: z.email('Informe um e-mail válido.'),
    password: z
      .string()
      .min(8, 'Use pelo menos 8 caracteres.')
      .max(128, 'Máximo de 128 caracteres.')
      .regex(/[A-Za-z]/, 'Inclua pelo menos uma letra.')
      .regex(/\d/, 'Inclua pelo menos um número.'),
    confirmPassword: z.string(),
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: 'As senhas não conferem.',
  })

export type LoginForm = z.infer<typeof loginSchema>
export type RegisterForm = z.infer<typeof registerSchema>
