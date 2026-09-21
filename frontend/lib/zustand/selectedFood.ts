import { Meal } from "@/utils/types/food"
import { create } from "zustand"


export enum SelectedFoodAction {
    CREATING,
    EDITING
}

export interface SelectedFoodState {
    meal: Meal | null
    foodId: number | null,
    trackedFoodId: number | null
    action: SelectedFoodAction | null
    selectFood: (meal: Meal, foodId: number, action: SelectedFoodAction, trackedFoodId?: number) => void
    unselect: () => void;
    setFoodId: (foodId: number | null) => void
    setTrackedFoodId: (trackedFoodId: number | null) => void
    showSearch: boolean
    setShowSearch: (showSearch: boolean) => void
}

export const useSelectedFoodStore = create<SelectedFoodState>((set) => ({
    meal: null,
    foodId: null,
    trackedFoodId: null,
    action: null,
    showSearch: false,
    selectFood: ((meal, foodId, action, trackedFoodId) => set(() => ({ meal, foodId, trackedFoodId, action }))),
    unselect: () => set(() => ({ meal: null, foodId: null, action: null, trackedFoodId: null })),
    setFoodId: (foodId) => set((state) => ({ ...state, foodId })),
    setTrackedFoodId: (trackedFoodId) => set((state) => ({ ...state, trackedFoodId })),
    setShowSearch: (showSearch) => set((state) => ({ ...state, showSearch }))

}))