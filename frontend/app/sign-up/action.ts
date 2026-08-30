'use server'
import { auth } from "@/lib/auth";
import { isEmailAllowed } from "@/utils/auth";
import { redirect } from "next/navigation";

export const signUp = async (formData: FormData) => {
    if (isEmailAllowed(formData.get("email")?.toString()!)) {
        console.error("Email is not allowed")
        return
    }
    const name = formData.get("firstName")?.toString()! + formData.get("lastName")?.toString()!
    const response = await auth.api.signUpEmail({
        body: {
            name: name,
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
