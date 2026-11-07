import { defineConfig } from "@playwright/test"
import { existsSync } from "node:fs"

// E2E_EMAIL and E2E_PASSWORD of the zitadel test user, git ignored
if (existsSync(".env.e2e")) process.loadEnvFile(".env.e2e")

const BASE_URL = process.env.E2E_BASE_URL ?? "http://localhost:3000"

// a phone and a desktop, the food dialogs and the drawer look different on both
const VIEWPORTS = [
    { name: "mobile", viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 3 },
    { name: "desktop", viewport: { width: 1280, height: 800 }, isMobile: false, hasTouch: false, deviceScaleFactor: 1 },
]

export default defineConfig({
    testDir: "./e2e",
    outputDir: "./e2e/.results",
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    // the dev server compiles every route on first request, too many workers just time out
    workers: process.env.CI ? 2 : 4,
    timeout: 60_000,
    reporter: [["list"], ["html", { outputFolder: "./e2e/.report", open: "never" }]],
    use: {
        baseURL: BASE_URL,
        browserName: "chromium",
        locale: "de-DE",
        timezoneId: "Europe/Berlin",
        screenshot: "only-on-failure",
        trace: "retain-on-failure",
    },
    projects: [
        { name: "setup", testMatch: /auth\.setup\.ts/ },
        ...VIEWPORTS.map(({ name, ...device }) => ({
            name,
            testMatch: /\.spec\.ts/,
            dependencies: ["setup"],
            use: { ...device, storageState: "e2e/.auth/user.json" },
        })),
    ],
    webServer: {
        command: "npm run dev",
        url: BASE_URL,
        reuseExistingServer: true,
        timeout: 120_000,
    },
})
