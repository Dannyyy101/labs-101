'use client'

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Spinner } from "@/components/ui/spinner"
import { FoodPortion, Meal, SearchFood } from "@/utils/types/food"
import { FoodImageAnalysis, FoodImageItem, foodImageUrl } from "@/utils/types/foodImage"
import { AlertCircle, Check, Minus, Plus, Search, X } from "lucide-react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useEffect, useRef, useState } from "react"
import { findFoodByNameAndUserId, getOpenFood, getTrackedFoodByFoodId } from "../../track/action"
import { MealTabs } from "../../track/FoodSearch"
import { formatNumber } from "../../track/format"
import { STATUS_LABEL } from "../../track/PhotoFoodInput"
import { acceptFoodImage, rejectFoodImage } from "../action"

// the job usually needs a few seconds, the page asks again until it is done
const POLL_INTERVAL_MS = 4000

// ml are counted 1:1 as grams
const GRAM_UNITS = ["g", "ml"]

interface ReviewItem {
    key: number
    query: string
    amount: number
    unit: string
    // estimate of the model, only used while the food doesn't have the unit yet
    gramsPerUnit: number
    food: FoodImageItem["food"]
    // set while the food only exists in the open food database
    openFoodId: number | null
}

type ItemStatus = "OK" | "NEW_UNIT" | "NOT_FOUND"

const sameLabel = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase()
const portionOf = (item: ReviewItem) => item.food?.portions.find((p) => sameLabel(p.label, item.unit)) ?? null
const isGrams = (unit: string) => GRAM_UNITS.includes(unit)

// an existing portion keeps its grams, the backend only adds missing ones
const gramsPerUnitOf = (item: ReviewItem) => isGrams(item.unit) ? 1 : portionOf(item)?.grams ?? item.gramsPerUnit
const gramsOf = (item: ReviewItem) => item.amount * gramsPerUnitOf(item)
const kcalOf = (item: ReviewItem) => (item.food?.kcal ?? 0) * gramsOf(item) / 100

const statusOf = (item: ReviewItem): ItemStatus =>
    !item.food ? "NOT_FOUND" : isGrams(item.unit) || portionOf(item) ? "OK" : "NEW_UNIT"

export default function FoodImageReview({ analysis }: { analysis: FoodImageAnalysis }) {
    const router = useRouter()

    useEffect(() => {
        if (analysis.status !== "PENDING") return
        const interval = setInterval(() => router.refresh(), POLL_INTERVAL_MS)
        return () => clearInterval(interval)
    }, [analysis.status, router])

    const status = STATUS_LABEL[analysis.status]

    return <div className="flex flex-col gap-4">
        <div className="relative">
            {/* eslint-disable-next-line @next/next/no-img-element -- private photo through our route handler */}
            <img src={foodImageUrl(analysis.id)} alt="Foto der Mahlzeit" className="w-full max-h-[50vh] object-contain glass rounded-[14px]" />
            <span className={`absolute top-3 left-3 text-label font-semibold rounded-full px-3 py-1 glass ${status.className}`}>{status.label}</span>
        </div>

        {analysis.description && <p className="glass rounded-[14px] px-4 py-3 text-body">
            <span className="text-ink-muted">Deine Beschreibung: </span>{analysis.description}
        </p>}

        {analysis.status === "PENDING" && <div className="glass rounded-[14px] p-6 flex flex-col items-center gap-2 text-center">
            <Spinner className="size-6" />
            <p className="font-semibold">Das Foto wird ausgewertet</p>
            <p className="text-body text-ink-muted">Du bekommst eine Benachrichtigung, du kannst die Seite auch offen lassen.</p>
        </div>}

        {analysis.status === "FAILED" && <Alert variant="destructive">
            <AlertCircle className="size-4" />
            <AlertTitle>Das Foto konnte nicht ausgewertet werden</AlertTitle>
            <AlertDescription>Versuch es später mit einem neuen Foto, dieses zählt nicht zu deinem Tageslimit.</AlertDescription>
        </Alert>}

        {analysis.status === "READY" && <Review analysis={analysis} />}

        {(analysis.status === "ACCEPTED" || analysis.status === "REJECTED") && <Summary analysis={analysis} />}
    </div>
}

