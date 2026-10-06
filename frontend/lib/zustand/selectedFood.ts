import { Meal } from "@/utils/types/food"
import { create } from "zustand"


export enum SelectedFoodAction {
    CREATING,
    EDITING
}

export interface SelectedFoodState {
    meal: Meal | null
    foodId: number | null,
    // set instead of foodId for an open food, it only gets imported when tracked
    openFoodId: number | null,
    trackedFoodId: number | null
    action: SelectedFoodAction | null
    selectFood: (meal: Meal, foodId: number, action: SelectedFoodAction, trackedFoodId?: number) => void
    selectOpenFood: (meal: Meal, openFoodId: number) => void
    unselect: () => void;
    setFoodId: (foodId: number | null) => void
    setTrackedFoodId: (trackedFoodId: number | null) => void
    setMeal: (meal: Meal) => void
    showSearch: boolean
    setShowSearch: (showSearch: boolean) => void
}

export const useSelectedFoodStore = create<SelectedFoodState>((set) => ({
    meal: null,
    foodId: null,
    openFoodId: null,
    trackedFoodId: null,
    action: null,
    showSearch: false,
    selectFood: ((meal, foodId, action, trackedFoodId) => set(() => ({ meal, foodId, openFoodId: null, trackedFoodId, action }))),
    selectOpenFood: (meal, openFoodId) => set(() => ({ meal, foodId: null, openFoodId, trackedFoodId: null, action: SelectedFoodAction.CREATING })),
    unselect: () => set(() => ({ meal: null, foodId: null, openFoodId: null, action: null, trackedFoodId: null })),
    setFoodId: (foodId) => set((state) => ({ ...state, foodId, openFoodId: null })),
    setTrackedFoodId: (trackedFoodId) => set((state) => ({ ...state, trackedFoodId })),
    setShowSearch: (showSearch) => set((state) => ({ ...state, showSearch })),
    setMeal: (meal) => set((state) => ({ ...state, meal }))

}))