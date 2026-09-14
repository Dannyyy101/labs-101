import {
    Table,
    TableBody,
    TableCaption,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table"
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
import EditFood from "./EditFood"
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

    const hrefFor = (p: number) => {
        const params = new URLSearchParams({ page: String(p), sort })
        if (query) params.set("query", query)
        return `?${params}`
    }


    return (
        <div className="px-8">
            <FoodSearch initialQuery={query} />
            <div className="max-h-[80vh] max-w-[90vw] overflow-auto rounded-md border mt-4">
                <FoodTable foods={data.content} />
            </div>
            <Pagination className="mt-4">
                <PaginationContent>
                    <PaginationItem>
                        <PaginationPrevious
                            href={hrefFor(page - 1)}
                            aria-disabled={page <= 1}
                            className={page <= 1 ? "pointer-events-none opacity-50" : ""}
                        />
                    </PaginationItem>

                    {pageItems(page, totalPages).map((p, i) =>
                        p === "..." ? (
                            <PaginationItem key={`e${i}`}>
                                <PaginationEllipsis />
                            </PaginationItem>
                        ) : (
                            <PaginationItem key={p}>
                                <PaginationLink href={hrefFor(p)} isActive={p === page}>
                                    {p}
                                </PaginationLink>
                            </PaginationItem>
                        )
                    )}

                    <PaginationItem>
                        <PaginationNext
                            href={hrefFor(page + 1)}
                            aria-disabled={page >= totalPages}
                            className={page >= totalPages ? "pointer-events-none opacity-50" : ""}
                        />
                    </PaginationItem>
                </PaginationContent>
            </Pagination>
        </div>
    )
}