function Summary({ analysis }: { analysis: FoodImageAnalysis }) {
    return <div className="glass rounded-[14px] p-4 flex flex-col gap-y-2">
        <p className="text-body text-ink-muted">
            {analysis.status === "ACCEPTED" ? "Diese Lebensmittel wurden eingetragen:" : "Du hast die Auswertung abgelehnt, es wurde nichts eingetragen."}
        </p>
        {analysis.items.map((item, i) => <div key={i} className="flex items-center justify-between gap-x-3">
            <span className="truncate">{item.food?.name ?? item.query}</span>
            <span className="text-ink-muted tabular-nums shrink-0">{formatNumber(item.amount)} {item.unit}{!isGrams(item.unit) && ` · ${formatNumber(item.amount * item.gramsPerUnit)} g`}</span>
        </div>)}
        {analysis.status === "ACCEPTED" && <Link href="/foods/track" className="text-brand-strong hover:underline self-start mt-1">Zum Ernährungstagebuch</Link>}
    </div>
}

function Review({ analysis }: { analysis: FoodImageAnalysis }) {
    const router = useRouter()
    const [items, setItems] = useState<ReviewItem[]>(() => analysis.items.map((item, key) => ({
        key, query: item.query, amount: item.amount, unit: item.unit, gramsPerUnit: item.gramsPerUnit,
        food: item.food, openFoodId: item.openFoodId,
    })))
    const [meal, setMeal] = useState<Meal>(analysis.meal)
    const [saving, setSaving] = useState<"ACCEPT" | "REJECT" | null>(null)
    const [error, setError] = useState<string | null>(null)

    const updateItem = (key: number, change: Partial<ReviewItem>) =>
        setItems((prev) => prev.map((item) => item.key === key ? { ...item, ...change } : item))
    const removeItem = (key: number) => setItems((prev) => prev.filter((item) => item.key !== key))

    const accept = async () => {
        setSaving("ACCEPT")
        setError(null)
        const result = await acceptFoodImage(analysis.id, {
            meal,
            items: items.map((item) => ({
                foodId: item.openFoodId === null ? item.food!.id : null,
                openFoodId: item.openFoodId,
                amount: item.amount,
                unit: item.unit,
                gramsPerUnit: gramsPerUnitOf(item),
            })),
        })
        if (result.ok) {
            router.push("/foods/track")
        } else {
            setError(result.error)
            setSaving(null)
        }
    }

    const reject = async () => {
        setSaving("REJECT")
        setError(null)
        const result = await rejectFoodImage(analysis.id)
        if (result.ok) router.refresh()
        else setError(result.error)
        setSaving(null)
    }

    const notFound = items.filter((i) => statusOf(i) === "NOT_FOUND").length
    const newUnits = items.filter((i) => statusOf(i) === "NEW_UNIT").length
    const totalKcal = items.reduce((sum, i) => sum + kcalOf(i), 0)
    const canAccept = items.length > 0 && notFound === 0 && items.every((i) => i.amount > 0 && gramsPerUnitOf(i) > 0) && saving === null

    const hint = items.length === 0
        ? "Nichts mehr übrig, lehne das Foto ab"
        : notFound > 0
            ? `${notFound} nicht gefunden – Lebensmittel wählen oder entfernen`
            : newUnits > 0
                ? `${newUnits} neue ${newUnits === 1 ? "Portion wird" : "Portionen werden"} angelegt`
                : `${items.length} Lebensmittel`

    return <div className="glass rounded-[14px] p-4 flex flex-col">
        <p className="text-body text-ink-muted">
            {analysis.items.length === 0
                ? "Auf dem Foto wurde kein Essen erkannt."
                : "Prüfe die erkannten Lebensmittel und Mengen, die Mengen sind Schätzungen der KI."}
        </p>
        <MealTabs meal={meal} onChange={setMeal} />

        <div className="flex flex-col gap-y-2 mt-3">
            {items.map((item) => <ItemRow key={item.key} item={item}
                onChange={(change) => updateItem(item.key, change)}
                onRemove={() => removeItem(item.key)} />)}
        </div>

        {error && <Alert variant="destructive" className="mt-3">
            <AlertCircle className="size-4" />
            <AlertTitle>Fehler</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
        </Alert>}

        <div className="flex flex-wrap items-center gap-3 border-t pt-3 mt-4">
            <div className="mr-auto">
                <p className="text-label text-ink-muted">{hint}</p>
                <p className="text-kpi tabular-nums">≈ {formatNumber(totalKcal)} kcal</p>
            </div>
            <button
                className="rounded-[10px] px-5 py-2 font-semibold glass-inset hover:bg-destructive/15 hover:text-destructive disabled:opacity-50 flex items-center gap-x-2"
                disabled={saving !== null}
                onClick={reject}
            >
                {saving === "REJECT" && <Spinner className="size-4" />}Ablehnen
            </button>
            <button
                className="rounded-[10px] bg-brand hover:bg-brand-strong text-on-brand font-semibold px-5 py-2 disabled:opacity-50 flex items-center gap-x-2"
                disabled={!canAccept}
                onClick={accept}
            >
                {saving === "ACCEPT" && <Spinner className="size-4" />}Eintragen
            </button>
        </div>
    </div>
}

