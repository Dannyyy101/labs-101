'use client'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { InputGroup, InputGroupInput, InputGroupAddon } from "@/components/ui/input-group";
import { Spinner } from "@/components/ui/spinner";
import { AlertCircle, Barcode, Check, Minus, Plus, Search, X } from "lucide-react";
import InfiniteScroll from "react-infinite-scroll-component";
import { ExtractedFood, Food, FoodPortion, FoodWithPortion, Meal, SearchFood } from "@/utils/types/food";
import { ReactNode, useState } from "react";
import { addFoodPortion, createTrackFood, extractFoodsFromText, findAndSafeFoodIfNotExistByBarcode, findFoodByNameAndUserId, importOpenFood, trackOpenFood } from "./action";
import { Button } from "@/components/ui/button";
import FoodTrackView from "./FoodTrackView";
import ScannerPanel from "@/components/ScannerPanel";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { SelectedFoodAction, SelectedFoodState } from "@/lib/zustand/selectedFood";
import { MEALS } from "./MealView";

const FoodSearch: React.FC<{ meal: Meal, selectedFoodStore: SelectedFoodState, children?: ReactNode, className?: string }> = ({ meal, selectedFoodStore, children, className }) => {

    const [food, setFood] = useState<SearchFood[]>([])
    const [hasMore, setHasMore] = useState(true);
    const [searchInput, setSearchInput] = useState<string>("")
    const [open, setOpen] = useState<boolean>(false)
    const [showBarcodeScanner, setShowBarcodeScanner] = useState<boolean>(false)
    const [page, setPage] = useState(0)

    const findByName = async (name: string) => {
        setSearchInput(name)
        const result = await findFoodByNameAndUserId(name)
        setFood(result.content)
        setPage(0)
        setHasMore(!result.last)
    }

    const fetchMore = async () => {
        const next = await findFoodByNameAndUserId(searchInput, { page: page + 1 })
        setFood((prev) => [...prev, ...next.content])
        setPage(next.number)
        setHasMore(!next.last)
    }
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)

    const lookupFood = async (code: string) => {
        setError(null)
        setLoading(true)
        const result = await findAndSafeFoodIfNotExistByBarcode(code)
        setLoading(false)

        if (result.ok) {
            selectedFoodStore.selectFood(meal, result.data.id, SelectedFoodAction.CREATING)
            setShowBarcodeScanner(false)
        } else {
            setError(result.error)
        }
    }

    const toggleBarcodeScanner = () => {
        setShowBarcodeScanner((prev) => !prev)
        setError(null)
    }
    const [showDialog, setShowDialog] = useState<"SEARCH" | "TEXT">("SEARCH")

    const close = () => {
        selectedFoodStore.unselect()
        selectedFoodStore.setShowSearch(false)
    }

    // a food selected from the search is shown right here instead of the list
    const selectedFood = selectedFoodStore.action === SelectedFoodAction.CREATING ? selectedFoodStore.foodId : null

    return <Dialog open={selectedFoodStore.showSearch} onOpenChange={(open) => { if (open) selectedFoodStore.setShowSearch(true); else close() }}>
        <DialogTrigger className={className}>{children}</DialogTrigger>
        <DialogContent className="w-4xl flex flex-col" showCloseButton={false}>
            <DialogHeader className="grid grid-cols-[1fr_auto_1fr] items-center">
                <button className="text-blue-500 hover:text-blue-600 justify-self-start" onClick={close}>Abbrechen</button>
                <DialogTitle className="text-center text-lg">Essen hinzufügen</DialogTitle>
            </DialogHeader>
            <section>
                <div className="flex gap-x-2">
                    {(["TEXT", "SEARCH"] as const).map((mode) =>
                        <button key={mode} onClick={() => { setShowDialog(mode); selectedFoodStore.setFoodId(null) }}
                            className={`rounded-full px-4 py-1.5 font-medium ${showDialog === mode ? "bg-foreground text-background" : "bg-accent"}`}>
                            {mode === "TEXT" ? "Per Text" : "Suchen"}
                        </button>
                    )}
                </div>
                <MealTabs meal={selectedFoodStore.meal ?? meal} onChange={selectedFoodStore.setMeal} />
                {selectedFood != null ?
                    <div className="mt-4">
                        <FoodTrackView key={selectedFood} props={{
                            meal: selectedFoodStore.meal ?? meal,
                            foodId: selectedFood,
                            back: () => selectedFoodStore.setFoodId(null),
                            closeView: close,
                            selectedFoodStore,
                        }} />
                    </div>
                    :
                    showDialog === "SEARCH" ?
                    <>
                        <InputGroup className="w-full mt-2 h-10 mb-2 rounded-md">
                            <InputGroupInput value={searchInput} placeholder="Suchen..." onChange={(e) => findByName(e.target.value)} />
                            <InputGroupAddon>
                                <Search />
                            </InputGroupAddon>
                            <button className="mr-2" onClick={toggleBarcodeScanner}><Barcode /></button>
                        </InputGroup>
                        {error && (
                            <Alert variant="destructive" className="mt-2">
                                <AlertCircle className="size-4" />
                                <AlertTitle>Fehler</AlertTitle>
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        {loading && <div className="w-full flex justify-center mt-2"><Spinner className="size-4" /></div>}
                        {showBarcodeScanner ? <ScannerPanel
                            onDetected={(ean) => {
                                setShowBarcodeScanner(false)
                                lookupFood(ean)
                            }}
                        /> :
                            <InfiniteScroll
                                className="rounded-2xl w-full max-h-96 overflow-y-auto mt-2 flex flex-col"
                                dataLength={food.length}
                                next={fetchMore}
                                hasMore={hasMore}
                                loader={<div className="w-full flex justify-center"><Spinner className="size-4" /></div>}
                                endMessage={<p style={{ textAlign: 'center' }}>All items loaded.</p>}
                            >
                                {food.map((f) => (
                                    <Button
                                        className="hover:bg-accent  max-w-96 text-left flex justify-start whitespace-normal h-auto py-2"
                                        variant="ghost"
                                        onClick={() => selectedFoodStore.selectFood(meal, f.id, SelectedFoodAction.CREATING)}
                                        key={f.id}
                                    >
                                        {f.name}
                                    </Button>
                                ))}
                            </InfiniteScroll>
                        }
                    </>
                    :
                    <TextFoodInput
                        meal={selectedFoodStore.meal ?? meal}
                        onTracked={() => selectedFoodStore.setShowSearch(false)}
                    />
                }

            </section>

        </DialogContent>
    </Dialog>
}

export const MealTabs: React.FC<{ meal: Meal, onChange: (meal: Meal) => void }> = ({ meal, onChange }) =>
    <div className="bg-accent flex rounded-xl p-1 mt-3">
        {MEALS.map((m) =>
            <button key={m.type} onClick={() => onChange(m.type)}
                className={`w-full rounded-lg py-1.5 font-semibold ${meal === m.type ? "bg-background shadow-sm" : ""}`}>
                {m.label}
            </button>
        )}
    </div>

type PreviewStatus = "OK" | "NEW_UNIT" | "NOT_FOUND"

interface PreviewItem {
    key: number
    query: string
    amount: number
    unit: string
    food: ExtractedFood["food"]
    // set while the food only exists in the open food database
    openFoodId: number | null
    portion: FoodPortion | null
    // grams per unit while the user still has to confirm a new unit
    unitGrams: number
    status: PreviewStatus
}

// ml are counted 1:1 as grams
const GRAM_UNITS = ["g", "ml"]

const UNIT_GUESSES: Record<string, number> = {
    tasse: 200, glas: 200, becher: 250, schüssel: 300, schale: 250, portion: 150, dose: 400, flasche: 500,
    scheibe: 30, handvoll: 30, esslöffel: 15, teelöffel: 5, stück: 100, riegel: 40, kugel: 60, prise: 1,
}

const sameLabel = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase()

const toPreviewItem = (extracted: ExtractedFood, key: number): PreviewItem => {
    const { food, amount } = extracted
    const unit = extracted.unit ?? food?.portions.find((p) => p.isDefault)?.label ?? "Stück"
    const base = { key, query: extracted.query, amount, unit, food, openFoodId: extracted.openFoodId, portion: null, unitGrams: UNIT_GUESSES[unit.toLowerCase()] ?? 100 }

    if (!food) return { ...base, status: "NOT_FOUND" }
    if (GRAM_UNITS.includes(unit)) return { ...base, unitGrams: 1, status: "OK" }

    const portion = food.portions.find((p) => sameLabel(p.label, unit))
    if (portion) return { ...base, portion, unitGrams: portion.grams, status: "OK" }

    return { ...base, status: "NEW_UNIT" }
}

const gramsOf = (item: PreviewItem) => item.amount * item.unitGrams
const kcalOf = (item: PreviewItem) => Math.round((item.food?.kcal ?? 0) * gramsOf(item) / 100)

const TextFoodInput: React.FC<{ meal: Meal, onTracked: () => void }> = ({ meal, onTracked }) => {
    const [text, setText] = useState("")
    const [items, setItems] = useState<PreviewItem[]>([])
    const [extracting, setExtracting] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const extract = async () => {
        if (!text.trim()) return
        setError(null)
        setExtracting(true)
        try {
            const result = await extractFoodsFromText(text)
            setItems(result.map(toPreviewItem))
        } catch {
            setError("Text konnte nicht erkannt werden")
        } finally {
            setExtracting(false)
        }
    }

    const updateItem = (key: number, change: Partial<PreviewItem>) =>
        setItems((prev) => prev.map((item) => item.key === key ? { ...item, ...change } : item))

    const removeItem = (key: number) => setItems((prev) => prev.filter((item) => item.key !== key))

    const saveUnit = async (item: PreviewItem) => {
        if (!item.food) return
        setError(null)
        try {
            // a unit needs a food, so open foods get copied into our foods first
            const food = item.openFoodId !== null ? await importOpenFood(item.openFoodId) : { ...item.food, id: item.food.id! }
            const portions = await addFoodPortion(food.id, { label: item.unit, grams: item.unitGrams, isDefault: false })
            const portion = portions.find((p) => sameLabel(p.label, item.unit)) ?? null
            updateItem(item.key, { food: { ...food, portions }, openFoodId: null, portion, status: portion ? "OK" : "NEW_UNIT" })
        } catch {
            setError(`Einheit „${item.unit}“ konnte nicht gespeichert werden`)
        }
    }

    const trackAll = async () => {
        setSubmitting(true)
        setError(null)
        try {
            for (const item of items) {
                const trackedFood = {
                    amount: item.portion ? item.amount : gramsOf(item),
                    portionId: item.portion?.id ?? null,
                    meal,
                }
                if (item.openFoodId !== null) await trackOpenFood(item.openFoodId, trackedFood)
                else await createTrackFood({ ...trackedFood, foodId: item.food!.id! })
            }
            setText("")
            setItems([])
            onTracked()
        } catch {
            setError("Eintragen fehlgeschlagen")
        } finally {
            setSubmitting(false)
        }
    }

    const pendingUnits = items.filter((i) => i.status === "NEW_UNIT").length
    const notFound = items.filter((i) => i.status === "NOT_FOUND").length
    const totalKcal = items.reduce((sum, i) => sum + kcalOf(i), 0)
    const canSubmit = items.length > 0 && pendingUnits === 0 && notFound === 0 && !submitting

    const footerHint = notFound > 0
        ? `${notFound} nicht gefunden – bitte entfernen`
        : pendingUnits > 0
            ? `${pendingUnits} neue ${pendingUnits === 1 ? "Einheit" : "Einheiten"} bestätigen`
            : `${items.length} Lebensmittel`

    return <div className="flex flex-col mt-3">
        <div className="bg-accent rounded-2xl p-4 flex flex-col gap-y-3">
            <textarea
                className="bg-transparent resize-none outline-none text-base min-h-16"
                value={text}
                placeholder="z. B. 2 Eier, eine Tasse Cappuccino, 1 Banane und 30 g Mandeln"
                onChange={(e) => setText(e.target.value)}
                onKeyDown={(e) => {
                    if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) extract()
                }}
            />
            <div className="flex items-end justify-between gap-x-4">
                <p className="text-xs text-muted-foreground">Menge · Einheit · Lebensmittel, getrennt durch Komma oder „und“</p>
                <button
                    className="shrink-0 rounded-full bg-blue-500 text-white font-semibold px-4 py-1.5 disabled:opacity-50 flex items-center gap-x-2"
                    disabled={extracting || !text.trim()}
                    onClick={extract}
                >
                    {extracting && <Spinner className="size-4" />}Erkennen
                </button>
            </div>
        </div>

        {error && (
            <Alert variant="destructive" className="mt-2">
                <AlertCircle className="size-4" />
                <AlertTitle>Fehler</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
            </Alert>
        )}

        {items.length > 0 && <>
            <h2 className="text-xs uppercase tracking-wide text-muted-foreground mt-4 mb-2">Vorschau</h2>
            <div className="flex flex-col gap-y-2 max-h-[45vh] overflow-y-auto">
                {items.map((item) => <PreviewRow key={item.key} item={item} onChange={(change) => updateItem(item.key, change)} onRemove={() => removeItem(item.key)} onSaveUnit={() => saveUnit(item)} />)}
            </div>
            <div className="flex items-center justify-between border-t pt-3 mt-3">
                <div>
                    <p className="text-xs text-muted-foreground">{footerHint}</p>
                    <p className="text-xl font-bold">{totalKcal} kcal</p>
                </div>
                <button
                    className="rounded-full bg-blue-500 text-white font-semibold px-5 py-2 disabled:bg-accent disabled:text-muted-foreground flex items-center gap-x-2"
                    disabled={!canSubmit}
                    onClick={trackAll}
                >
                    {submitting && <Spinner className="size-4" />}Eintragen
                </button>
            </div>
        </>}
    </div>
}

