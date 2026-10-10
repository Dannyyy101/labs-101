import { expect, type Page, type TestInfo } from "@playwright/test"

export interface LayoutIssue {
    element: string
    detail: string
}

export interface LayoutReport {
    viewportWidth: number
    scrollWidth: number
    /** elements that stick out of the viewport and are not inside a scroll/clip container */
    overflowing: LayoutIssue[]
    /** interactive elements smaller than the minimum touch target */
    smallTargets: LayoutIssue[]
    /** short button/link labels that break onto multiple lines */
    wrappedLabels: LayoutIssue[]
}

/** Waits until the route is rendered and the layout settled (fonts, charts, client components). */
export async function settle(page: Page) {
    await page.waitForLoadState("networkidle")
    await page.evaluate(() => document.fonts.ready)
    // the dev server badge floats over the bottom right corner and would swallow clicks
    await page.addStyleTag({ content: "nextjs-portal { display: none !important; }" })
    // responsive charts measure their container in an animation frame
    await page.evaluate(() => new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve))))
}

/** Measures the current page in the browser, see {@link LayoutReport}. */
export function auditLayout(page: Page, { minTarget = 24 } = {}): Promise<LayoutReport> {
    return page.evaluate(({ minTarget }) => {
        const vw = document.documentElement.clientWidth
        const TOLERANCE = 1

        const describe = (el: Element) => {
            const id = el.id ? `#${el.id}` : ""
            const cls = typeof el.className === "string" && el.className.trim()
                ? "." + el.className.trim().split(/\s+/).slice(0, 4).join(".")
                : ""
            const label = el.getAttribute("aria-label") ?? (el.textContent ?? "").trim().replace(/\s+/g, " ").slice(0, 40)
            return `<${el.tagName.toLowerCase()}${id}${cls}>${label ? ` "${label}"` : ""}`
        }

        const visible = (el: Element) =>
            el.checkVisibility({ opacityProperty: true, visibilityProperty: true, contentVisibilityAuto: true })

        // an ancestor that scrolls or clips horizontally contains the overflow on purpose (tables, tab bars, ...)
        const containedHorizontally = (el: Element) => {
            if (getComputedStyle(el).position === "fixed") return false
            for (let p = el.parentElement; p && p !== document.body && p !== document.documentElement; p = p.parentElement) {
                if (getComputedStyle(p).overflowX !== "visible") return true
                if (getComputedStyle(p).position === "fixed") return false
            }
            return false
        }

        const offenders = new Set<Element>()
        for (const el of document.body.querySelectorAll("*")) {
            if (!visible(el)) continue
            const rect = el.getBoundingClientRect()
            // screen reader only content is clipped to 1px
            if (rect.width <= 1 || rect.height <= 1) continue
            if (rect.right <= vw + TOLERANCE && rect.left >= -TOLERANCE) continue
            if (containedHorizontally(el)) continue
            offenders.add(el)
        }
        // only report the outermost element, its children overflow because of it
        const overflowing = [...offenders]
            .filter((el) => !el.parentElement || !offenders.has(el.parentElement))
            .map((el) => {
                const r = el.getBoundingClientRect()
                return { element: describe(el), detail: `left ${Math.round(r.left)}px, right ${Math.round(r.right)}px, viewport ${vw}px` }
            })

        const INTERACTIVE = "a[href], button, input:not([type=hidden]), select, textarea, [role=button], [role=tab], [role=checkbox], [role=switch], [role=combobox], [role=menuitem]"
        const smallTargets: { element: string, detail: string }[] = []
        const wrappedLabels: { element: string, detail: string }[] = []
        for (const el of document.body.querySelectorAll(INTERACTIVE)) {
            if (!visible(el) || (el as HTMLButtonElement).disabled) continue
            // links inside running text are exempt (WCAG 2.5.8)
            if (el.tagName === "A" && getComputedStyle(el).display === "inline") continue
            // tapping the wrapping label focuses the input as well
            const r = (el.matches("input, select, textarea") && el.closest("label") || el).getBoundingClientRect()
            if (r.width <= 1 || r.height <= 1) continue
            // dense targets like chart bars opt out explicitly, their neighbours are the same action
            if ((r.width < minTarget || r.height < minTarget) && !el.closest("[data-dense-targets]"))
                smallTargets.push({ element: describe(el), detail: `${Math.round(r.width)}x${Math.round(r.height)}px` })

            // a short label breaking onto a second line means its container got too narrow
            const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT)
            for (let node = walker.nextNode(); node; node = walker.nextNode()) {
                const text = node.textContent!.trim()
                if (!text || text.length > 25 || !text.includes(" ") && !text.includes("-")) continue
                const range = document.createRange()
                range.selectNodeContents(node)
                const lines = new Set([...range.getClientRects()].filter((rect) => rect.width > 0).map((rect) => Math.round(rect.top / 4)))
                if (lines.size > 1) wrappedLabels.push({ element: describe(el), detail: `"${text}" wraps onto ${lines.size} lines` })
            }
        }

        return { viewportWidth: vw, scrollWidth: document.documentElement.scrollWidth, overflowing, smallTargets, wrappedLabels }
    }, { minTarget })
}

const format = (issues: LayoutIssue[]) => issues.map((i) => `  ${i.element}: ${i.detail}`).join("\n")

/** Fails (softly, so every problem of a page is reported at once) when the page is not usable at this viewport. */
export async function expectResponsive(page: Page, testInfo: TestInfo) {
    const touch = !!testInfo.project.use.hasTouch
    const report = await auditLayout(page)

    await testInfo.attach("screenshot", { body: await page.screenshot({ fullPage: true }), contentType: "image/png" })

    expect.soft(report.scrollWidth, "page scrolls horizontally").toBeLessThanOrEqual(report.viewportWidth)
    expect.soft(report.overflowing, `elements stick out of the viewport:\n${format(report.overflowing)}`).toEqual([])
    expect.soft(report.wrappedLabels, `labels wrap:\n${format(report.wrappedLabels)}`).toEqual([])
    if (touch)
        expect.soft(report.smallTargets, `touch targets smaller than 24px:\n${format(report.smallTargets)}`).toEqual([])
}
