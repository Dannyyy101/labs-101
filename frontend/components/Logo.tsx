import { cn } from "@/lib/utils"

/** The app icon, same artwork as app/icon.svg */
export function LogoMark({ className }: { className?: string }) {
    return <svg viewBox="0 0 1024 1024" aria-hidden="true" className={cn("size-7 shrink-0", className)}>
        <rect width="1024" height="1024" rx="230" fill="#0A84FF" />
        <g transform="translate(42 0) translate(0 480) skewX(-12) translate(0 -480)">
            <line x1="220" y1="350" x2="220" y2="610" stroke="#FFFFFF" strokeWidth="120" strokeLinecap="round" />
            <ellipse cx="470" cy="480" rx="100" ry="135" fill="none" stroke="#FFFFFF" strokeWidth="110" />
            <line x1="720" y1="350" x2="720" y2="610" stroke="#FFFFFF" strokeWidth="120" strokeLinecap="round" />
        </g>
        <rect x="160" y="756" width="110" height="44" rx="22" fill="#A9D1FF" />
        <rect x="300" y="756" width="220" height="44" rx="22" fill="#FF9F0A" />
    </svg>
}

/** Icon plus the "Labs-1o1" wordmark, the 0 is drawn as a blue ring */
export function Logo({ className }: { className?: string }) {
    return <span className={cn("inline-flex items-center gap-2", className)}>
        <LogoMark />
        <span className="text-lg font-bold italic tracking-tight">
            <span className="sr-only">Labs-101</span>
            Labs-1<span className="text-[#0A84FF] ml-[1px]">o</span>1
        </span>
    </span>
}
