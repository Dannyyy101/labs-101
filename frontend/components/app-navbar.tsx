'use client'

import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { AvatarImage } from "@/components/AvatarImage"
import { Logo } from "@/components/Logo"
import { NotificationBell } from "@/components/notifications/NotificationBell"
import { useUser } from "@/components/user-provider"
import { signOut } from "@/app/sign-in/action"
import { addDays, formatShortDay, today } from "@/app/foods/track/format"
import { ChevronLeft, ChevronRight, LogOut, Settings, Trophy, UserPen, Users } from "lucide-react"
import { useTheme } from "next-themes"
import Link from "next/link"
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { Suspense } from "react"

const NAV = [
    { href: "/", label: "Heute" },
    { href: "/foods/track", label: "Track Food" },
    { href: "/planner", label: "Calendar" },
    { href: "/workouts", label: "Workouts" },
    { href: "/runs", label: "Laufen" },
    { href: "/exercises", label: "Exercises" },
    { href: "/foods", label: "Foods" },
]

const HIDDEN_ON = ["/sign-in", "/sign-up"]

export function AppNavbar() {
    const pathname = usePathname()

    if (HIDDEN_ON.some((path) => pathname.startsWith(path)))
        return null

    // "/foods/track" must not also mark "/foods" as active
    const activeHref = NAV
        .filter((item) => pathname === item.href || pathname.startsWith(item.href + "/"))
        .sort((a, b) => b.href.length - a.href.length)[0]?.href
    // the day switcher leaves no room for the wordmark on a phone
    const showDateNav = activeHref === "/foods/track"

    return (
        <header className="sticky top-0 z-40 px-3 pt-3 md:px-6">
            {/* equally wide outer columns keep the tabs centered on the page, on small screens they get their own row */}
            <div className="glass mx-auto grid max-w-7xl grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2 rounded-[16px] px-3 py-2 md:grid-cols-[1fr_auto_1fr] md:px-4">
                <Link href="/" aria-label="Labs-101" className="justify-self-start"><Logo compact={showDateNav} /></Link>

                {/* min-w-0 lets the tabs scroll instead of widening the grid past the card */}
                <nav className="col-span-2 row-start-2 min-w-0 overflow-x-auto md:col-span-1 md:row-start-auto">
                    {/* mx-auto instead of justify-center, so scrolling still reaches the first tab */}
                    <div className="mx-auto flex w-fit gap-0.5 rounded-[12px] bg-ink/5 p-[3px] dark:bg-black/20">
                        {NAV.map((item) => {
                            const active = item.href === activeHref
                            return <Link
                                key={item.href}
                                href={item.href}
                                aria-current={active ? "page" : undefined}
                                className={`whitespace-nowrap rounded-[9px] px-3.5 py-1.5 text-label font-semibold transition-colors ${active
                                    ? "bg-surface-raised text-ink shadow-xs dark:bg-white/15"
                                    : "text-ink-muted hover:text-ink"}`}
                            >
                                {item.label}
                            </Link>
                        })}
                    </div>
                </nav>

                <div className="flex items-center justify-self-end gap-x-2 sm:gap-x-4">
                    {showDateNav && <Suspense><DateNav /></Suspense>}
                    <NotificationBell />
                    <UserMenu />
                </div>
            </div>
        </header>
    )
}

// switches the day of the track food page via "?date=YYYY-MM-DD"
function DateNav() {
    const param = useSearchParams().get("date")
    const day = param && /^\d{4}-\d{2}-\d{2}$/.test(param) ? param : today()

    return <div className="flex items-center gap-x-1">
        <Link href={`/foods/track?date=${addDays(day, -1)}`} aria-label="Vorheriger Tag" className="p-1 text-brand"><ChevronLeft className="size-5" /></Link>
        <span className="w-24 text-center font-medium tabular-nums">{formatShortDay(day)}</span>
        <Link href={`/foods/track?date=${addDays(day, 1)}`} aria-label="Nächster Tag" className="p-1 text-brand"><ChevronRight className="size-5" /></Link>
        {day !== today() && <Link href="/foods/track" className="ml-1 text-label font-semibold text-brand-strong">Heute</Link>}
    </div>
}

function UserMenu() {
    const user = useUser()
    const { theme, setTheme } = useTheme()
    const router = useRouter()

    const initial = user?.name?.trim().charAt(0).toUpperCase() ?? ""

    return <DropdownMenu>
        <DropdownMenuTrigger aria-label="Benutzermenü" className="flex size-9 items-center justify-center overflow-hidden rounded-full bg-brand font-semibold text-on-brand ring-2 ring-white/70 dark:ring-white/15">
            <AvatarImage src={user?.image ?? null} alt="" size={40} className="size-9 object-cover" fallback={initial} />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-auto min-w-48 max-w-[min(20rem,calc(100vw-2rem))]">
            {user && <>
                <DropdownMenuGroup>
                    <DropdownMenuLabel>
                        <p className="truncate font-medium text-foreground">{user.name}</p>
                        <p className="truncate text-xs font-normal" title={user.email ?? undefined}>{user.email}</p>
                    </DropdownMenuLabel>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
            </>}
            <DropdownMenuGroup>
                <DropdownMenuLabel>Design</DropdownMenuLabel>
                <DropdownMenuRadioGroup value={theme} onValueChange={setTheme}>
                    <DropdownMenuRadioItem value="light">Hell</DropdownMenuRadioItem>
                    <DropdownMenuRadioItem value="dark">Dunkel</DropdownMenuRadioItem>
                    <DropdownMenuRadioItem value="system">System</DropdownMenuRadioItem>
                </DropdownMenuRadioGroup>
            </DropdownMenuGroup>
            {user && <>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => router.push("/profile")}><Trophy />Profil & Achievements</DropdownMenuItem>
                <DropdownMenuItem onClick={() => router.push("/friends")}><Users />Freunde</DropdownMenuItem>
                <DropdownMenuItem onClick={() => window.open("/auth/profile", "_blank")}><UserPen />Konto & Profilbild</DropdownMenuItem>
                <DropdownMenuItem onClick={() => router.push("/settings")}><Settings />Einstellungen</DropdownMenuItem>
                <DropdownMenuItem onClick={() => signOut()}><LogOut />Abmelden</DropdownMenuItem>
            </>}
        </DropdownMenuContent>
    </DropdownMenu>
}
