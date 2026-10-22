// how strongly an exercise works a muscle: 1 a little, 2 clearly, 3 it's the main muscle
export const MUSCLE_LEVELS = ["#ff9f0a55", "#ff9f0aaa", "#ff9f0a"]

export const getNewSlugColor = (intensity: number): string => MUSCLE_LEVELS[intensity] ?? ""
