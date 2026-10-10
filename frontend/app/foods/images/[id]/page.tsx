import ErrorState from "@/components/ErrorState"
import { getFoodImage } from "../action"
import FoodImageReview from "./FoodImageReview"

export default async function FoodImagePage({ params }: PageProps<"/foods/images/[id]">) {
    const { id } = await params
    const analysis = /^\d+$/.test(id) ? await getFoodImage(Number(id)) : { ok: false as const, error: "Foto nicht gefunden" }

    return <div className="flex-1 w-full bg-muted/50 px-3.5 py-5 md:px-6 md:py-7">
        <div className="mx-auto flex max-w-2xl flex-col gap-5">
            <h1 className="text-[34px] font-bold leading-tight tracking-tight">Foto prüfen</h1>
            {analysis.ok
                // a new status (e.g. analyzed while the page was open) starts the review from scratch
                ? <FoodImageReview key={analysis.data.status} analysis={analysis.data} />
                : <ErrorState title="Foto nicht verfügbar" message={analysis.error} />}
        </div>
    </div>
}
