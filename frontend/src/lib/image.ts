/**
 * Prepara uma foto para envio: reduz para no máximo {@link MAX_SIDE}px no maior lado e converte para WebP
 * (fotos de celular de 4–8 MB viram ~200–400 KB). Se o navegador não codificar WebP ou o resultado ficar maior
 * que o original, envia o original. O backend valida o tipo pelos bytes e o limite de 5 MB de qualquer forma.
 */
export const MAX_SIDE = 1600
export const MAX_BYTES = 5 * 1024 * 1024
export const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export async function prepareImage(file: File): Promise<Blob> {
  if (!ACCEPTED_TYPES.includes(file.type)) {
    throw new Error('Escolha uma imagem JPEG, PNG ou WebP.')
  }
  let bitmap: ImageBitmap
  try {
    bitmap = await createImageBitmap(file)
  } catch {
    throw new Error('Não foi possível ler essa imagem.')
  }
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(bitmap.width * scale)
  canvas.height = Math.round(bitmap.height * scale)
  canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close()

  const resized = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/webp', 0.85))
  const best = resized && ACCEPTED_TYPES.includes(resized.type) && resized.size < file.size ? resized : file
  if (best.size > MAX_BYTES) {
    throw new Error('A imagem deve ter até 5 MB.')
  }
  return best
}