function ItemRow({ item, onChange, onRemove }: { item: ReviewItem, onChange: (change: Partial<ReviewItem>) => void, onRemove: () => void }) {
    const status = statusOf(item)
    const [searching, setSearching] = useState(status === "NOT_FOUND")
    const perUnit = gramsPerUnitOf(item)
    const step = isGrams(item.unit) ? 10 : 1

    const statusIcon = {
        OK: <span className="size-6 shrink-0 rounded-full bg-success-bar text-on-brand flex items-center justify-center"><Check className="size-4" /></span>,
        NEW_UNIT: <span className="size-6 shrink-0 rounded-full bg-warning text-on-brand flex items-center justify-center font-bold text-body">?</span>,
        NOT_FOUND: <span className="size-6 shrink-0 rounded-full bg-danger text-on-brand flex items-center justify-center"><X className="size-4" /></span>,
    }[status]

    // keeps the grams when switching to g, starts with one piece for a portion
    const selectUnit = (unit: string, portion: FoodPortion | null) => {
        if (isGrams(unit)) onChange({ unit, amount: Math.round(gramsOf(item)), gramsPerUnit: 1 })
        else onChange({ unit, amount: 1, gramsPerUnit: portion?.grams ?? item.gramsPerUnit })
    }

    // the detected unit stays selectable while the food doesn't have it, it is added as portion then
    const units = [
        ...GRAM_UNITS.filter((u) => u === "g" || item.unit === "ml").map((u) => ({ label: u, portion: null as FoodPortion | null })),
        ...(item.food?.portions ?? []).map((p) => ({ label: p.label, portion: p as FoodPortion | null })),
        ...(status === "NEW_UNIT" ? [{ label: item.unit, portion: null }] : []),
    ]

    return <div className="rounded-[12px] glass-inset p-3" style={{ outline: status === "NEW_UNIT" ? "1.5px solid var(--warning)" : status === "NOT_FOUND" ? "1.5px solid var(--danger)" : "none" }}>
        <div className="flex items-center gap-x-3">
            {statusIcon}
            <div className="flex-1 min-w-0">
                <p className="font-medium truncate">{item.food?.name ?? item.query}</p>
                <p className="text-label text-ink-muted truncate">
                    {status === "NOT_FOUND" ? `„${item.query}“ nicht gefunden` : `erkannt: ${item.query}`}
                    {item.openFoodId !== null && " · Open Food Facts"}
                </p>
            </div>
            {item.food && <div className="text-right">
                <p className="font-bold leading-tight tabular-nums">{formatNumber(kcalOf(item))}</p>
                <p className="text-label text-ink-muted">kcal</p>
            </div>}
            <button aria-label="Entfernen" className="size-6 shrink-0 rounded-full glass-inset flex items-center justify-center text-ink-muted" onClick={onRemove}><X className="size-3.5" /></button>
        </div>

        {item.food && <div className="flex flex-col gap-y-2 mt-3">
            <div className="flex flex-wrap items-center gap-2">
                <div className="flex items-center glass-inset rounded-lg px-2 py-1 gap-x-2">
                    <button aria-label="Weniger" className="text-brand-strong disabled:opacity-40" disabled={item.amount <= step} onClick={() => onChange({ amount: Math.max(step, item.amount - step) })}><Minus className="size-4" /></button>
                    <input
                        aria-label="Menge"
                        className="w-14 bg-transparent text-right font-semibold tabular-nums outline-none"
                        type="number"
                        min={0}
                        value={item.amount === 0 ? "" : item.amount}
                        onChange={(e) => onChange({ amount: e.target.value === "" ? 0 : Math.max(0, Math.round(parseFloat(e.target.value) * 10) / 10) })}
                    />
                    <button aria-label="Mehr" className="text-brand-strong" onClick={() => onChange({ amount: item.amount + step })}><Plus className="size-4" /></button>
                </div>
                {units.map((u) => <button
                    key={u.label}
                    className={`rounded-full px-3 py-1 text-body ${sameLabel(u.label, item.unit) ? "bg-brand text-on-brand" : "glass-inset hover:bg-surface-raised/50 dark:hover:bg-white/5"}`}
                    onClick={() => selectUnit(u.label, u.portion)}
                >{u.label}{u.portion && <span className={sameLabel(u.label, item.unit) ? "text-on-brand/80" : "text-ink-muted"}> {formatNumber(u.portion.grams)} g</span>}</button>)}
                {!isGrams(item.unit) && <span className="text-body text-ink-muted tabular-nums">= {formatNumber(gramsOf(item))} g</span>}
            </div>

            {status === "NEW_UNIT" && <div className="flex flex-wrap items-center gap-2 text-body">
                <span>Neue Portion: 1 {item.unit} =</span>
                <div className="flex items-center glass-inset rounded-lg px-2 py-0.5 gap-x-1">
                    <input
                        aria-label={`Gramm pro ${item.unit}`}
                        className="w-14 bg-transparent text-right font-semibold tabular-nums outline-none"
                        type="number"
                        min={1}
                        value={perUnit === 0 ? "" : perUnit}
                        onChange={(e) => onChange({ gramsPerUnit: e.target.value === "" ? 0 : Math.max(0, parseFloat(e.target.value)) })}
                    />
                    <span className="text-ink-muted">g</span>
                </div>
                <span className="text-label text-ink-muted">geschätzt, wird für {item.food.name} gespeichert</span>
            </div>}
        </div>}

        {searching
            ? <FoodPicker initialQuery={item.query} onCancel={item.food ? () => setSearching(false) : undefined}
                onPicked={(food, openFoodId) => { onChange({ food, openFoodId }); setSearching(false) }} />
            : <button className="text-body text-brand-strong hover:underline mt-2" onClick={() => setSearching(true)}>Anderes Lebensmittel</button>}
    </div>
}

