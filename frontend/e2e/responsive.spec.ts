import { expect, test } from "@playwright/test"
import { expectResponsive, settle } from "./layout"

// every route of the app, dynamic routes with a value that always exists
const PAGES = [
    { name: "Heute", path: "/" },
    { name: "Track Food", path: "/foods/track" },
    { name: "Calendar", path: "/planner" },
    { name: "Workouts", path: "/workouts" },
    { name: "Workout erstellen", path: "/workouts/new" },
    { name: "Laufen", path: "/runs" },
    { name: "Exercises", path: "/exercises" },
    { name: "Foods", path: "/foods" },
    { name: "Benachrichtigungen", path: "/notifications" },
    { name: "Einstellungen", path: "/settings" },
]

const PUBLIC_PAGES = [
    { name: "Sign in", path: "/sign-in" },
    { name: "Sign up", path: "/sign-up" },
]

test.describe("pages fit the viewport", () => {
    for (const { name, path } of PAGES) {
        test(name, async ({ page }, testInfo) => {
            const response = await page.goto(path)
            expect(response?.ok(), `${path} responded with ${response?.status()}`).toBeTruthy()
            await expect(page, "redirected to the sign in, is the session valid?").not.toHaveURL(/sign-in/)
            await settle(page)
            await expectResponsive(page, testInfo)
        })
    }
})

test.describe("public pages fit the viewport", () => {
    test.use({ storageState: { cookies: [], origins: [] } })

    for (const { name, path } of PUBLIC_PAGES) {
        test(name, async ({ page }, testInfo) => {
            await page.goto(path)
            await settle(page)
            await expectResponsive(page, testInfo)
        })
    }
})

test.describe("navigation", () => {
    test("navbar does not take over the screen", async ({ page }) => {
        await page.goto("/foods/track")
        await settle(page)
        const header = await page.locator("header").first().boundingBox()
        const viewport = page.viewportSize()!
        expect(header!.height, "header height").toBeLessThanOrEqual(viewport.height * 0.25)
    })

    test("every tab is reachable", async ({ page }) => {
        await page.goto("/")
        await settle(page)
        const links = page.locator("header nav a")
        for (let i = 0; i < await links.count(); i++) {
            const link = links.nth(i)
            await link.scrollIntoViewIfNeeded()
            await expect(link).toBeInViewport({ ratio: 0.99 })
        }
    })

    test("user menu opens inside the viewport", async ({ page }, testInfo) => {
        await page.goto("/")
        await settle(page)
        await page.getByRole("button", { name: "Benutzermenü" }).click()
        await expect(page.getByRole("menu")).toBeVisible()
        await expect(page.getByRole("menu")).toBeInViewport({ ratio: 0.99 })
        await expectResponsive(page, testInfo)
    })
})
