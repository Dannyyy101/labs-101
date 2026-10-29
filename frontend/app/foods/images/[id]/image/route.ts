import { api } from "@/lib/api/client"

// the backend needs the access token, an <img> can't send it, so the photo is passed through here
export async function GET(_request: Request, ctx: RouteContext<"/foods/images/[id]/image">) {
    const { id } = await ctx.params
    if (!/^\d+$/.test(id)) return new Response(null, { status: 404 })

    const { response } = await api.GET("/api/users/me/food-images/{id}/image", { params: { path: { id: Number(id) } }, parseAs: "stream" })
    if (!response.ok) return new Response(null, { status: response.status })

    return new Response(response.body, {
        headers: {
            "Content-Type": response.headers.get("Content-Type") ?? "image/jpeg",
            // a photo never changes, but it is private
            "Cache-Control": "private, max-age=2592000, immutable",
        },
    })
}
