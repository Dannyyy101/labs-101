import { Button } from "@/components/ui/button"
import {
    Card,
    CardAction,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import Link from "next/link"
import { signUp } from "./action"
import { signInWithGoogle } from "../sign-in/action"

export default function SignUp() {
    return (
        <Card className="w-full max-w-md absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
            <CardHeader>
                <CardTitle>Create a new account</CardTitle>
                <CardDescription>
                    Enter your email below to create to your account
                </CardDescription>
                <CardAction>
                    <Link className="hover:underline" href={"/sign-in"}>Sign In</Link>
                </CardAction>
            </CardHeader>
            <CardContent>
                <form action={signUp}>
                    <div className="flex flex-col gap-6">
                        <div className="flex gap-2">
                            <div className="grid gap-2">
                                <Label htmlFor="firstName">First name</Label>
                                <Input
                                    name="firstName"
                                    id="firstName"
                                    type="text"
                                    required
                                />
                            </div>
                            <div className="grid gap-2">
                                <Label htmlFor="firstName">Last name</Label>
                                <Input
                                    name="lastName"
                                    id="lastName"
                                    type="text"
                                    required
                                />
                            </div>
                        </div>
                        <div className="grid gap-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                name="email"
                                id="email"
                                type="email"
                                placeholder="m@example.com"
                                required
                            />
                        </div>
                        <div className="grid gap-2">
                            <div className="flex items-center">
                                <Label htmlFor="password">Password</Label>
                            </div>
                            <Input name="password" id="password" type="password" required />
                        </div>
                    </div>

                    <CardFooter className="flex-col gap-2 w-full mt-8">
                        <Button type="submit" className="w-full">
                            Sign Up
                        </Button>
                        <Button onClick={signInWithGoogle} variant="outline" className="w-full">
                            Sign In with Google
                        </Button>
                    </CardFooter>
                </form>
            </CardContent>
        </Card>
    )
}
