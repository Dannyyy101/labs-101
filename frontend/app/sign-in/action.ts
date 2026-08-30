'use server'
import { auth } from "@/lib/auth";
import { redirect } from "next/navigation";

export const signIn = async (formData: FormData) => {
    const response = await auth.api.signInEmail({
        body: {
            email: formData.get("email")?.toString()!,
            password: formData.get("password")?.toString()!
        },
        asResponse: true
    });

    if (response.ok) {
        redirect("/planner")
    }

    console.error(response)
}

export const signInWithGoogle = async () => {
    const { url } = await auth.api.signInSocial({
        body: {
            provider: "google",
            callbackURL: "/planner",
        },
    });

    if (!url) {
        throw new Error("No redirect URL from provider");
    }

    redirect(url);
};


