export function toTimeInputValue(date: Date) {
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
}

export function addTimeToDate(time: string, date: Date) {
    const [hours, minutes, seconds] = time.split(":").map((e) => parseInt(e))
    date.setHours(hours)
    date.setMinutes(minutes)
    date.setSeconds(seconds)
}