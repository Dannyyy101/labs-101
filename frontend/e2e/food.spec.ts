import { expect, test, type Locator, type Page } from "@playwright/test"

// foods are shared by all users and can not be deleted, so the chain creates a single food per project
// and run and the later tests track exactly that one. The values give round numbers for 200 g:
// 330 kcal, 20 g protein, 40 g carbs, 10 g fat
const FOOD = { protein: 10, carbohydrates: 20, fat: 5, kcal: 165, portion: { label: "Scheibe", grams: 40 } }

test.describe.configure({ mode: "serial" })

let name: string

test.beforeAll(({}, testInfo) => {
    name = `E2E Testessen ${testInfo.project.name} ${Date.now()}`
})

const drawer = (page: Page) => page.getByRole("dialog")

/** The number input of a nutrient, the field labels are not linked to their inputs. */
const nutrient = (drawer: Locator, label: string) =>
    drawer.getByRole("group").filter({ has: drawer.page().getByText(label, { exact: true }) }).getByRole("spinbutton")

async function searchFoods(page: Page, query: string) {
    await page.goto(`/foods?query=${encodeURIComponent(query)}`)
    await expect(page, "redirected to the sign in, is the session valid?").not.toHaveURL(/sign-in/)
}

const foodRow = (page: Page) => page.getByRole("row").filter({ hasText: name })

const mealSection = (page: Page, meal: string) =>
    page.locator("section").filter({ has: page.getByRole("heading", { name: meal, exact: true }) })

const entry = (page: Page) => mealSection(page, "Lunch").getByRole("listitem").filter({ hasText: name })

test.describe("foods", () => {
    test("create a food", async ({ page }) => {
        await searchFoods(page, "")
        await page.getByRole("button", { name: "Add food" }).click()
        const form = drawer(page)
        await expect(form.getByRole("heading", { name: "New food" })).toBeVisible()

        const submit = form.getByRole("button", { name: "Create food" })
        await expect(form.getByText("Every food needs a name.")).toBeVisible()
        await expect(submit).toBeDisabled()

        await form.getByLabel("Name").fill(name)
        await expect(form.getByText("Every food needs a name.")).toBeHidden()

        await test.step("energy from the macros", async () => {
            await nutrient(form, "Protein").fill(String(FOOD.protein))
            await nutrient(form, "Carbohydrates").fill(String(FOOD.carbohydrates))
            await nutrient(form, "Fat").fill(String(FOOD.fat))
            await nutrient(form, "Energy").fill("300")
            await expect(form.getByText(`Macros add up to ${FOOD.kcal} kcal`)).toBeVisible()

            await form.getByRole("button", { name: `Use ${FOOD.kcal} kcal from macros` }).click()
            await expect(nutrient(form, "Energy")).toHaveValue(String(FOOD.kcal))
            await expect(form.getByText(/Macros add up to/)).toBeHidden()
        })

        await test.step("a portion needs grams", async () => {
            await form.getByRole("button", { name: "Add portion" }).click()
            await expect(form.getByText("Portions need a weight in grams.")).toBeVisible()
            await expect(submit).toBeDisabled()

            const portion = form.getByRole("listitem")
            await portion.getByPlaceholder("Slice").fill(FOOD.portion.label)
            await portion.getByRole("spinbutton").fill(String(FOOD.portion.grams))
            // the first portion is the default one
            await expect(portion.getByRole("button", { name: "Set as default portion" })).toHaveAttribute("aria-pressed", "true")
            await expect(form.getByText("Portions need a weight in grams.")).toBeHidden()
        })

        await submit.click()
        await expect(form).toBeHidden()

        await searchFoods(page, name)
        await expect(foodRow(page)).toHaveCount(1)
        await expect(foodRow(page).getByRole("cell")).toHaveText([name, `${FOOD.protein} g`, String(FOOD.kcal)])
    })

    test("search finds the food", async ({ page }) => {
        await searchFoods(page, "")
        const search = page.getByPlaceholder("Lebensmittel suchen…")
        await search.fill(name)
        await search.press("Enter")

        await expect(page).toHaveURL(/query=/)
        await expect(foodRow(page)).toHaveCount(1)
    })

    test("edit the food", async ({ page }) => {
        await searchFoods(page, name)
        await foodRow(page).click()
        const form = drawer(page)
        await expect(form.getByRole("heading", { name: "Edit food" })).toBeVisible()
        await expect(form.getByLabel("Name")).toHaveValue(name)
        await expect(nutrient(form, "Protein")).toHaveValue(String(FOOD.protein))

        await test.step("cancel keeps the old values", async () => {
            await nutrient(form, "Protein").fill("99")
            await form.getByRole("button", { name: "Cancel" }).click()
            await expect(form).toBeHidden()
            await expect(foodRow(page).getByRole("cell").nth(1)).toHaveText(`${FOOD.protein} g`)
        })

        await foodRow(page).click()
        await nutrient(form, "Protein").fill("12,5")
        await form.getByRole("button", { name: "Save changes" }).click()
        await expect(form).toBeHidden()
        await expect(foodRow(page).getByRole("cell").nth(1)).toHaveText("12,5 g")

        // back to the values the tracking tests count with
        await foodRow(page).click()
        await nutrient(form, "Protein").fill(String(FOOD.protein))
        await form.getByRole("button", { name: "Save changes" }).click()
        await expect(form).toBeHidden()
        await expect(foodRow(page).getByRole("cell").nth(1)).toHaveText(`${FOOD.protein} g`)
    })
})

