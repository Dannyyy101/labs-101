import { buttonVariants } from "@/components/ui/button"
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from "@/components/ui/card"

const ERRORS: Record<string, string> = {
    expired: "Die Anmeldung hat zu lange gedauert, bitte versuche es noch einmal.",
    failed: "Die Anmeldung ist fehlgeschlagen, bitte versuche es noch einmal.",
}

export default async function SignIn({ searchParams }: PageProps<"/sign-in">) {
    const { error, returnTo } = await searchParams
    const loginHref = `/auth/login${typeof returnTo === "string" ? `?returnTo=${encodeURIComponent(returnTo)}` : ""}`

    return (
        <Card className="w-full max-w-md absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
            <CardHeader>
                <CardTitle>Anmelden</CardTitle>
                <CardDescription>
                    Mit E-Mail und Passwort, Passkey oder Google bei labs-101
                </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
                {typeof error === "string" && <p className="text-sm text-destructive">{ERRORS[error] ?? ERRORS.failed}</p>}
                {/* a plain link, the route handler redirects to zitadel */}
                <a className={buttonVariants({ className: "w-full" })} href={loginHref}>
                    Anmelden
                </a>
            </CardContent>
        </Card>
    )
}
