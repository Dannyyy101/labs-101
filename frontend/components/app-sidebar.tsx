'use client'

import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarSeparator,
} from "@/components/ui/sidebar"
import { Skeleton } from "@/components/ui/skeleton"
import { authClient } from "@/lib/auth-client"
import {
    Apple,
    CalendarDays,
    Dumbbell,
    ListChecks,
    User2,
    Utensils,
} from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { ModeToggle } from "./ModeToggle"

const mainNav = [
    { href: "/foods/track", label: "Track Food", icon: Utensils },
    { href: "/planner", label: "Calendar", icon: CalendarDays },
    { href: "/workouts", label: "Workouts", icon: ListChecks },
]

const dataNav = [
    { href: "/exercises", label: "Exercises", icon: Dumbbell },
    { href: "/foods", label: "Foods", icon: Apple },
]

export function AppSidebar() {
    const { data: session, isPending, error } = authClient.useSession()
    const pathname = usePathname()

    const isActive = (href: string) =>
        pathname === href || pathname.startsWith(href + "/")

    return (
        <Sidebar>
            <SidebarHeader className="p-4">
                <Link href="/" className="flex items-center gap-2">
                    <div className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                        <Dumbbell className="size-4" />
                    </div>
                    <span className="text-base font-semibold tracking-tight">
                        Labs-101
                    </span>
                </Link>
            </SidebarHeader>

            <SidebarSeparator />

            <SidebarContent>
                <SidebarGroup>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            {mainNav.map((item) => (
                                <SidebarMenuItem key={item.href}>
                                    <SidebarMenuButton
                                        isActive={isActive(item.href)}
                                        tooltip={item.label}
                                        render={<Link href={item.href} />}
                                    >
                                        <item.icon />
                                        <span>{item.label}</span>
                                    </SidebarMenuButton>
                                </SidebarMenuItem>
                            ))}
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>

                <SidebarGroup>
                    <SidebarGroupLabel>Manage Data</SidebarGroupLabel>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            {dataNav.map((item) => (
                                <SidebarMenuItem key={item.href}>
                                    <SidebarMenuButton
                                        isActive={isActive(item.href)}
                                        tooltip={item.label}
                                        render={<Link href={item.href} />}
                                    >
                                        <item.icon />
                                        <span>{item.label}</span>
                                    </SidebarMenuButton>
                                </SidebarMenuItem>
                            ))}
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>

            <SidebarFooter className="gap-2">
                <SidebarSeparator />
                <div className="flex items-center gap-2">
                    <SidebarMenu className="min-w-0 flex-1">
                        <SidebarMenuItem>
                            {isPending ? (
                                <div className="flex items-center gap-2 p-2">
                                    <Skeleton className="size-8 rounded-full" />
                                    <Skeleton className="h-4 w-24" />
                                </div>
                            ) : error || !session ? (
                                <SidebarMenuButton render={<Link href="/login" />}>
                                    <User2 />
                                    <span>Anmelden</span>
                                </SidebarMenuButton>
                            ) : (
                                <SidebarMenuButton
                                    size="lg"
                                    isActive={isActive(`/users/${session.user.id}`)}
                                    render={<Link href={`/users/${session.user.id}`} />}
                                >
                                    {session.user.image ? (
                                        <Image
                                            className="size-8 shrink-0 rounded-full object-cover"
                                            src={session.user.image}
                                            alt=""
                                            width={32}
                                            height={32}
                                        />
                                    ) : (
                                        <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted">
                                            <User2 className="size-4" />
                                        </span>
                                    )}
                                    <span className="truncate font-medium">
                                        {session.user.name}
                                    </span>
                                </SidebarMenuButton>
                            )}
                        </SidebarMenuItem>
                    </SidebarMenu>
                    <ModeToggle />
                </div>
            </SidebarFooter>
        </Sidebar>
    )
}