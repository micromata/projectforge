import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import { formatDateInput } from "../lib/date-parse";
import { CURRENCY_PAIR_METADATA } from "../lib/metadata/currency-pair.generated";
import { CURRENCY_PAIR_PAGE } from "../components/features/currency-pair/currency-pair.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { Page } from "@playwright/test";
import { writeHeaders } from "./fixtures/seed";

/**
 * The currency pair page against the live backend (see CURRENCY_PAIR_PAGE and CurrencyPairEntityRest).
 *
 * Read-only: the database is a copy of production, whose rates convert the invoices, so nothing is created. The
 * online rates and the writes of a rate are intercepted and answered here — what is asserted is how the dialog
 * proposes the online rates and shows the backend's answers, not the backend's rules (CurrencyPairEntityRestTest).
 */
test.describe("currency pair", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .post("/rs/currencyPair/filter/reset", {
        headers: await writeHeaders(page.request),
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under their labels", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/currencyPair");

    await expect(
      page.getByRole("heading", {
        name: format.t(CURRENCY_PAIR_PAGE.titleKey),
      })
    ).toBeVisible();
    for (const column of CURRENCY_PAIR_PAGE.columns) {
      if ("hiddenByDefault" in column && column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, CURRENCY_PAIR_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: startsWith(label(format, key)),
        }),
        `column ${columnIdOf(column)}`
        // The first list call of a session pays for the route being compiled and the caches being filled.
      ).toHaveCount(1, { timeout: 20_000 });
    }
  });

  test("proposes the online rates for a new rate and shows the backend's answers", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const id = await openFirstPair(page);

    const askedDates: (string | null)[] = [];
    await page.route("**/rs/currencyPair/rate/onlineRates*", async (route) => {
      const url = new URL(route.request().url());
      askedDates.push(url.searchParams.get("date"));
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          conversionRate: 0.92,
          inverseConversionRate: 1.0869565,
          info: "e2e: fetched online",
        }),
      });
    });
    const posted: unknown[] = [];
    let answer: "invalid" | "ok" = "invalid";
    await page.route("**/rs/currencyPair/rate/insert*", async (route) => {
      posted.push(route.request().postDataJSON());
      if (answer === "invalid") {
        await route.fulfill({
          status: 406,
          contentType: "application/json",
          body: JSON.stringify({
            validationErrors: [
              { fieldId: "validFrom", message: "e2e: date already taken" },
            ],
          }),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          entries: [
            {
              id: 999_999_999,
              validFrom: "2000-01-03",
              conversionRate: 0.92,
              inverseConversionRate: 1.0869565,
              comment: "e2e rate",
              currencyPairId: id,
            },
          ],
        }),
      });
    });

    await page.getByRole("button", { name: t("add"), exact: true }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(
      t("fibu.currencyConversion.conversionRate")
    );
    // Asked when the dialog opens, and again for a changed date.
    await expect(dialog.getByText("e2e: fetched online")).toBeVisible();
    const date = dialog.getByLabel(t("attr.validSince"), { exact: true });
    await date.fill(formatDateInput("2000-01-03", format.context));
    await date.press("Tab");
    await expect.poll(() => askedDates.at(-1)).toBe("2000-01-03");
    await dialog.getByLabel(t("comment"), { exact: true }).fill("e2e rate");
    await dialog.getByRole("button", { name: t("save") }).click();

    // A field error of the backend stands at the field it names; the dialog stays open.
    await expect(dialog.getByText("e2e: date already taken")).toBeVisible();
    expect(posted[0]).toMatchObject({
      data: {
        validFrom: "2000-01-03",
        conversionRate: 0.92,
        inverseConversionRate: 1.0869565,
        comment: "e2e rate",
        currencyPairId: id,
      },
    });

    answer = "ok";
    await dialog.getByRole("button", { name: t("save") }).click();
    await expect(dialog).toHaveCount(0);
    // The section shows what the backend answered; the only rate is the one in force today.
    await expect(
      page.locator('tr[data-validity="current"]', { hasText: "e2e rate" })
    ).toBeVisible();
  });
});

/**
 * Opens the form of the first currency pair of the list and answers its id. Skips when the list is empty (a fresh
 * slot database has no pairs) or the user may not write the pairs.
 */
async function openFirstPair(page: Page): Promise<number> {
  await goto(page, "/currencyPair");
  await expect(page.locator("table")).toBeVisible({ timeout: 20_000 });
  const rows = listRows(page);
  const count = await rows
    .first()
    .waitFor({ state: "visible", timeout: 20_000 })
    .then(() => 1)
    .catch(() => 0);
  test.skip(count === 0, "no currency pair in this database");
  await rows.first().click();
  await expect(page).toHaveURL(/\/currencyPair\/\d+$/, { timeout: 20_000 });
  const id = Number(page.url().match(/\/currencyPair\/(\d+)$/)?.[1]);
  const pair = (await (
    await page.request.get(`/rs/currencyPair/${id}`, {
      headers: { "X-PF-Frontend": "next" },
    })
  ).json()) as { writeAccess?: boolean | null };
  test.skip(
    pair.writeAccess === false,
    "no write access to the currency pairs"
  );
  const { t } = await userFormat(page);
  await expect(
    page.getByText(t("fibu.currencyConversion.rates"), { exact: true }).first()
  ).toBeVisible({ timeout: 20_000 });
  return id;
}

function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(\\s|$)`);
}
