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
 * Every screenshot is taken in English (2026-<name>.png) and in German (2026-<name>-de.png, for the German
 * website); PF_LANGS=en or PF_LANGS=de takes one language only. The sample users have the locale "en" and the US date
 * format of the installation, so the user's locale and date formats are switched for the German shots and switched
 * back afterwards.
 *
 * Never run it against the main instance: its database is a copy of production, and the screenshots are published.
 *
 * The shots match the other ones of the website (doc/website-screenshots/shoot.mjs): a viewport of 1440×860 at a
 * zoom of 2, scaled down to 1920×1146 (sips, macOS), light theme, without the development badge and the
 * customer's logo.
 */
import { chromium } from "playwright";
import { execFileSync } from "node:child_process";
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

/**
 * The languages: locale of the browser, settings of the user (null: the installation's default), suffix of the
 * file name and the texts the scenes look for (I18nResources*.properties).
 */
const LANGUAGES = {
  en: {
    browser: "en-US",
    account: { locale: "en", dateFormat: null, excelDateFormat: null },
    suffix: "",
    quickViewButton: /Who is available/,
    quickViewTitle: "Availability at a glance",
  },
  de: {
    browser: "de-DE",
    // The default date format of the test system is the US one.
    account: {
      locale: "de",
      dateFormat: "dd.MM.yyyy",
      excelDateFormat: "DD.MM.YYYY",
    },
    suffix: "-de",
    quickViewButton: /Wer ist verfügbar/,
    quickViewTitle: "Verfügbarkeit auf einen Blick",
  },
};
const langs = (process.env.PF_LANGS ?? "en,de").split(",");
if (langs.some((lang) => !LANGUAGES[lang])) {
  console.error(`PF_LANGS: ${Object.keys(LANGUAGES).join(", ")}`);
  process.exit(2);
}

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
  AvailabilityQuickView: async (page, lang) => {
    await open(page, "/availability");
    await page.getByRole("button", { name: lang.quickViewButton }).click();
    const dialog = page.getByRole("dialog", { name: lang.quickViewTitle });
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

/** A browser context in the given language, logged in as the sample user. */
async function login(browser, lang) {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 860 },
    deviceScaleFactor: 2,
    colorScheme: "light",
    locale: lang.browser,
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
  return { context, page };
}

/** The editable fields of the user's account (MyAccountRest.MyAccountData, see lib/rs/my-account.ts). */
const ACCOUNT_FIELDS = [
  "firstname",
  "lastname",
  "locale",
  "dateFormat",
  "excelDateFormat",
  "timeNotation",
  "timeZone",
  "personalPhoneIdentifiers",
  "gpgPublicKey",
  "sshPublicKey",
];

/**
 * Sets account settings (locale, formats) of the logged-in user. Answers the previous values of these settings,
 * or null if nothing had to change.
 */
async function setUserSettings(page, settings) {
  const origin = new URL(BASE).origin;
  const account = await (await page.request.get(`${origin}/rs/myAccount`)).json();
  const data = Object.fromEntries(
    ACCOUNT_FIELDS.map((field) => [field, account.data[field] ?? null])
  );
  const previous = Object.fromEntries(
    Object.keys(settings).map((field) => [field, data[field]])
  );
  if (Object.keys(settings).every((field) => previous[field] === settings[field]))
    return null;
  const { csrfToken } = await (
    await page.request.get(`${origin}/rs/userStatus`)
  ).json();
  const response = await page.request.post(`${origin}/rs/myAccount/save`, {
    data: { data: { ...data, ...settings } },
    headers: { "X-PF-CSRF-Token": csrfToken },
  });
  if (!response.ok()) {
    throw new Error(
      `settings ${JSON.stringify(settings)}: ${response.status()} ${await response.text()}`
    );
  }
  return previous;
}

const browser = await chromium.launch();
let failed = false;
for (const key of langs) {
  const lang = LANGUAGES[key];
  let { context, page } = await login(browser, lang);
  const previousSettings = await setUserSettings(page, lang.account);
  if (previousSettings) {
    // The session keeps the settings of the login (dates formatted by the server), so log in anew.
    await context.close();
    ({ context, page } = await login(browser, lang));
  }
  try {
    for (const name of names) {
      const file = path.join(OUT, `2026-${name}${lang.suffix}.png`);
      try {
        await SHOTS[name](page, lang);
        await page.screenshot({ path: file });
        execFileSync("sips", ["-Z", "1920", file], { stdio: "ignore" });
        console.log(`${name} (${key}): ${file}`);
      } catch (error) {
        failed = true;
        console.error(`${name} (${key}): ${error.message.split("\n")[0]}`);
        // What the page showed when it failed, for finding the step that broke.
        await page
          .screenshot({ path: `/tmp/site-screenshot-${name}${lang.suffix}-failed.png` })
          .catch(() => {});
      }
    }
  } finally {
    if (previousSettings) await setUserSettings(page, previousSettings);
    await context.close();
  }
}
await browser.close();
process.exit(failed ? 1 : 0);
