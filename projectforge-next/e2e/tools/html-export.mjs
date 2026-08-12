/**
 * Exports a rendered page of the running Next app as a self-contained HTML file (styles inlined),
 * for handing to a design tool.
 *
 *   PF_USER=… PF_PASS=… node e2e/tools/html-export.mjs /order/62589779 /tmp/pf-export
 *
 * The order page is opened with one position expanded and one payment schedule row added, so the
 * export shows every kind of element the page has rather than only the collapsed ones.
 */
import { chromium } from "playwright";
import { mkdir, writeFile, readFile } from "node:fs/promises";
import { homedir } from "node:os";
import path from "node:path";

const appPath = process.argv[2] ?? "/order/62589779";
const outDir = path.resolve(process.argv[3] ?? "html-export");
const BASE = "http://localhost:3000/next";

// PF_USER/PF_PASS override the local test account file (whose user may require a 2nd factor).
const creds = (
  await readFile(
    path.join(homedir(), "ProjectForge/localTestAccount.txt"),
    "utf8"
  )
)
  .split(/\r?\n/)
  .map((l) => l.trim())
  .filter(Boolean);
const username = process.env.PF_USER ?? creds[0];
const password = process.env.PF_PASS ?? creds[1];

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1600, height: 1200 } });

await page.goto(
  `${BASE}/login?returnUrl=${encodeURIComponent("/next" + appPath)}`,
  {
    waitUntil: "domcontentloaded",
  }
);
await page.waitForFunction(() =>
  Object.keys(document.querySelector("form") ?? {}).some((k) =>
    k.startsWith("__reactProps$")
  )
);
await page.fill("#username", username);
await page.fill("#password", password);
await page.click('button[type="submit"]');
for (let i = 0; i < 40 && page.url().includes("/login"); i++)
  await page.waitForTimeout(500);
if (page.url().includes("/login")) {
  console.error("still on login:", page.url());
  console.error(await page.locator("body").innerText());
  process.exit(1);
}

// Not "networkidle": the dev server keeps an HMR connection open, so the page is never idle.
await page.goto(`${BASE}${appPath}`, { waitUntil: "domcontentloaded" });
await page.waitForLoadState("load");
// The route is fetched client-side; wait for the loading spinner to give way to real content.
await page
  .locator("main, form, table")
  .first()
  .waitFor({ state: "visible", timeout: 90_000 })
  .catch(() => console.error("warning: no main/form/table appeared"));
await page.waitForTimeout(5000);

// Expand the first position: collapsed rows hide most of the form's field types.
const firstPosition = page.locator('[data-slot="collapsible-trigger"]').first();
if (await firstPosition.count()) {
  await firstPosition.click();
  await page.waitForTimeout(1000);
}

// Add a payment schedule row, so its fields are in the export instead of the empty-state text.
const addSchedule = page
  .getByRole("button", { name: /zahlplanposition|payment schedule/i })
  .first();
if (await addSchedule.count()) {
  await addSchedule.click();
  await page.waitForTimeout(1500);
} else {
  console.error("warning: no add-payment-schedule button found");
}

// Inline every stylesheet the page loaded, so the export renders without the dev server.
const html = await page.evaluate(async () => {
  const texts = [];
  for (const sheet of Array.from(document.styleSheets)) {
    try {
      texts.push(
        Array.from(sheet.cssRules)
          .map((r) => r.cssText)
          .join("\n")
      );
    } catch {
      if (sheet.href) {
        try {
          texts.push(await (await fetch(sheet.href)).text());
        } catch {
          /* cross-origin, skip */
        }
      }
    }
  }
  const doc = document.documentElement.cloneNode(true);
  // Drop the link/script tags: the CSS is inlined below and the JS would only fail offline.
  doc
    .querySelectorAll('link[rel="stylesheet"], script')
    .forEach((n) => n.remove());
  // The dev overlay ("N 2 Issues") belongs to the dev server, not to the design.
  doc
    .querySelectorAll("nextjs-portal, #__next-build-watcher")
    .forEach((n) => n.remove());
  const style = document.createElement("style");
  style.textContent = texts.join("\n");
  doc.querySelector("head").appendChild(style);
  return "<!DOCTYPE html>\n" + doc.outerHTML;
});

await mkdir(outDir, { recursive: true });
const name = appPath.replace(/^\//, "").replace(/\//g, "-");
await writeFile(path.join(outDir, `${name}.html`), html, "utf8");
// Back to the top: the clicks scrolled the page, and a full-page shot of a scrolled page is mostly
// blank. The page scrolls in an inner container, so every scrollable element is reset, not just the
// window.
await page.evaluate(() => {
  window.scrollTo(0, 0);
  for (const el of Array.from(document.querySelectorAll("*"))) {
    if (el.scrollTop) el.scrollTop = 0;
  }
});
await page.waitForTimeout(300);
// The page scrolls inside a container, which `fullPage` does not grow — so grow the viewport to the
// tallest scroll height instead, otherwise the shot ends where the first screen does.
const height = await page.evaluate(() =>
  Math.max(
    document.documentElement.scrollHeight,
    ...Array.from(document.querySelectorAll("*")).map((el) =>
      el.scrollHeight > el.clientHeight ? el.scrollHeight : 0
    )
  )
);
await page.setViewportSize({
  width: 1600,
  height: Math.min(height + 200, 20_000),
});
await page.waitForTimeout(1000);
await page.screenshot({
  path: path.join(outDir, `${name}.png`),
  fullPage: true,
});
await browser.close();
console.log(
  `wrote ${outDir}/${name}.html (${(html.length / 1024).toFixed(0)} kB) and ${name}.png`
);