test.describe("track food", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/foods/track")
        await expect(page, "redirected to the sign in, is the session valid?").not.toHaveURL(/sign-in/)
    })

    test("add the food to a meal", async ({ page }) => {
        await mealSection(page, "Lunch").getByRole("button", { name: "Hinzufügen" }).click()
        const dialog = page.getByRole("dialog")
        await expect(dialog.getByRole("heading", { name: "Essen hinzufügen" })).toBeVisible()

        await dialog.getByPlaceholder("Suchen...").fill(name)
        await dialog.getByRole("button", { name }).click()

        await expect(dialog.getByRole("heading", { name })).toBeVisible()
        await expect(dialog.getByText(`pro 100 g · ${FOOD.kcal} kcal · P ${FOOD.protein} · K ${FOOD.carbohydrates} · F ${FOOD.fat}`)).toBeVisible()

        const amount = dialog.getByLabel("Menge")
        await amount.fill("190")
        await dialog.getByRole("button", { name: "Mehr" }).click()
        await expect(amount).toHaveValue("200")
        await expect(dialog.getByText("330 kcal")).toBeVisible()

        await dialog.getByRole("button", { name: "Zu Lunch hinzufügen" }).click()
        await expect(dialog).toBeHidden()

        await expect(entry(page)).toContainText("200 g · P 20 · K 40 · F 10")
        await expect(entry(page)).toContainText("330")
    })

    test("switch the entry to a portion", async ({ page }) => {
        await entry(page).getByRole("button", { name }).click()
        const dialog = page.getByRole("dialog")
        await expect(dialog.getByRole("heading", { name: "Eintrag bearbeiten" })).toBeVisible()
        await expect(dialog.getByLabel("Menge")).toHaveValue("200")

        // a portion starts with one piece
        await dialog.getByRole("button", { name: new RegExp(`^${FOOD.portion.label}`) }).click()
        await expect(dialog.getByLabel("Menge")).toHaveValue("1")
        await dialog.getByRole("button", { name: "Mehr" }).click()
        await expect(dialog.getByText("= 80 g")).toBeVisible()

        await dialog.getByRole("button", { name: "Speichern" }).click()
        await expect(dialog).toBeHidden()
        await expect(entry(page)).toContainText(`2 ${FOOD.portion.label} · 80 g`)
        await expect(entry(page)).toContainText("132")
    })

    test("remove the entry", async ({ page }) => {
        await entry(page).getByRole("button", { name: `${name} entfernen` }).click()
        await expect(entry(page)).toHaveCount(0)

        await page.reload()
        await expect(entry(page)).toHaveCount(0)
    })
})
