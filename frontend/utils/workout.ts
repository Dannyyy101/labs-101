import { WorkoutTemplate } from "./types/workoutTypes";


export const sortWorkoutTemplates = (templates: WorkoutTemplate[]): Map<string, WorkoutTemplate[]> => {
    const sortedTemplates = new Map<string, WorkoutTemplate[]>();

    templates.forEach((template) => {
        const item = sortedTemplates.get(template.type) || [];
        sortedTemplates.set(template.type, [...item, template])
    })
    return sortedTemplates;
}