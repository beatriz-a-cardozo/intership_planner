import 'leaflet/dist/leaflet.css'
import L from 'leaflet'
import { useEffect, useRef } from 'react'
import type { MapLocation } from '@/api/types'

/** Cores do tema atual (claro/escuro): o marcador acompanha os tokens do design. */
function themeColor(token: '--primary' | '--accent' | '--surface') {
  return getComputedStyle(document.documentElement).getPropertyValue(token).trim()
}

/** Ponto do mapa: universidade com coordenadas. */
export type MapPoint = MapLocation & { latitude: number; longitude: number }

function tooltipContent(p: MapPoint) {
  // Conteúdo montado com textContent: nomes vêm do usuário e nunca viram HTML
  const root = document.createElement('div')
  const title = document.createElement('strong')
  title.textContent = p.name
  root.append(title)
  const place = [p.city, p.countryCode].filter(Boolean).join(', ')
  for (const line of [place, ...p.programs.map((pr) => [pr.name, pr.degree].filter(Boolean).join(' · '))]) {
    if (!line) continue
    const div = document.createElement('div')
    div.textContent = line
    root.append(div)
  }
  return root
}

/**
 * Mapa (Leaflet + tiles do OpenStreetMap) com um marcador por universidade. Ajusta o zoom para mostrar todos;
 * `selected` destaca e centraliza um deles. Clicar num marcador chama `onSelect`.
 */
export default function LocationsMap({
  points,
  selected,
  onSelect,
}: {
  points: MapPoint[]
  selected: string | null
  onSelect: (universityId: string) => void
}) {
  const container = useRef<HTMLDivElement>(null)
  const map = useRef<L.Map | null>(null)
  const markers = useRef(new Map<string, L.CircleMarker>())

  useEffect(() => {
    if (!container.current) return
    const m = L.map(container.current, {
      scrollWheelZoom: false,
      zoomControl: false,
      attributionControl: true,
    })
    L.control.zoom({ zoomInTitle: 'Aproximar', zoomOutTitle: 'Afastar' }).addTo(m)
    m.attributionControl.setPrefix(false)
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(m)
    map.current = m
    const registry = markers.current
    return () => {
      registry.clear()
      m.remove()
      map.current = null
    }
  }, [])

  useEffect(() => {
    const m = map.current
    if (!m) return
    const registry = markers.current
    registry.forEach((marker) => marker.remove())
    registry.clear()
    for (const p of points) {
      const marker = L.circleMarker([p.latitude, p.longitude], {
        radius: 9,
        weight: 3,
        color: themeColor('--surface'),
        fillColor: themeColor('--primary'),
        fillOpacity: 1,
      })
        .bindTooltip(tooltipContent(p), { direction: 'top', offset: [0, -8] })
        .on('click', () => onSelect(p.universityId))
        .addTo(m)
      registry.set(p.universityId, marker)
    }
    const [only] = points
    if (points.length === 1 && only) {
      m.setView([only.latitude, only.longitude], 9)
    } else if (points.length > 1) {
      m.fitBounds(L.latLngBounds(points.map((p) => [p.latitude, p.longitude])), {
        padding: [36, 36],
        maxZoom: 9,
      })
    }
  }, [points, onSelect])

  useEffect(() => {
    markers.current.forEach((marker, id) =>
      marker.setStyle({
        fillColor: themeColor(id === selected ? '--accent' : '--primary'),
        radius: id === selected ? 12 : 9,
      }),
    )
    const marker = selected ? markers.current.get(selected) : undefined
    if (marker && map.current) {
      marker.bringToFront().openTooltip()
      map.current.panTo(marker.getLatLng())
    }
  }, [selected, points])

  return (
    <div
      ref={container}
      role="region"
      aria-label="Mapa dos destinos escolhidos"
      className="locations-map h-72 w-full overflow-hidden rounded-xl bg-surface-2 sm:h-80"
    />
  )
}
