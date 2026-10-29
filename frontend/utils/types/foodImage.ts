import type { Schemas } from "@/lib/api/types"

export type FoodImageItem = Schemas["FoodImageItemDto"]
export type FoodImageAnalysis = Schemas["FoodImageAnalysisDto"]
// PENDING until the job analyzed the photo, READY until the user accepted or rejected it
export type FoodImageStatus = FoodImageAnalysis["status"]
// failed photos don't count
export type FoodImageUsage = Schemas["FoodImageUsageDto"]
// a unit the food doesn't have yet is added as portion with gramsPerUnit
export type AcceptFoodImage = Schemas["AcceptFoodImageDto"]

// served by the route handler next to the review page, it adds the access token
export const foodImageUrl = (id: number) => `/foods/images/${id}/image`
