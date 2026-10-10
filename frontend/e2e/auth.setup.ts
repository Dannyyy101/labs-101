import { expect, test as setup } from "@playwright/test"

const EMAIL = process.env.E2E_EMAIL ?? "e2e-responsive@labs-101.test"
const PASSWORD = process.env.E2E_PASSWORD ?? "e2e-responsive-password"

// logs in once and stores the session cookie for all viewport projects
setup("authenticate", async ({ request, baseURL }) => {
    // better-auth rejects requests without a trusted origin
    const headers = { Origin: baseURL! }

    let response = await request.post("/api/auth/sign-in/email", { headers, data: { email: EMAIL, password: PASSWORD } })
    if (!response.ok() && !process.env.E2E_EMAIL) {
        // first run against this database, the test user does not exist yet
        response = await request.post("/api/auth/sign-up/email", { headers, data: { name: "E2E Responsive", email: EMAIL, password: PASSWORD } })
    }
    expect(response.ok(), `login failed: ${response.status()} ${await response.text()}`).toBeTruthy()

    await request.storageState({ path: "e2e/.auth/user.json" })
})
