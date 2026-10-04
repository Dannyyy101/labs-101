const WEEKDAYS_SHORT = ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]
const MONTHS_SHORT = ["Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"]

export const WEEKDAY = (date: Date) => WEEKDAYS_SHORT[date.getDay()]

// 12.345 -> "12,3"
export const de = (value: number, decimals = 0) =>
    value.toLocaleString("de-DE", { minimumFractionDigits: decimals, maximumFractionDigits: decimals })

export const km = (meters: number, decimals = 1) => de(meters / 1000, decimals)

// seconds per km -> "5:12"
export const formatPace = (seconds: number) => {
    const total = Math.round(seconds)
    return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`
}

// "1:02:03" or "42:10"
export const formatDuration = (seconds: number) => {
    const s = Math.round(seconds)
    const h = Math.floor(s / 3600), m = Math.floor(s % 3600 / 60), x = s % 60
    return h ? `${h}:${String(m).padStart(2, "0")}:${String(x).padStart(2, "0")}` : `${m}:${String(x).padStart(2, "0")}`
}

// "3 h 12 min" or "45 min"
export const formatHours = (seconds: number) => {
    const h = Math.floor(seconds / 3600), m = Math.round(seconds % 3600 / 60)
    return h ? `${h} h ${m} min` : `${m} min`
}

// "Mi, 30. Sep"
export const formatDate = (date: Date) => `${WEEKDAY(date)}, ${date.getDate()}. ${MONTHS_SHORT[date.getMonth()]}`

export const formatMonth = (date: Date) => `${MONTHS_SHORT[date.getMonth()]} ${date.getFullYear()}`

// green (fast) → yellow → orange → red (slow), `value` between 0 and 1
const STOPS = [[48, 209, 88], [255, 214, 10], [255, 159, 10], [255, 69, 58]]
export function paceColor(value: number) {
    const v = Math.max(0, Math.min(1, value)) * 3
    const i = Math.min(2, Math.floor(v)), f = v - i, a = STOPS[i], b = STOPS[i + 1]
    return `rgb(${a.map((x, k) => Math.round(x + (b[k] - x) * f)).join(",")})`
}
