/**
 * Takes the screenshots of the website (site/uploads/screenshots) from an instance filled with the test data of
 * the setup (bin/pfDev.sh setup <slot>), so they can be redone after the pages changed:
 *
 *   PF_PASS=… node e2e/tools/site-screenshots.mjs [name …]
 *
 * PF_PASS is the admin password the first run of the slot printed (the sample users share it), PF_USER the
 * sample user (default laura, who has the HR rights), PF_BASE the app (default slot 1: http://localhost:8081/next,
 * or http://localhost:3001/next for its dev server). Without names, all screenshots are taken.
 *
 * Never run it against the main instance: its database is a copy of production, and the screenshots are published.
 *
 * The shots match the existing ones: 1920×1146 at a zoom of 1.2, light theme, without the development badge and
 * the customer's logo.
 */
import { chromium } from "playwright";
import path from "node:path";
import { fileURLToPath } from "node:url";

const BASE = process.env.PF_BASE ?? "http://localhost:8081/next";
const USER = process.env.PF_USER ?? "laura";
const PASSWORD = process.env.PF_PASS;
if (!PASSWORD) {
  console.error(
    "usage: PF_PASS=<admin password of the slot> node e2e/tools/site-screenshots.mjs [name …]"
  );
  process.exit(2);
}
const OUT = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../../site/uploads/screenshots"
);

/** Waits for the page to settle: data loaded, no spinner, fonts and charts drawn. */
async function settle(page) {
  await page.waitForLoadState("load");
  await page
    .locator("main, form, table")
    .first()
    .waitFor({ state: "visible", timeout: 60_000 });
  await page.waitForTimeout(2500);
}

async function open(page, appPath) {
  await page.goto(`${BASE}${appPath}`, { waitUntil: "domcontentloaded" });
  await settle(page);
}

/**
 * Picks an entry of an autocomplete: opens it (unless it is open already), types into its search and takes the
 * first option matching the text.
 */
async function pick(page, trigger, text) {
  if (trigger) await trigger.click();
  await page.keyboard.type(text, { delay: 30 });
  await page
    .getByRole("option", { name: new RegExp(text) })
    .first()
    .click();
  await page.waitForTimeout(1500);
}

/** The screenshots, by file name (2026-<name>.png). */
const SHOTS = {
  VacationList: async (page) => {
    await open(page, "/vacation");
  },
  VacationEdit: async (page) => {
    await open(page, "/vacation");
    await page
      .getByRole("row", { name: /Alex Peters/ })
      .first()
      .click();
    await settle(page);
  },
  VacationAccount: async (page) => {
    await open(page, "/vacationAccount");
    await pick(page, page.getByRole("combobox").first(), "Max Bach");
    await settle(page);
  },
  AvailabilityEdit: async (page) => {
    await open(page, "/availability");
    await page
      .getByRole("row", { name: /Training in Berlin/ })
      .first()
      .click();
    await settle(page);
  },
  AvailabilityQuickView: async (page) => {
    await open(page, "/availability");
    await page.getByRole("button", { name: /Who is available/ }).click();
    const dialog = page.getByRole("dialog", {
      name: "Availability at a glance",
    });
    await dialog.waitFor();
    // The picker opens with the dialog and keeps its search focused after each pick.
    for (const name of [
      "Alex Peters",
      "Chris Clark",
      "Julia White",
      "Max Bach",
      "Mona Nike",
    ]) {
      await pick(page, null, name);
    }
    await dialog.getByRole("radio", { name: "8" }).click();
    // Closes the picker's list, so the bars are not covered.
    await dialog.getByRole("heading").first().click();
    await page.waitForTimeout(2000);
  },
  TeamVacationCalendar: async (page) => {
    await open(page, "/vacationExport");
  },
};

const names =
  process.argv.length > 2 ? process.argv.slice(2) : Object.keys(SHOTS);
const unknown = names.filter((name) => !SHOTS[name]);
if (unknown.length) {
  console.error(
    `unknown: ${unknown.join(", ")}; known: ${Object.keys(SHOTS).join(", ")}`
  );
  process.exit(2);
}

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 1600, height: 955 },
  deviceScaleFactor: 1.2,
  colorScheme: "light",
  locale: "en-US",
});
// Neither the development badge nor the logo of the installation belong on the website.
await context.route("**/rsPublic/systemStatus*", async (route) => {
  const response = await route.fetch();
  const status = await response.json();
  await route.fulfill({
    response,
    json: {
      ...status,
      developmentMode: false,
      testsystemMode: false,
      logoUrl: undefined,
      logoUrlDark: undefined,
    },
  });
});
// The dev server's own tools (Next's indicator, the TanStack Query devtools) neither.
await context.addInitScript(() => {
  document.addEventListener("DOMContentLoaded", () => {
    const style = document.createElement("style");
    style.textContent =
      "nextjs-portal, .tsqd-parent-container { display: none !important; }";
    document.head.append(style);
  });
});
const page = await context.newPage();

await page.goto(`${BASE}/login`, { waitUntil: "domcontentloaded" });
await page.waitForFunction(() =>
  Object.keys(document.querySelector("form") ?? {}).some((k) =>
    k.startsWith("__reactProps$")
  )
);
await page.fill("#username", USER);
await page.fill("#password", PASSWORD);
await page.click('button[type="submit"]');
for (let i = 0; i < 40 && page.url().includes("/login"); i++)
  await page.waitForTimeout(500);
if (page.url().includes("/login")) {
  console.error("login failed:", page.url());
  process.exit(1);
}

let failed = false;
for (const name of names) {
  const file = path.join(OUT, `2026-${name}.png`);
  try {
    await SHOTS[name](page);
    await page.screenshot({ path: file });
    console.log(`${name}: ${file}`);
  } catch (error) {
    failed = true;
    console.error(`${name}: ${error.message.split("\n")[0]}`);
    // What the page showed when it failed, for finding the step that broke.
    await page
      .screenshot({ path: `/tmp/site-screenshot-${name}-failed.png` })
      .catch(() => {});
  }
}
await browser.close();
process.exit(failed ? 1 : 0);
