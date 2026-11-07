import { Card } from "@/components/dashboard"
import {
    Pagination,
    PaginationContent,
    PaginationEllipsis,
    PaginationItem,
    PaginationLink,
    PaginationNext,
    PaginationPrevious,
} from "@/components/ui/pagination"
import { getAllFoods } from "./action"
import { FoodSearch } from "./FoodSearch"
import FoodTable from "./FoodTable"

function pageItems(current: number, total: number): (number | "...")[] {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

    const wanted = [1, total, current, current - 1, current + 1]
    const pages = [...new Set(wanted)]
        .filter((p) => p >= 1 && p <= total)
        .sort((a, b) => a - b)

    const out: (number | "...")[] = []
    let prev = 0
    for (const p of pages) {
        if (p - prev > 1) out.push("...")
        out.push(p)
        prev = p
    }
    return out
}

export default async function Page({
    searchParams,
}: {
    searchParams: Promise<{ [key: string]: string | string[] | undefined }>
}) {
    const sp = await searchParams
    const page = Math.max(0, Number(sp.page) || 0)
    const sort = sp.sort === "desc" ? "desc" : "asc"
    const query = typeof sp.query === "string" ? sp.query : ""

    const data = await getAllFoods(page, sort, query)
    const totalPages = Math.max(1, data.totalPages)
    // the backend counts pages from 0, people from 1
    const current = page + 1

    const hrefFor = (p: number) => {
        const params = new URLSearchParams({ page: String(p - 1), sort })
        if (query) params.set("query", query)
        return `?${params}`
    }

    return (
        <div className="w-full flex-1 px-3.5 py-5 md:px-6 md:py-7">
            <div className="mx-auto flex max-w-5xl flex-col gap-3 md:gap-4">
                <div className="flex flex-wrap items-end gap-x-4 gap-y-3">
                    <div className="min-w-48 flex-1">
                        <h1 className="text-[22px] font-bold leading-7 tracking-tight">Lebensmittel</h1>
                        <p className="text-label text-ink">
                            {data.totalElements.toLocaleString("de-DE")} {data.totalElements === 1 ? "Eintrag" : "Einträge"}{query && ` für „${query}“`}
                        </p>
                    </div>
                    <FoodSearch initialQuery={query} />
                </div>

                <Card className="flex flex-col p-0">
                    <div className="max-h-[calc(100dvh-19rem)] overflow-auto md:max-h-[calc(100dvh-17rem)]">
                        <FoodTable foods={data.content} />
                    </div>
                    <div className="flex items-center gap-3 border-t border-line px-4 py-2">
                        <span className="hidden whitespace-nowrap text-label text-ink-muted sm:block">Seite {current} von {totalPages}</span>
                        <Pagination className="justify-end">
                            <PaginationContent>
                                <PaginationItem>
                                    <PaginationPrevious text="Zurück" aria-label="Vorherige Seite"
                                        href={hrefFor(current - 1)}
                                        aria-disabled={current <= 1}
                                        className={current <= 1 ? "pointer-events-none opacity-50" : ""}
                                    />
                                </PaginationItem>

                                {pageItems(current, totalPages).map((p, i) =>
                                    p === "..." ? (
                                        <PaginationItem key={`e${i}`}>
                                            <PaginationEllipsis />
                                        </PaginationItem>
                                    ) : (
                                        <PaginationItem key={p}>
                                            <PaginationLink href={hrefFor(p)} isActive={p === current} className="tabular-nums">
                                                {p}
                                            </PaginationLink>
                                        </PaginationItem>
                                    )
                                )}

                                <PaginationItem>
                                    <PaginationNext text="Weiter" aria-label="Nächste Seite"
                                        href={hrefFor(current + 1)}
                                        aria-disabled={current >= totalPages}
                                        className={current >= totalPages ? "pointer-events-none opacity-50" : ""}
                                    />
                                </PaginationItem>
                            </PaginationContent>
                        </Pagination>
                    </div>
                </Card>
            </div>
        </div>
    )
}