const PreviewRow: React.FC<{ item: PreviewItem, onChange: (change: Partial<PreviewItem>) => void, onRemove: () => void, onSaveUnit: () => void }> = ({ item, onChange, onRemove, onSaveUnit }) => {
    const [saving, setSaving] = useState(false)
    const isNewUnit = item.status === "NEW_UNIT"
    const approx = isNewUnit ? "≈ " : ""
    const step = item.unitGrams > 50 ? 10 : 5
    const guess = UNIT_GUESSES[item.unit.toLowerCase()] ?? 100
    const presets = [guess - 50, guess, guess + 50].filter((g) => g > 0)

    const statusIcon = {
        OK: <span className="size-6 rounded-full bg-green-500 text-white flex items-center justify-center"><Check className="size-4" /></span>,
        NEW_UNIT: <span className="size-6 rounded-full bg-orange-400 text-white flex items-center justify-center font-bold text-sm">?</span>,
        NOT_FOUND: <span className="size-6 rounded-full bg-red-500 text-white flex items-center justify-center"><X className="size-4" /></span>,
    }[item.status]

    const subtitle = item.status === "NOT_FOUND"
        ? "Nicht gefunden"
        : GRAM_UNITS.includes(item.unit)
            ? `${item.amount} ${item.unit}`
            : `${item.amount} ${item.unit} · ${approx}${Math.round(gramsOf(item))} g`

    return <div className="rounded-2xl bg-accent p-3" style={{ outline: isNewUnit ? "1.5px solid var(--color-orange-400)" : "none" }}>
        <div className="flex items-center gap-x-3">
            {statusIcon}
            <div className="flex-1 min-w-0">
                <p className="font-medium truncate">{item.food?.name ?? item.query}</p>
                <p className="text-xs text-muted-foreground">{subtitle}{item.openFoodId !== null && " · Open Food Facts"}</p>
            </div>
            {item.food && <div className="text-right">
                <p className="font-bold leading-tight">{approx}{kcalOf(item)}</p>
                <p className="text-xs text-muted-foreground">kcal</p>
            </div>}
            <button className="size-6 rounded-full bg-background/60 flex items-center justify-center text-muted-foreground" onClick={onRemove}><X className="size-3.5" /></button>
        </div>

        {isNewUnit && <div className="bg-background rounded-xl p-3 mt-3 flex flex-col gap-y-3">
            <p><span className="font-semibold">Neue Einheit:</span> „{item.unit}“ gibt es für {item.food?.name} noch nicht. Wie viel Gramm sind 1 {item.unit}?</p>
            <div className="flex flex-wrap items-center gap-2">
                <div className="flex items-center bg-accent rounded-lg px-2 py-1 gap-x-2">
                    <button className="text-blue-500" onClick={() => onChange({ unitGrams: Math.max(step, item.unitGrams - step) })}><Minus className="size-4" /></button>
                    <input
                        className="w-12 bg-transparent text-right font-semibold outline-none"
                        type="number"
                        min={1}
                        value={item.unitGrams}
                        onChange={(e) => onChange({ unitGrams: Math.max(0, Number(e.target.value)) })}
                    />
                    <span className="text-muted-foreground">g</span>
                    <button className="text-blue-500" onClick={() => onChange({ unitGrams: item.unitGrams + step })}><Plus className="size-4" /></button>
                </div>
                {presets.map((grams) => <button
                    key={grams}
                    className="rounded-full px-3 py-1"
                    style={{ backgroundColor: item.unitGrams === grams ? "var(--color-blue-500)" : "var(--accent)", color: item.unitGrams === grams ? "white" : "inherit" }}
                    onClick={() => onChange({ unitGrams: grams })}
                >{grams} g</button>)}
            </div>
            <div className="flex items-center justify-between">
                <p className="text-xs text-muted-foreground">1 {item.unit} = {item.unitGrams} g · {Math.round((item.food?.kcal ?? 0) * item.unitGrams / 100)} kcal</p>
                <button
                    className="rounded-full bg-orange-400 text-white font-semibold px-4 py-1.5 disabled:opacity-50 flex items-center gap-x-2"
                    disabled={saving || item.unitGrams <= 0}
                    onClick={async () => {
                        setSaving(true)
                        await onSaveUnit()
                        setSaving(false)
                    }}
                >
                    {saving && <Spinner className="size-4" />}Einheit speichern
                </button>
            </div>
        </div>}
    </div>
}

export default FoodSearch;