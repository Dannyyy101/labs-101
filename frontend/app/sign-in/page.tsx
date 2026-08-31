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
import { signIn, signInWithGoogle } from "./action"

export default function SignIn() {
    return (
        <Card className="w-full max-w-md absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
            <CardHeader>
                <CardTitle>Sing in</CardTitle>
                <CardDescription>
                    Enter your email below to sign in
                </CardDescription>
                <CardAction>
                    <Link className="hover:underline" href={"/sign-up"}>Sign Up</Link>
                </CardAction>
            </CardHeader>
            <CardContent>
                <form action={signIn}>
                    <div className="flex flex-col gap-6">
                        <div className="grid gap-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                name="email"
                                id="email"
                                type="email"
                                placeholder="m@example.com"
                            />
                        </div>
                        <div className="grid gap-2">
                            <div className="flex items-center">
                                <Label htmlFor="password">Password</Label>
                            </div>
                            <Input name="password" id="password" type="password" />
                        </div>
                    </div>

                    <CardFooter className="flex-col gap-2 w-full mt-8">
                        <Button type="submit" className="w-full">
                            Sign In
                        </Button>
                        <Button
                            type="submit"
                            formAction={signInWithGoogle}
                            variant="outline"
                            className="w-full"
                        >
                            Sign In with Google
                        </Button>
                    </CardFooter>
                </form>
            </CardContent>
        </Card>
    )
}
