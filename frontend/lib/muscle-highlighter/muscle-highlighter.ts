export const getNewSlugColor = (intensity: number): string => {
    switch (intensity) {
        case 0:
            return "#00ff00"
        case 1:
            return "#0000ff"
        case 2:
            return "#ff0000"
        default:
            return ""
    }
}