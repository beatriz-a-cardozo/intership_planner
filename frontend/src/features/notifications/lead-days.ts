/** "30, 7, 1" ⇄ [30, 7, 1]; nulo se inválido. */
export function parseLeadDays(text: string): number[] | null {
  const parts = text
    .split(/[,;\s]+/)
    .map((p) => p.trim())
    .filter(Boolean)
  if (parts.length === 0 || parts.length > 5 || parts.some((p) => !/^\d{1,3}$/.test(p) || Number(p) > 365))
    return null
  return [...new Set(parts.map(Number))].sort((a, b) => b - a)
}
