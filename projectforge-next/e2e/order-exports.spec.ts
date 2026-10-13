import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { ORDER_PAGE } from "../components/features/order/order.page";
import type { Page } from "@playwright/test";

/**
 * The two exports of the order book, which Wicket offers in its content menu and the migrated list in
 * its toolbar: the filtered list as Excel, and the forecast.
 *
 * Both act on the filter the list is showing, so every case here narrows the list to the seeded order
 * first — not for the assertion, but for the size: over the whole order book the Excel export is 11 MB
 * and both take well over 20 s against a real database.
 *
 * The forecast case is the one that carries behaviour rather than plumbing. Unlike Wicket, which derives
 * the start month silently from the period-of-performance filter, the dialog asks for it and the backend
 * remembers the answer per user — so the round trip (export, reload, reopen) is what has to hold.
 */

// Both exports run for a while on a real database, and the dev server compiles the route on top of it.
test.describe.configure({ timeout: 180_000 });

test.describe("order book exports", { tag: "@lane-order" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The filter is stored per user, and a criterion left behind by another spec of this lane would
    // hide the seeded order the search is meant to find.
    await page.request
      .get("/rs/order/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("offers both exports in the toolbar's export menu", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order");
    await waitForList(page, format.t);

    // One opening of the menu shows both; a second click on the trigger would close it again.
    await expect(await excelButton(page, format)).toBeVisible();
    await expect(
      page.getByRole("menuitem", {
        name: new RegExp(`^${format.t("fibu.auftrag.forecastExportAsXls._")}`),
      })
    ).toBeVisible();
  });

  test("exports the filtered list as an Excel file", async ({
    loggedInPage: page,
    seededOrder,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order");
    await waitForList(page, format.t);
    await narrowTo(page, format, seededOrder.title);

    const download = page.waitForEvent("download");
    await (await excelButton(page, format)).click();
    // Named by the backend through Content-Disposition (`OrderExport` writes the legacy .xls format),
    // so the assertion is on the extension rather than on a name this side made up.
    expect((await download).suggestedFilename()).toMatch(/\.xls$/);
  });

  test("asks for the forecast's start month and remembers the answer", async ({
    loggedInPage: page,
    seededOrder,
  }) => {
    const format = await userFormat(page);
    const restore = await readForecastSettings(page);
    try {
      await goto(page, "/order");
      await waitForList(page, format.t);
      await narrowTo(page, format, seededOrder.title);

      await (await forecastButton(page, format)).click();
      const dialog = page.getByRole("dialog");
      const startDate = dialog.getByLabel(
        format.t("fibu.auftrag.forecastExport.startDate._")
      );
      // Preset, never empty: the stored answer, or the begin of the current year the first time.
      await expect(startDate).not.toHaveValue("");

      // A month of its own and the other budget variant, so the reopened dialog can only show these
      // two values if they were stored and read back.
      const chosen = "2025-04-01";
      await startDate.fill(format.date(chosen));
      const budget = dialog.getByLabel(
        format.t("fibu.auftrag.forecast.analysis.variants.true.label")
      );
      const budgetBefore = await budget.isChecked();
      await budget.click();

      const download = page.waitForEvent("download");
      await dialog
        .getByRole("button", { name: format.t("exportAsXls") })
        .click();
      // From the template (`ForecastTemplate.xlsx`), and named after the month and the variant.
      expect((await download).suggestedFilename()).toMatch(/\.xlsx$/);
      await expect(dialog).toBeHidden();

      // Not the same page state: what is preset has to come from the backend, not from what is still
      // in memory.
      await page.reload();
      await waitForList(page, format.t);
      await (await forecastButton(page, format)).click();
      const reopened = page.getByRole("dialog");
      await expect(
        reopened.getByLabel(format.t("fibu.auftrag.forecastExport.startDate._"))
      ).toHaveValue(format.date(chosen));
      const reopenedBudget = reopened.getByLabel(
        format.t("fibu.auftrag.forecast.analysis.variants.true.label")
      );
      await expect(reopenedBudget).toBeChecked({ checked: !budgetBefore });
    } finally {
      // The settings are the account's, not this run's — put back whatever was stored before.
      await writeForecastSettings(page, restore);
    }
  });
});

/** The list has arrived once its heading is up (see order.spec.ts). */
async function waitForList(page: Page, t: UserFormat["t"]) {
  await expect(
    page.getByRole("heading", { name: t(ORDER_PAGE.titleKey) })
  ).toBeVisible({ timeout: 60_000 });
}

/**
 * Narrows the list to one order, which is what keeps an export small enough to wait for: the whole
 * order book is 11 MB of Excel, and the exports read the same filter the list does.
 */
async function narrowTo(page: Page, format: UserFormat, title: string) {
  await page.getByPlaceholder(format.t("filter.searchList")).fill(title);
  await expect(page.getByRole("row", { name: new RegExp(title) })).toHaveCount(
    1,
    { timeout: 60_000 }
  );
}

/**
 * Opens the export menu of the toolbar and returns one of its entries. An entry's accessible name is its
 * label followed by its explanation, hence the match on the start.
 */
async function exportMenuItem(
  page: Page,
  format: UserFormat,
  labelKey: string
) {
  await page
    .getByRole("button", { name: format.t("export"), exact: true })
    .click();
  const label = format.t(labelKey).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return page.getByRole("menuitem", { name: new RegExp(`^${label}`) });
}

function excelButton(page: Page, format: UserFormat) {
  return exportMenuItem(page, format, "exportAsXls");
}

function forecastButton(page: Page, format: UserFormat) {
  return exportMenuItem(page, format, "fibu.auftrag.forecastExportAsXls._");
}

/** The stored dialog answer of the account, so a case that overwrites it can put it back. */
async function readForecastSettings(page: Page): Promise<unknown> {
  const res = await page.request.get("/rs/order/forecastExportSettings", {
    headers: { "X-PF-Frontend": "next" },
  });
  return res.ok() ? await res.json() : null;
}

/**
 * Writes the settings back through the export endpoint — the only one that stores them, since they are
 * saved on the way to the file (`OrderEntityRest.exportForecast`). A filter matching nothing keeps that
 * cheap: it stores the settings and answers 404 instead of building a sheet. The search string has to be
 * one no order can contain — it is tokenized, so a phrase like "no such order" still matches orders whose
 * title carries any of those words.
 */
async function writeForecastSettings(page: Page, settings: unknown) {
  if (!settings) return;
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  await page.request
    .post("/rs/order/exportForecast", {
      headers: { "X-PF-Frontend": "next", "X-PF-CSRF-Token": csrfToken },
      data: {
        filter: { searchString: "Qzxwvutsrq9876", entries: [] },
        settings,
      },
    })
    .catch(() => undefined);
}
