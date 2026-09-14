'use client'
import {
    Sidebar,
    SidebarContent,
    SidebarFooter,
    SidebarGroup,
    SidebarGroupAction,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
} from "@/components/ui/sidebar"
import { authClient } from "@/lib/auth-client"
import { Dumbbell, Apple, User2 } from "lucide-react"
import { Spinner } from "./ui/spinner"
import Image from "next/image"
import Link from "next/link"

export function AppSidebar() {
    const {
        data: session,
        isPending, //loading state
        error, //error object
        refetch //refetch the session
    } = authClient.useSession()


    if (isPending) {
        return <Spinner className="size-6" />
    }

    if (error || !session) {
        return error?.message
    }

    return (
        <Sidebar>
            <SidebarHeader>Labs-101</SidebarHeader>
            <SidebarContent className="flex flex-col justify-center">
                <SidebarGroup>
                    <SidebarGroupLabel render={<a href={"/foods/track"} />}>Track Food</SidebarGroupLabel>
                </SidebarGroup>
                <SidebarGroup>
                    <SidebarGroupLabel render={<a href={"/planner"} />}>Calendar</SidebarGroupLabel>
                </SidebarGroup>
                <SidebarGroup>
                    <SidebarGroupLabel render={<a href={"/workouts"} />}>Workouts</SidebarGroupLabel>
                </SidebarGroup>
                <SidebarGroup>
                    <SidebarGroupLabel>Manage Data</SidebarGroupLabel>
                    <SidebarGroupContent>
                        <SidebarMenu>
                            <SidebarMenuItem>
                                <SidebarMenuButton render={<a href={"/exercises"} />}>
                                    <Dumbbell />
                                    <span>Exercises</span>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                            <SidebarMenuItem>
                                <SidebarMenuButton render={<a href={"/foods"} />}>
                                    <Apple />
                                    <span>Foods</span>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        </SidebarMenu>
                    </SidebarGroupContent>
                </SidebarGroup>
            </SidebarContent>
            <SidebarFooter>
                <SidebarMenu>
                    <SidebarMenuItem>
                        <Link href={"users/" + session.user.id}>
                            <SidebarMenuButton>
                                {session.user.image ? <Image className="rounded-full" src={session.user.image} alt="profile image" width={32} height={32} /> : <User2 />} {session.user.name}
                            </SidebarMenuButton>
                        </Link>
                    </SidebarMenuItem>
                </SidebarMenu>
            </SidebarFooter>
        </Sidebar>
    )
}