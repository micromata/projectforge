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
 * The chat shots need the sample chat of dev-docs/website-screenshots/chat-seed.mjs. The ones of a confidential
 * conversation (ChatSecretOffer, ChatSecretDialog) log in a second sample user (max) as its other side; the
 * conversation is ended afterwards.
 *
 * Never run it against the main instance: its database is a copy of production, and the screenshots are published.
 *
 * The shots match the other ones of the website (dev-docs/website-screenshots/shoot.mjs): a viewport of 1440×860 at a
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
    threadReplies: "3 replies",
    secretAction: "Confidential conversation",
    secretStart: "Start",
    secretAccept: "Accept",
    secretDecline: "Decline",
    secretDialog: /^Confidential conversation with /,
    secretText: "Confidential text",
    secretSend: "Encrypt and send",
    secretEnd: "End conversation",
    secretMessage: "VPN password of the test system: 7rQ#m2-Lx9!vK",
    secretAnswer: "Got it, thanks – works.",
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
    threadReplies: "3 Antworten",
    secretAction: "Vertrauliches Gespräch",
    secretStart: "Starten",
    secretAccept: "Annehmen",
    secretDecline: "Ablehnen",
    secretDialog: /^Vertrauliches Gespräch mit /,
    secretText: "Vertraulicher Text",
    secretSend: "Verschlüsseln und senden",
    secretEnd: "Gespräch beenden",
    secretMessage: "VPN-Passwort des Testsystems: 7rQ#m2-Lx9!vK",
    secretAnswer: "Angekommen, danke – klappt.",
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

/** The sample chat (chat-seed.mjs): its project room, and the partner of laura's direct message. */
const CHAT_ROOM = "Fleet Tracking Platform";
const CHAT_PARTNER = "max";
const CHAT_PARTNER_NAME = "Max Bach";

/** Closes the notification banners on top of the page (sample messages of the chat seed notify, too). */
async function closeBanners(page) {
  const banner = page.getByTestId("notification-banner");
  // Bounded, in case a banner doesn't close or new ones keep coming.
  for (let i = 0; i < 10 && (await banner.count()); i++) {
    await banner.first().locator("button").last().click();
    await page.waitForTimeout(300);
  }
}

/** Opens a conversation of the chat page by the name shown in its list. */
async function openChat(page, name) {
  await open(page, "/chat");
  await closeBanners(page);
  await page.getByText(name, { exact: true }).first().click();
  await page.waitForURL(/\/chat\/\d+/);
  await settle(page);
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
  // The sample chat of dev-docs/website-screenshots/chat-seed.mjs.
  ChatRoom: async (page) => {
    await openChat(page, CHAT_ROOM);
  },
  ChatThread: async (page, lang) => {
    await openChat(page, CHAT_ROOM);
    await page
      .getByRole("button", { name: lang.threadReplies })
      .first()
      .click();
    await page.waitForTimeout(2500);
  },
  ChatList: async (page) => {
    await open(page, "/chat");
    await closeBanners(page);
  },
};

/**
 * The screenshots of a confidential conversation: both sides have to be logged in at the same time, so they are
 * taken by one run with a second browser context (the sample user CHAT_PARTNER).
 */
const SECRET_SHOTS = ["ChatSecretOffer", "ChatSecretDialog"];

const known = [...Object.keys(SHOTS), ...SECRET_SHOTS];
const names = process.argv.length > 2 ? process.argv.slice(2) : known;
const unknown = names.filter((name) => !known.includes(name));
if (unknown.length) {
  console.error(`unknown: ${unknown.join(", ")}; known: ${known.join(", ")}`);
  process.exit(2);
}

