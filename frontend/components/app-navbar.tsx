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
import { NotificationBell } from "@/components/notifications/NotificationBell"
import { useUser } from "@/components/user-provider"
import { signOut } from "@/app/sign-in/action"
import { addDays, formatShortDay, today } from "@/app/foods/track/format"
import { ChevronLeft, ChevronRight, LogOut, Settings, UserPen } from "lucide-react"
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

    return (
        <header className="sticky top-0 z-40 border-b bg-background/80 backdrop-blur">
            {/* equally wide outer columns keep the tabs centered on the page, on small screens they get their own row */}
            <div className="grid grid-cols-[1fr_auto] items-center gap-x-4 gap-y-2 px-4 py-3 md:grid-cols-[1fr_auto_1fr] md:px-8">
                <Link href="/" className="justify-self-start text-lg font-semibold tracking-tight">Labs-101</Link>

                <nav className="col-span-2 row-start-2 overflow-x-auto md:col-span-1 md:row-start-auto">
                    {/* mx-auto instead of justify-center, so scrolling still reaches the first tab */}
                    <div className="mx-auto flex w-fit rounded-xl bg-muted p-1">
                        {NAV.map((item) => {
                            const active = item.href === activeHref
                            return <Link
                                key={item.href}
                                href={item.href}
                                aria-current={active ? "page" : undefined}
                                className={`whitespace-nowrap rounded-lg px-4 py-1.5 text-sm font-medium transition-colors ${active
                                    ? "bg-background text-foreground shadow-sm"
                                    : "text-muted-foreground hover:text-foreground"}`}
                            >
                                {item.label}
                            </Link>
                        })}
                    </div>
                </nav>

                <div className="flex items-center justify-self-end gap-x-4">
                    {activeHref === "/foods/track" && <Suspense><DateNav /></Suspense>}
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
        <Link href={`/foods/track?date=${addDays(day, -1)}`} aria-label="Vorheriger Tag" className="p-1 text-blue-500"><ChevronLeft className="size-5" /></Link>
        <span className="w-24 text-center font-medium tabular-nums">{formatShortDay(day)}</span>
        <Link href={`/foods/track?date=${addDays(day, 1)}`} aria-label="Nächster Tag" className="p-1 text-blue-500"><ChevronRight className="size-5" /></Link>
        {day !== today() && <Link href="/foods/track" className="ml-1 text-sm text-blue-500">Heute</Link>}
    </div>
}

function UserMenu() {
    const user = useUser()
    const { theme, setTheme } = useTheme()
    const router = useRouter()

    const initial = user?.name?.trim().charAt(0).toUpperCase() ?? ""

    return <DropdownMenu>
        <DropdownMenuTrigger aria-label="Benutzermenü" className="flex size-10 items-center justify-center overflow-hidden rounded-full bg-muted-foreground/70 font-semibold text-white">
            <AvatarImage src={user?.image ?? null} alt="" size={40} className="size-10 object-cover" fallback={initial} />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-48">
            {user && <>
                <DropdownMenuGroup>
                    <DropdownMenuLabel>
                        <p className="font-medium text-foreground">{user.name}</p>
                        <p className="text-xs font-normal">{user.email}</p>
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
                <DropdownMenuItem onClick={() => window.open("/auth/profile", "_blank")}><UserPen />Profil & Profilbild</DropdownMenuItem>
                <DropdownMenuItem onClick={() => router.push("/settings")}><Settings />Einstellungen</DropdownMenuItem>
                <DropdownMenuItem onClick={() => signOut()}><LogOut />Abmelden</DropdownMenuItem>
            </>}
        </DropdownMenuContent>
    </DropdownMenu>
}
