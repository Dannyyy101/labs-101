import { defineConfig } from "@playwright/test"

const BASE_URL = process.env.E2E_BASE_URL ?? "http://localhost:3000"

// the viewports every page has to work on, the narrowest one is a small android phone
const VIEWPORTS = [
    { name: "mobile-small", viewport: { width: 360, height: 740 }, isMobile: true, hasTouch: true, deviceScaleFactor: 3 },
    { name: "mobile", viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 3 },
    { name: "tablet", viewport: { width: 768, height: 1024 }, isMobile: true, hasTouch: true, deviceScaleFactor: 2 },
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