/** A browser context in the given language, logged in as the given sample user. */
async function login(browser, lang, user = USER) {
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
  await page.fill("#username", user);
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
  const account = await (
    await page.request.get(`${origin}/rs/myAccount`)
  ).json();
  const data = Object.fromEntries(
    ACCOUNT_FIELDS.map((field) => [field, account.data[field] ?? null])
  );
  const previous = Object.fromEntries(
    Object.keys(settings).map((field) => [field, data[field]])
  );
  if (
    Object.keys(settings).every((field) => previous[field] === settings[field])
  )
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

/** Takes a screenshot of the page into the website's folder, scaled down like the others. */
async function shoot(page, name, key) {
  const lang = LANGUAGES[key];
  const file = path.join(OUT, `2026-${name}${lang.suffix}.png`);
  await page.screenshot({ path: file });
  execFileSync("sips", ["-Z", "1920", file], { stdio: "ignore" });
  console.log(`${name} (${key}): ${file}`);
}

/**
 * A context of the user in the given language: logs in, switches the user's settings to the language and answers
 * them with the previous settings (null if nothing changed), to be restored afterwards.
 */
async function loginInLanguage(browser, lang, user = USER) {
  let { context, page } = await login(browser, lang, user);
  const previousSettings = await setUserSettings(page, lang.account);
  if (previousSettings) {
    // The session keeps the settings of the login (dates formatted by the server), so log in anew.
    await context.close();
    try {
      ({ context, page } = await login(browser, lang, user));
    } catch (error) {
      const again = await login(browser, lang, user).catch(() => null);
      if (again) {
        await setUserSettings(again.page, previousSettings).catch(() => {});
        await again.context.close();
      }
      throw error;
    }
  }
  return { context, page, previousSettings };
}

/**
 * laura offers max a confidential conversation in their direct message, max sees the offer (ChatSecretOffer) and
 * accepts it, both exchange a text, laura's dialog with the fingerprint is taken (ChatSecretDialog). Afterwards
 * the conversation is declined or ended, also after a failure, so nothing of it is left.
 */
async function secretConversation(browser, key, page, wanted) {
  const lang = LANGUAGES[key];
  const partner = await loginInLanguage(browser, lang, CHAT_PARTNER);
  const offer = partner.page.getByTestId("chat-secret-offer");
  const sendDialog = page.getByRole("dialog", { name: lang.secretDialog });
  const receiveDialog = partner.page.getByRole("dialog", {
    name: lang.secretDialog,
  });
  try {
    await openChat(page, CHAT_PARTNER_NAME);
    const conversation = new URL(page.url()).pathname.replace(
      /^.*\/chat\//,
      ""
    );
    await open(partner.page, `/chat/${conversation}`);
    await closeBanners(partner.page);
    await page.getByRole("button", { name: lang.secretAction }).click();
    await sendDialog.getByRole("button", { name: lang.secretStart }).click();
    await offer.waitFor({ timeout: 15_000 });
    await partner.page.waitForTimeout(1500);
    if (wanted.includes("ChatSecretOffer"))
      await shoot(partner.page, "ChatSecretOffer", key);
    if (!wanted.includes("ChatSecretDialog")) return;
    await offer.getByRole("button", { name: lang.secretAccept }).click();
    await sendDialog
      .getByTestId("chat-secret-fingerprint")
      .waitFor({ timeout: 15_000 });
    await sendDialog
      .getByRole("textbox", { name: lang.secretText })
      .fill(lang.secretMessage);
    await sendDialog.getByRole("button", { name: lang.secretSend }).click();
    await receiveDialog
      .getByTestId("chat-secret-text")
      .waitFor({ timeout: 15_000 });
    await receiveDialog
      .getByRole("textbox", { name: lang.secretText })
      .fill(lang.secretAnswer);
    await receiveDialog.getByRole("button", { name: lang.secretSend }).click();
    await sendDialog
      .getByTestId("chat-secret-text")
      .waitFor({ timeout: 15_000 });
    await page.waitForTimeout(2500);
    await shoot(page, "ChatSecretDialog", key);
  } finally {
    // Whatever state it reached: an offer is declined, a running conversation ended.
    const button = async (locator, name) => {
      const it = locator.getByRole("button", { name });
      if (await it.isVisible().catch(() => false))
        await it.click().catch(() => {});
    };
    await button(offer, lang.secretDecline);
    await button(receiveDialog, lang.secretEnd);
    await button(sendDialog, lang.secretEnd);
    await page.waitForTimeout(1500);
    if (partner.previousSettings)
      await setUserSettings(partner.page, partner.previousSettings);
    await partner.context.close();
  }
}

const browser = await chromium.launch();
let failed = false;
for (const key of langs) {
  const lang = LANGUAGES[key];
  const { context, page, previousSettings } = await loginInLanguage(
    browser,
    lang
  );
  try {
    for (const name of names.filter((it) => SHOTS[it])) {
      try {
        await SHOTS[name](page, lang);
        await shoot(page, name, key);
      } catch (error) {
        failed = true;
        console.error(`${name} (${key}): ${error.message.split("\n")[0]}`);
        // What the page showed when it failed, for finding the step that broke.
        await page
          .screenshot({
            path: `/tmp/site-screenshot-${name}${lang.suffix}-failed.png`,
          })
          .catch(() => {});
      }
    }
    const secretShots = names.filter((it) => SECRET_SHOTS.includes(it));
    if (secretShots.length) {
      try {
        await secretConversation(browser, key, page, secretShots);
      } catch (error) {
        failed = true;
        console.error(
          `${secretShots.join(", ")} (${key}): ${error.message.split("\n")[0]}`
        );
        await page
          .screenshot({
            path: `/tmp/site-screenshot-ChatSecret${lang.suffix}-failed.png`,
          })
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
