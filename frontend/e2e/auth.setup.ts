import { expect, test as setup } from "@playwright/test"

const EMAIL = process.env.E2E_EMAIL
const PASSWORD = process.env.E2E_PASSWORD

// signs in once through the login of zitadel and stores the session cookie for all viewport projects
setup("authenticate", async ({ page, baseURL }) => {
    if (!EMAIL || !PASSWORD) throw new Error("E2E_EMAIL and E2E_PASSWORD of a zitadel user are needed")
    const backInApp = (url: URL) => url.origin === new URL(baseURL!).origin

    await page.goto("/auth/login")
    await page.locator("#loginName").fill(EMAIL)
    await page.locator("#submit-button").click()
    await page.locator("#password").fill(PASSWORD)
    await page.locator("#submit-button").click()

    // zitadel may offer to set up a second factor after the password, the test user skips it
    const skipMfa = page.getByRole("button", { name: /skip|überspringen/i })
    const wrongPassword = page.getByText(/passwort ungültig|password is invalid/i)
    await Promise.race([page.waitForURL(backInApp), skipMfa.waitFor(), wrongPassword.waitFor()])
    if (await wrongPassword.isVisible()) throw new Error(`zitadel rejected the password of ${EMAIL}`)
    if (await skipMfa.isVisible()) await skipMfa.click()

    await page.waitForURL(backInApp)
    await expect(page, "still on the sign in, are the credentials right?").not.toHaveURL(/sign-in/)

    await page.context().storageState({ path: "e2e/.auth/user.json" })
})
