'use client'

import { Map, MapControls, useMap } from "@/components/ui/map"
import { RunPoint } from "@/utils/types/run"
import type { GeoJSONSource, LngLatBoundsLike } from "maplibre-gl"
import { useEffect, useMemo } from "react"
import { paceColor } from "./format"

const ACCENT = "#0a84ff"

type Collection = GeoJSON.FeatureCollection

const empty: Collection = { type: "FeatureCollection", features: [] }

const line = (points: RunPoint[], properties: GeoJSON.GeoJsonProperties = {}): GeoJSON.Feature => ({
    type: "Feature",
    properties,
    geometry: { type: "LineString", coordinates: points.map((p) => [p.longitude, p.latitude]) },
})

const point = (p: RunPoint, properties: GeoJSON.GeoJsonProperties = {}): GeoJSON.Feature => ({
    type: "Feature",
    properties,
    geometry: { type: "Point", coordinates: [p.longitude, p.latitude] },
})

// route colored by pace, between the 5th and 95th percentile so single GPS outliers don't flatten the colors
function paceSegments(points: RunPoint[]): Collection {
    const paces = points.map((p) => p.pace).filter((p): p is number => p != null).sort((a, b) => a - b)
    if (!paces.length) return { type: "FeatureCollection", features: [line(points, { color: ACCENT })] }
    const lo = paces[Math.floor(paces.length * .05)], hi = paces[Math.floor(paces.length * .95)]
    const features: GeoJSON.Feature[] = []
    for (let i = 0; i < points.length - 1; i += 3) {
        const segment = points.slice(i, i + 4)
        const known = segment.filter((p) => p.pace != null)
        const avg = known.reduce((sum, p) => sum + p.pace!, 0) / (known.length || 1)
        features.push(line(segment, { color: paceColor((avg - lo) / Math.max(1, hi - lo)) }))
    }
    return { type: "FeatureCollection", features }
}

export default function RunMap({ points, highlightKm, cursor, children }: {
    points: RunPoint[]
    // km index of the split under the mouse
    highlightKm: number | null
    cursor: RunPoint | null
    children?: React.ReactNode
}) {
    return <div className="relative h-[420px] overflow-hidden rounded-[18px] bg-muted lg:h-[520px]">
        <Map center={[13.4, 52.5]} zoom={12} scrollZoom={false} attributionControl={{ compact: true }}>
            <MapControls position="bottom-right" />
            <RouteLayers points={points} highlightKm={highlightKm} cursor={cursor} />
        </Map>
        {children}
    </div>
}

function RouteLayers({ points, highlightKm, cursor }: { points: RunPoint[], highlightKm: number | null, cursor: RunPoint | null }) {
    const { map, isLoaded } = useMap()

    const route = useMemo(() => paceSegments(points), [points])
    const outline = useMemo<Collection>(() => points.length > 1 ? { type: "FeatureCollection", features: [line(points)] } : empty, [points])
    const markers = useMemo<Collection>(() => points.length > 1 ? {
        type: "FeatureCollection",
        features: [point(points[0], { color: "#30d158" }), point(points[points.length - 1], { color: "#ff453a" })],
    } : empty, [points])
    const highlight = useMemo<Collection>(() => {
        if (highlightKm == null) return empty
        const segment = points.filter((p) => p.distance >= highlightKm * 1000 - 20 && p.distance <= (highlightKm + 1) * 1000 + 20)
        return segment.length > 1 ? { type: "FeatureCollection", features: [line(segment)] } : empty
    }, [points, highlightKm])
    const cursorData = useMemo<Collection>(() => cursor ? { type: "FeatureCollection", features: [point(cursor)] } : empty, [cursor])

    // layers are added again whenever the style changes (light/dark)
    useEffect(() => {
        if (!map || !isLoaded) return
        const sources = ["run-outline", "run-route", "run-highlight", "run-markers", "run-cursor"]
        sources.forEach((id) => map.addSource(id, { type: "geojson", data: empty }))
        const round = { "line-join": "round", "line-cap": "round" } as const
        map.addLayer({ id: "run-outline", type: "line", source: "run-outline", layout: round, paint: { "line-color": "#000", "line-width": 9, "line-opacity": .18 } })
        map.addLayer({ id: "run-route", type: "line", source: "run-route", layout: round, paint: { "line-color": ["get", "color"], "line-width": 5 } })
        map.addLayer({ id: "run-highlight-casing", type: "line", source: "run-highlight", layout: round, paint: { "line-color": "#fff", "line-width": 11, "line-opacity": .9 } })
        map.addLayer({ id: "run-highlight", type: "line", source: "run-highlight", layout: round, paint: { "line-color": ACCENT, "line-width": 6 } })
        map.addLayer({ id: "run-markers", type: "circle", source: "run-markers", paint: { "circle-radius": 7, "circle-color": ["get", "color"], "circle-stroke-color": "#fff", "circle-stroke-width": 3 } })
        map.addLayer({ id: "run-cursor", type: "circle", source: "run-cursor", paint: { "circle-radius": 7, "circle-color": ACCENT, "circle-stroke-color": "#fff", "circle-stroke-width": 3 } })
        return () => {
            try {
                ["run-cursor", "run-markers", "run-highlight", "run-highlight-casing", "run-route", "run-outline"].forEach((id) => map.getLayer(id) && map.removeLayer(id))
                sources.forEach((id) => map.getSource(id) && map.removeSource(id))
            } catch {
                // the style is already gone
            }
        }
    }, [map, isLoaded])

    useEffect(() => {
        if (!map || !isLoaded) return
        const set = (id: string, data: Collection) => (map.getSource(id) as GeoJSONSource | undefined)?.setData(data)
        set("run-outline", outline)
        set("run-route", route)
        set("run-markers", markers)
    }, [map, isLoaded, outline, route, markers])

    useEffect(() => {
        if (map && isLoaded) (map.getSource("run-highlight") as GeoJSONSource | undefined)?.setData(highlight)
    }, [map, isLoaded, highlight])

    useEffect(() => {
        if (map && isLoaded) (map.getSource("run-cursor") as GeoJSONSource | undefined)?.setData(cursorData)
    }, [map, isLoaded, cursorData])

    // only when another run is shown, not when the style changes
    useEffect(() => {
        if (!map || points.length < 2) return
        const lons = points.map((p) => p.longitude), lats = points.map((p) => p.latitude)
        const bounds: LngLatBoundsLike = [[Math.min(...lons), Math.min(...lats)], [Math.max(...lons), Math.max(...lats)]]
        map.fitBounds(bounds, { padding: 60, duration: 600 })
    }, [map, points])

    return null
}
