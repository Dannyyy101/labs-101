'use client'

import Body, { type ExtendedBodyPart, type Slug } from "react-muscle-highlighter"
import { de } from "@/app/runs/format"
import { COLORS, MUSCLES } from "./stats"

const LEVELS = [`${COLORS.volume}55`, `${COLORS.volume}99`, `${COLORS.volume}cc`, COLORS.volume]

/** Front and back of a body, the more sets a muscle got the stronger it is colored. */
export default function MuscleMap({ muscles, scale = 0.62, legend = 5 }: { muscles: [string, number][], scale?: number, legend?: number }) {
    const max = Math.max(1, ...muscles.map(([, sets]) => sets))
    const data: ExtendedBodyPart[] = muscles.map(([slug, sets]) => ({
        slug: slug as Slug,
        intensity: Math.max(1, Math.ceil(sets / max * LEVELS.length)),
    }))

    return <div>
        <div className="flex justify-center gap-2">
            {(["front", "back"] as const).map((side) =>
                <Body key={side} data={data} side={side} scale={scale} colors={LEVELS} border="none" defaultFill="#8e8e9340" />)}
        </div>
        {legend > 0 && muscles.length > 0 && <div className="mt-3 flex flex-col">
            {muscles.slice(0, legend).map(([slug, sets]) => <div key={slug} className="grid grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)_44px] items-center gap-2.5 py-1 text-sm">
                <span className="truncate">{MUSCLES[slug] ?? slug}</span>
                <div className="h-2 overflow-hidden rounded-full bg-muted">
                    <div className="h-full rounded-full" style={{ width: `${sets / max * 100}%`, background: COLORS.volume }} />
                </div>
                <span className="text-right tabular-nums text-muted-foreground">{de(sets)} {sets === 1 ? "Satz" : "Sätze"}</span>
            </div>)}
        </div>}
        {muscles.length === 0 && <p className="mt-3 text-center text-sm text-muted-foreground">Noch keine Muskeln trainiert</p>}
    </div>
}
