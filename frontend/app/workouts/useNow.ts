import { useEffect, useState } from "react"

/** The current time, updated every `interval` ms (for running clocks). */
export function useNow(interval = 1000) {
    const [now, setNow] = useState(() => Date.now())
    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), interval)
        return () => clearInterval(timer)
    }, [interval])
    return now
}
