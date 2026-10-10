'use client'

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Spinner } from "@/components/ui/spinner"
import { Meal } from "@/utils/types/food"
import { FoodImageAnalysis, FoodImageStatus, FoodImageUsage, foodImageUrl } from "@/utils/types/foodImage"
import { formatDistanceToNow } from "date-fns"
import { de } from "date-fns/locale"
import { AlertCircle, Camera, CheckCircle2 } from "lucide-react"
import Link from "next/link"
import { useEffect, useRef, useState } from "react"
import { getFoodImages, getFoodImageUsage, uploadFoodImage } from "../images/action"

// enough to recognize the food, keeps the upload and the stored photo small
const MAX_SIDE = 1600
const JPEG_QUALITY = 0.85
// same limit as the backend
const MAX_DESCRIPTION_LENGTH = 500

export const STATUS_LABEL: Record<FoodImageStatus, { label: string, className: string }> = {
    PENDING: { label: "Wird ausgewertet", className: "bg-blue-500/10 text-blue-500" },
    READY: { label: "Prüfen", className: "bg-orange-400/15 text-orange-500" },
    FAILED: { label: "Fehlgeschlagen", className: "bg-destructive/10 text-destructive" },
    ACCEPTED: { label: "Eingetragen", className: "bg-green-500/10 text-green-600" },
    REJECTED: { label: "Abgelehnt", className: "bg-muted text-muted-foreground" },
}

// the browser decodes the photo (also HEIC on Safari) and turns the EXIF rotation into pixels,
// the original is sent if it can't decode it
async function downscale(file: File): Promise<Blob> {
    try {
        const bitmap = await createImageBitmap(file)
        const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
        const canvas = document.createElement("canvas")
        canvas.width = Math.round(bitmap.width * scale)
        canvas.height = Math.round(bitmap.height * scale)
        canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
        bitmap.close()
        const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", JPEG_QUALITY))
        return blob ?? file
    } catch {
        return file
    }
}

const PhotoFoodInput: React.FC<{ meal: Meal }> = ({ meal }) => {
    const input = useRef<HTMLInputElement>(null)
    const [usage, setUsage] = useState<FoodImageUsage | null>(null)
    const [recent, setRecent] = useState<FoodImageAnalysis[]>([])
    const [uploading, setUploading] = useState(false)
    const [uploaded, setUploaded] = useState<FoodImageAnalysis | null>(null)
    const [description, setDescription] = useState("")
    const [error, setError] = useState<string | null>(null)

    const load = () => Promise.all([getFoodImageUsage(), getFoodImages(5)]).then(([usage, recent]) => {
        if (usage.ok) setUsage(usage.data)
        if (recent.ok) setRecent(recent.data)
    })

    useEffect(() => {
        let active = true
        Promise.all([getFoodImageUsage(), getFoodImages(5)]).then(([usage, recent]) => {
            if (!active) return
            if (usage.ok) setUsage(usage.data)
            if (recent.ok) setRecent(recent.data)
        })
        return () => { active = false }
    }, [])

    const upload = async (file: File) => {
        setError(null)
        setUploaded(null)
        setUploading(true)
        try {
            const photo = await downscale(file)
            const formData = new FormData()
            formData.append("image", photo, photo === file ? file.name : "meal.jpg")
            formData.append("meal", meal)
            if (description.trim()) formData.append("description", description.trim())
            const result = await uploadFoodImage(formData)
            if (result.ok) {
                setUploaded(result.data)
                setDescription("")
                await load()
            } else {
                setError(result.error)
            }
        } catch {
            setError("Foto konnte nicht hochgeladen werden")
        } finally {
            setUploading(false)
            // the same photo can be picked again after an error
            if (input.current) input.current.value = ""
        }
    }

    const left = usage ? Math.max(0, usage.limit - usage.used) : null

    return <div className="flex flex-col mt-3 gap-y-3">
        <div className="bg-accent rounded-2xl p-4 flex flex-col items-center gap-y-3 text-center">
            <span className="size-12 rounded-full bg-blue-500/10 text-blue-500 flex items-center justify-center"><Camera className="size-6" /></span>
            <div>
                <p className="font-semibold">Foto deiner Mahlzeit</p>
                <p className="text-sm text-muted-foreground">Die KI erkennt die Lebensmittel und schätzt die Mengen. Du bekommst eine Benachrichtigung, sobald du sie prüfen kannst.</p>
            </div>
            <textarea
                className="w-full bg-background rounded-xl px-3 py-2 resize-none outline-none text-sm min-h-16"
                value={description}
                maxLength={MAX_DESCRIPTION_LENGTH}
                placeholder="Optional: Was gibt es? z. B. Spaghetti Bolognese mit Parmesan, dazu ein Glas Apfelschorle"
                onChange={(e) => setDescription(e.target.value)}
            />
            <input
                ref={input}
                type="file"
                accept="image/*"
                capture="environment"
                className="hidden"
                onChange={(e) => { const file = e.target.files?.[0]; if (file) upload(file) }}
            />
            <button
                className="rounded-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 disabled:opacity-50 flex items-center gap-x-2"
                disabled={uploading || left === 0}
                onClick={() => input.current?.click()}
            >
                {uploading ? <Spinner className="size-4" /> : <Camera className="size-4" />}Foto aufnehmen oder auswählen
            </button>
            {left !== null && <p className="text-xs text-muted-foreground tabular-nums">
                {left === 0 ? "Heute sind keine Fotos mehr übrig, morgen geht es weiter" : `Noch ${left} von ${usage!.limit} Fotos heute`}
            </p>}
        </div>

        {uploaded && <Alert>
            <CheckCircle2 className="size-4 text-green-600" />
            <AlertTitle>Foto wird ausgewertet</AlertTitle>
            <AlertDescription>
                Das dauert meist nur ein paar Sekunden, du bekommst dann eine Benachrichtigung.{" "}
                <Link href={`/foods/images/${uploaded.id}`} className="text-blue-500 hover:underline">Jetzt öffnen</Link>
            </AlertDescription>
        </Alert>}

        {error && <Alert variant="destructive">
            <AlertCircle className="size-4" />
            <AlertTitle>Fehler</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
        </Alert>}

        {recent.length > 0 && <>
            <h2 className="text-xs uppercase tracking-wide text-muted-foreground mt-1">Letzte Fotos</h2>
            <ul className="flex flex-col gap-y-1">
                {recent.map((photo) => <li key={photo.id}>
                    <Link href={`/foods/images/${photo.id}`} className="flex items-center gap-x-3 rounded-2xl p-2 hover:bg-accent">
                        {/* eslint-disable-next-line @next/next/no-img-element -- private photo through our route handler */}
                        <img src={foodImageUrl(photo.id)} alt="" className="size-12 rounded-xl object-cover bg-muted" />
                        <div className="flex-1 min-w-0">
                            <p className="font-medium truncate">{photo.items.length > 0 ? photo.items.map((i) => i.food?.name ?? i.query).join(", ") : "Foto"}</p>
                            <p className="text-xs text-muted-foreground">{formatDistanceToNow(new Date(photo.createdAt), { addSuffix: true, locale: de })}</p>
                        </div>
                        <span className={`text-xs font-medium rounded-full px-2.5 py-1 ${STATUS_LABEL[photo.status].className}`}>{STATUS_LABEL[photo.status].label}</span>
                    </Link>
                </li>)}
            </ul>
        </>}
    </div>
}

export default PhotoFoodInput
