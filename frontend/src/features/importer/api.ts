import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidateMoney, invalidatePlanning, invalidateUniversities } from '@/api/query-keys'
import type { CommitRequest, ImportPreview } from '@/api/types'

export function useUploadSpreadsheet() {
  return useMutation({
    mutationFn: (file: File) => {
      const form = new FormData()
      form.append('file', file, file.name)
      return api.upload<ImportPreview>('/api/imports', form)
    },
  })
}

/** Importar mexe em gastos, universidades e mala: tudo é recarregado. */
export function useCommitImport() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: CommitRequest }) =>
      api.post<ImportPreview>(`/api/imports/${id}/commit`, body),
    onSuccess: () => {
      invalidateMoney(client)
      invalidateUniversities(client)
      invalidatePlanning(client)
      void client.invalidateQueries({ queryKey: ['luggage'] })
      void client.invalidateQueries({ queryKey: ['catalog'] })
    },
  })
}
