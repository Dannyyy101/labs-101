import { expect, test, type Locator, type Page } from "@playwright/test"
import { expectResponsive, settle } from "./layout"

async function open(page: Page, path: string) {
    await page.goto(path)
    await settle(page)
}

/** The popup has to be fully visible, otherwise parts of it can not be reached on a phone. */
async function expectInViewport(popup: Locator) {
    await expect(popup).toBeVisible()
    // let the open animation finish before measuring, spinners run forever and cancelled animations reject
    await popup.evaluate((el) => Promise.all(el.getAnimations({ subtree: true })
        .filter((a) => a.effect?.getComputedTiming().iterations !== Infinity)
        .map((a) => a.finished.catch(() => undefined))))
    await expect(popup).toBeInViewport({ ratio: 0.99 })
    const box = (await popup.boundingBox())!
    const viewport = popup.page().viewportSize()!
    expect(box.x, "popup starts left of the viewport").toBeGreaterThanOrEqual(-1)
    expect(box.x + box.width, "popup ends right of the viewport").toBeLessThanOrEqual(viewport.width + 1)
}

async function addFirstExercise(page: Page) {
    await page.getByRole("button", { name: "Übung hinzufügen" }).click()
    const dialog = page.getByRole("dialog")
    await dialog.locator("button:has(.truncate)").first().click()
    await page.keyboard.press("Escape")
    await expect(dialog).toBeHidden()
    await settle(page)
}

test.describe("dialogs fit the viewport", () => {
    test("create exercise", async ({ page }, testInfo) => {
        await open(page, "/exercises")
        await page.getByRole("button", { name: "Create exercise" }).click()
        await expectInViewport(page.getByRole("dialog"))
        await expectResponsive(page, testInfo)
    })

    test("create food", async ({ page }, testInfo) => {
        await open(page, "/foods")
        await page.getByRole("button", { name: "Add food" }).click()
        await expectInViewport(page.getByRole("dialog"))
        await expectResponsive(page, testInfo)
    })

    test("add food to a meal", async ({ page }, testInfo) => {
        await open(page, "/foods/track")
        await page.getByRole("button", { name: "Essen hinzufügen" }).first().click()
        const dialog = page.getByRole("dialog")
        await expectInViewport(dialog)
        await expectResponsive(page, testInfo)

        // the detail view of a food replaces the search inside the same dialog
        await dialog.getByPlaceholder("Suchen...").fill("Hafer")
        const result = dialog.getByRole("button", { name: /Hafer/ }).first()
        await result.click()
        await expect(result).toBeHidden()
        await settle(page)
        await expectInViewport(dialog)
        await expectResponsive(page, testInfo)
    })

    test("create calendar event", async ({ page }, testInfo) => {
        await open(page, "/planner")
        // every empty hour slot opens the event dialog
        await page.locator("main button.h-20").first().click()
        await expectInViewport(page.getByRole("dialog"))
        await expectResponsive(page, testInfo)
    })

    test("pick an exercise for a workout", async ({ page }, testInfo) => {
        await open(page, "/workouts/new")
        await page.getByRole("button", { name: "Übung hinzufügen" }).click()
        await expectInViewport(page.getByRole("dialog"))
        await expectResponsive(page, testInfo)
        await page.keyboard.press("Escape")

        // the editor with an exercise and its set rows, it is never saved
        await addFirstExercise(page)
        await expectResponsive(page, testInfo)
    })

    test("notifications", async ({ page }, testInfo) => {
        await open(page, "/")
        await page.getByRole("button", { name: /Benachrichtigungen/ }).click()
        await expectInViewport(page.getByRole("dialog"))
        await expectResponsive(page, testInfo)
    })
})

// all projects share one test user and a user can only have one active session,
// so a single project walks through the phone and tablet widths
test.describe("workout session", () => {
    test("session view and finish dialog", async ({ page }, testInfo) => {
        test.skip(testInfo.project.name !== "mobile-small", "runs once for all widths")

        // a previous aborted run may have left its session running, otherwise this redirects to the dashboard
        await open(page, "/workouts/session")
        if (!page.url().endsWith("/workouts/session")) {
            await page.getByRole("button", { name: "Freies Training" }).click()
            await page.waitForURL("**/workouts/session")
            await settle(page)
        }

        try {
            // the set rows with their inputs are the widest part of the session, no set is ticked so it stays discardable
            if (await page.getByLabel("Übung entfernen").count() === 0) await addFirstExercise(page)

            for (const width of [360, 390, 768]) {
                await test.step(`${width}px`, async () => {
                    await page.setViewportSize({ width, height: 800 })
                    await settle(page)
                    await expectResponsive(page, testInfo)

                    await page.getByRole("button", { name: "Beenden", exact: true }).click()
                    const dialog = page.getByRole("dialog")
                    await expectInViewport(dialog)
                    await expectResponsive(page, testInfo)
                    await dialog.getByRole("button", { name: "Weiter trainieren" }).click()
                    await expect(dialog).toBeHidden()
                })
            }
        } finally {
            // leave no active session behind for the next run
            await page.getByRole("button", { name: "Beenden", exact: true }).click()
            await page.getByRole("dialog").getByRole("button", { name: "Training verwerfen" }).click()
            await page.waitForURL("**/workouts")
        }
    })
})
