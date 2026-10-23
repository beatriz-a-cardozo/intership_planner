import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import { invalidateUniversities, qk } from '@/api/query-keys'
import type {
  MapLocation,
  Application,
  ApplicationCreateRequest,
  ApplicationStatus,
  ApplicationUpdateRequest,
  CompareResult,
  Program,
  ProgramRequest,
  Rating,
  Requirement,
  StatusHistory,
  University,
  UniversityDetail,
  UniversityImage,
  UniversityRequest,
  UniversityRequestItem,
  UniversityRequestRequest,
} from '@/api/types'

function query(params: object) {
  const s = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') s.set(k, String(v))
  })
  const str = s.toString()
  return str ? `?${str}` : ''
}

function useUniMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const client = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => invalidateUniversities(client) })
}

// ------------------------------------------------------------------ universidades

/** Universidades com programas escolhidos, para o mapa do dashboard (coordenadas descobertas pelo backend). */
export function useUniversityLocations() {
  return useQuery({
    queryKey: qk.universityLocations,
    queryFn: () => api.get<MapLocation[]>('/api/universities/locations'),
    staleTime: 5 * 60_000,
  })
}

export function useUniversities(params: { q?: string } = {}) {
  return useQuery({
    queryKey: qk.universities(params),
    queryFn: () => api.get<University[]>(`/api/universities${query(params)}`),
  })
}

export function useUniversity(id: string) {
  return useQuery({
    queryKey: qk.university(id),
    queryFn: () => api.get<UniversityDetail>(`/api/universities/${id}`),
  })
}

export function useSaveUniversity() {
  return useUniMutation(({ id, body }: { id?: string; body: UniversityRequest }) =>
    id
      ? api.put<University>(`/api/universities/${id}`, body)
      : api.post<University>('/api/universities', body),
  )
}

export function useDeleteUniversity() {
  return useUniMutation((id: string) => api.delete<void>(`/api/universities/${id}`))
}

// ------------------------------------------------------------------ programas

export interface ProgramFilters {
  universityId?: string
  chosen?: boolean
  applicationStatus?: ApplicationStatus
  gre?: Requirement
  interview?: Requirement
  nc?: Requirement
  minAdmissionChance?: Rating
  maxSemesterFee?: number
  q?: string
}

export function usePrograms(filters: ProgramFilters) {
  return useQuery({
    queryKey: qk.programs(filters),
    queryFn: () => api.get<Program[]>(`/api/programs${query(filters)}`),
    placeholderData: (previous) => previous,
  })
}

export function useProgram(id: string) {
  return useQuery({ queryKey: qk.program(id), queryFn: () => api.get<Program>(`/api/programs/${id}`) })
}

export function useCompare(ids: string[]) {
  return useQuery({
    queryKey: qk.compare(ids),
    queryFn: () => api.get<CompareResult>(`/api/programs/compare?ids=${ids.join(',')}`),
    enabled: ids.length >= 2 && ids.length <= 5,
  })
}

export function useSaveProgram() {
  return useUniMutation(({ id, body }: { id?: string; body: ProgramRequest }) =>
    id ? api.put<Program>(`/api/programs/${id}`, body) : api.post<Program>('/api/programs', body),
  )
}

export function useDeleteProgram() {
  return useUniMutation((id: string) => api.delete<void>(`/api/programs/${id}`))
}

export function useSetChosen() {
  return useUniMutation(({ id, chosen }: { id: string; chosen: boolean }) =>
    api.patch<Program>(`/api/programs/${id}/chosen`, { chosen }),
  )
}

// ------------------------------------------------------------------ candidaturas

export function useApplications(params: { status?: ApplicationStatus; includeArchived?: boolean } = {}) {
  return useQuery({
    queryKey: qk.applications(params),
    queryFn: () => api.get<Application[]>(`/api/applications${query(params)}`),
  })
}

export function useApplication(id: string | undefined) {
  return useQuery({
    queryKey: qk.application(id ?? ''),
    queryFn: () => api.get<Application>(`/api/applications/${id}`),
    enabled: !!id,
  })
}

export function useApplicationHistory(id: string | undefined) {
  return useQuery({
    queryKey: qk.applicationHistory(id ?? ''),
    queryFn: () => api.get<StatusHistory[]>(`/api/applications/${id}/history`),
    enabled: !!id,
  })
}

export function useApplicationRequests(id: string | undefined) {
  return useQuery({
    queryKey: qk.applicationRequests(id ?? ''),
    queryFn: () => api.get<UniversityRequestItem[]>(`/api/applications/${id}/requests`),
    enabled: !!id,
  })
}

export function useCreateApplication() {
  return useUniMutation((body: ApplicationCreateRequest) => api.post<Application>('/api/applications', body))
}

export function useUpdateApplication() {
  return useUniMutation(({ id, body }: { id: string; body: ApplicationUpdateRequest }) =>
    api.put<Application>(`/api/applications/${id}`, body),
  )
}

export function useChangeStatus() {
  return useUniMutation(
    ({
      id,
      status,
      note,
      changedOn,
    }: {
      id: string
      status: ApplicationStatus
      note?: string
      changedOn?: string
    }) => api.patch<Application>(`/api/applications/${id}/status`, { status, note, changedOn }),
  )
}

export function useDeleteApplication() {
  return useUniMutation((id: string) => api.delete<void>(`/api/applications/${id}`))
}

export function useSaveRequest(applicationId: string) {
  return useUniMutation(({ id, body }: { id?: string; body: UniversityRequestRequest }) =>
    id
      ? api.put<UniversityRequestItem>(`/api/applications/${applicationId}/requests/${id}`, body)
      : api.post<UniversityRequestItem>(`/api/applications/${applicationId}/requests`, body),
  )
}

export function useDeleteRequest(applicationId: string) {
  return useUniMutation((id: string) => api.delete<void>(`/api/applications/${applicationId}/requests/${id}`))
}

// ------------------------------------------------------------------ imagens

export function useUniversityImages(universityId: string) {
  return useQuery({
    queryKey: qk.universityImages(universityId),
    queryFn: () => api.get<UniversityImage[]>(`/api/universities/${universityId}/images`),
  })
}

/** Bytes da imagem (autenticados). O id muda se a imagem mudar, então o cache nunca expira. */
export function useImageBlob(universityId: string, imageId: string | null | undefined) {
  return useQuery({
    queryKey: qk.imageContent(universityId, imageId ?? ''),
    queryFn: () => api.blob(`/api/universities/${universityId}/images/${imageId}/content`),
    enabled: !!imageId,
    staleTime: Infinity,
    gcTime: 30 * 60_000,
  })
}

export function useUploadImage(universityId: string) {
  return useUniMutation(({ file, caption }: { file: Blob; caption?: string }) => {
    const form = new FormData()
    form.append('file', file, 'imagem')
    const qs = caption ? `?caption=${encodeURIComponent(caption)}` : ''
    return api.upload<UniversityImage>(`/api/universities/${universityId}/images${qs}`, form)
  })
}

export function useSetCover(universityId: string) {
  return useUniMutation((imageId: string) =>
    api.patch<UniversityImage[]>(`/api/universities/${universityId}/images/${imageId}/cover`),
  )
}

export function useDeleteImage(universityId: string) {
  return useUniMutation((imageId: string) =>
    api.delete<void>(`/api/universities/${universityId}/images/${imageId}`),
  )
}
