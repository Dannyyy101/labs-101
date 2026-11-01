import type { Schemas } from "@/lib/api/types"

// a page of Spring Data, every page has the same fields
export type Page<T> = Omit<Schemas["PageFoodDto"], "content"> & { content: T[] }

// a slice of Spring Data, it doesn't count the pages
export type Slice<T> = Omit<Schemas["SliceSearchFoodResponseDto"], "content"> & { content: T[] }