// search like in the food dialog, the picked food is loaded with its portions
function FoodPicker({ initialQuery, onPicked, onCancel }: {
    initialQuery: string
    onPicked: (food: NonNullable<ReviewItem["food"]>, openFoodId: number | null) => void
    onCancel?: () => void
}) {
    const [query, setQuery] = useState(initialQuery)
    const [results, setResults] = useState<SearchFood[]>([])
    const [loading, setLoading] = useState(false)
    const [picking, setPicking] = useState<string | null>(null)
    // debounce typing and drop answers of outdated queries, like the food dialog
    const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null)
    const searchId = useRef(0)

    const search = (value: string) => {
        setQuery(value)
        if (searchTimeout.current) clearTimeout(searchTimeout.current)
        const id = ++searchId.current
        const trimmed = value.trim()
        if (!trimmed) {
            setResults([])
            setLoading(false)
            return
        }
        setLoading(true)
        searchTimeout.current = setTimeout(async () => {
            try {
                const page = await findFoodByNameAndUserId(trimmed)
                if (id === searchId.current) setResults(page.content.slice(0, 6))
            } catch {
                if (id === searchId.current) setResults([])
            } finally {
                if (id === searchId.current) setLoading(false)
            }
        }, 250)
    }

    // results for the name the model gave the food right away
    useEffect(() => {
        const trimmed = initialQuery.trim()
        if (!trimmed) return
        const id = ++searchId.current
        findFoodByNameAndUserId(trimmed)
            .then((page) => { if (id === searchId.current) setResults(page.content.slice(0, 6)) })
            .catch(() => undefined)
        return () => { if (searchTimeout.current) clearTimeout(searchTimeout.current) }
    }, [initialQuery])

    const pick = async (result: SearchFood) => {
        const key = `${result.id}-${result.openFoodId}`
        setPicking(key)
        try {
            if (result.id !== null) {
                onPicked(await getTrackedFoodByFoodId(result.id), null)
            } else {
                onPicked(await getOpenFood(result.openFoodId!), result.openFoodId)
            }
        } finally {
            setPicking(null)
        }
    }

    return <div className="glass-inset rounded-[12px] p-2 mt-3 flex flex-col gap-y-1">
        <div className="flex items-center gap-x-2 px-1">
            <Search className="size-4 text-ink-muted" />
            <input autoFocus className="flex-1 bg-transparent outline-none py-1" value={query} placeholder="Lebensmittel suchen" onChange={(e) => search(e.target.value)} />
            {loading && <Spinner className="size-4" />}
            {onCancel && <button className="text-body text-ink-muted hover:text-foreground" onClick={onCancel}>Abbrechen</button>}
        </div>
        {results.map((result) => {
            const key = `${result.id}-${result.openFoodId}`
            return <button key={key} className="text-left rounded-lg px-2 py-1.5 hover:bg-surface-raised/50 dark:hover:bg-white/5 flex items-center gap-x-2 disabled:opacity-50" disabled={picking !== null} onClick={() => pick(result)}>
                <span className="flex-1">{result.name}{result.openFoodId !== null && <span className="text-label text-ink-muted"> · Open Food Facts</span>}</span>
                {picking === key && <Spinner className="size-4" />}
            </button>
        })}
        {!loading && query.trim() && results.length === 0 && <p className="text-body text-ink-muted px-2 py-1">Nichts gefunden</p>}
    </div>
}
