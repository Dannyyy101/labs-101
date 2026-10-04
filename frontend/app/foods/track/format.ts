const WEEKDAYS_SHORT = ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]
const MONTHS_SHORT = ["Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"]

// 1959.4 -> "1.959"
export const formatNumber = (value: number) => Math.round(value).toLocaleString("de-DE")

// days are passed around as "YYYY-MM-DD"
const toDate = (day: string) => new Date(`${day}T12:00:00`)

const toDay = (date: Date) =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`

export const today = () => toDay(new Date())

export const addDays = (day: string, days: number) => {
    const date = toDate(day)
    date.setDate(date.getDate() + days)
    return toDay(date)
}

// "Mi, 30. Sep"
export const formatShortDay = (day: string) => {
    const date = toDate(day)
    return `${WEEKDAYS_SHORT[date.getDay()]}, ${date.getDate()}. ${MONTHS_SHORT[date.getMonth()]}`
}

// "Mittwoch, 30. September"
export const formatLongDay = (day: string) =>
    toDate(day).toLocaleDateString("de-DE", { weekday: "long", day: "numeric", month: "long" })

export const formatDayTitle = (day: string) => {
    if (day === today()) return "Heute"
    if (day === addDays(today(), -1)) return "Gestern"
    return toDate(day).toLocaleDateString("de-DE", { weekday: "long" })
}

// "07:30"
export const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString("de-DE", { hour: "2-digit", minute: "2-digit" })
