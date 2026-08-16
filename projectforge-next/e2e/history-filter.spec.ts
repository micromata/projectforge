import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { formatTimeInput, hourLabelOf, timeOf } from "../lib/time-parse";

/** The message lookup of [userFormat], the only source of expected texts. */
type Translate = (
  key: string,
  values?: Record<string, string | number>
) => string;

/**
 * The combined change-history filter on the books list, against the live backend.
 *
 * What it guards is the wire format, because that is where the two bugs this feature fixed lived:
 * a timestamp sent as a bare date and a user sent as a search term both parse to null server-side
 * and the criterion is dropped *silently* — the list looks fine and simply doesn't filter. Asserting
 * on the request body catches that; asserting on the rendered pill would not.
 *
 * Read-only apart from the filter itself, which is stored per user and per entity; `afterAll` resets
 * it so it cannot leak into the other books specs.
 */
test.describe("history filter", () => {
  // Before each, not just once at the end: the backend stores the filter per user and per entity, so a
  // criterion one case saved is still there in the next one — and saving an unchanged filter sends no
  // list request at all, which is what the assertions here wait for.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test.afterAll(async ({ request }) => {
    await request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("groups the three history fields into one entry", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");

    await page.getByRole("button", { name: t("filter.addField") }).click();
    const list = page.getByRole("listbox");
    await expect(
      list.getByRole("option", { name: t("filter.history"), exact: true })
    ).toHaveCount(1);
    // The three backend labels do not appear on their own — that is what "one pill" means.
    for (const key of [
      "modifiedBy",
      "modificationTime",
      "modifiedHistoryValue",
    ]) {
      await expect(
        list.getByRole("option", { name: t(key), exact: true })
      ).toHaveCount(0);
    }
  });

  test("sends a parsable instant for a quick-select period", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    await openHistoryPill(page, t);

    const request = listRequest(page);
    await choosePeriod(page, t, t("search.lastMinutes", { arg0: 30 }));
    await page.getByRole("button", { name: t("save"), exact: true }).click();

    const interval = (await request).entries.find(
      (entry) => entry.field === "modifiedInterval"
    );
    // The regression guard: a date-only "2026-08-09" is what the old RangeField sent, and
    // PFDateTimeUtils parses it to null for a timestamp field.
    expect(interval?.value.from).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/
    );
    expect(interval?.value.to).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/
    );
  });

  test("writes the time in the notation the account has chosen", async ({
    loggedInPage: page,
  }) => {
    const { t, context } = await userFormat(page);
    await goto(page, "/book");
    await openHistoryPill(page, t);

    // A preset fills both bounds, so the inputs show a real time without typing one.
    await choosePeriod(page, t, t("search.lastMinutes", { arg0: 30 }));

    const from = page.getByLabel(
      `${t("modificationTime")}: ${t("filter.timeFrom")}`
    );
    // 12h shows a day period, 24h an hour past noon can reach — either way it is the account's
    // setting that decides, never the browser's, which a native time input would have used.
    await expect(from).toHaveValue(
      context.hour12 ? /^\d{1,2}:\d{2}\s+\S+$/ : /^\d{2}:\d{2}$/
    );
  });

  test("opens each picker by focusing its own field", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    await openHistoryPill(page, t);
    await choosePeriod(page, t, t("search.lastMinutes", { arg0: 30 }));

    // By role, because the clear button inside the field carries its label too.
    const date = page.getByRole("textbox", {
      name: `${t("modificationTime")}: ${t("filter.dateFrom")}`,
    });
    const grid = page.getByRole("grid");
    await date.focus();
    await expect(grid).toBeVisible();
    // Still typable: the popover must not have taken the caret away from the field.
    await expect(date).toBeFocused();

    // Moving to the time field swaps one picker for the other, rather than leaving both open.
    await page
      .getByRole("textbox", {
        name: `${t("modificationTime")}: ${t("filter.timeFrom")}`,
      })
      .focus();
    await expect(grid).toBeHidden();
    await expect(
      page.getByRole("button", { name: "45", exact: true })
    ).toBeVisible();
  });

  test("sets a time from the picker columns", async ({
    loggedInPage: page,
  }) => {
    const { t, context } = await userFormat(page);
    await goto(page, "/book");
    await openHistoryPill(page, t);

    // A preset first: without a date there is no instant to attach a time to, so the field waits.
    await choosePeriod(page, t, t("search.lastMinutes", { arg0: 30 }));

    // The lower bound's picker — the upper bound has one of its own.
    await page
      .getByRole("button", { name: t("calendar.chooseTime") })
      .first()
      .click();
    // Hour and minute in one visit — the popover deliberately stays open between the two. The hour is
    // labelled in the account's notation, the minute always as two digits, so neither label occurs in
    // the other column.
    await page
      .getByRole("button", { name: hourLabelOf(13, context), exact: true })
      .click();
    await page.getByRole("button", { name: "45", exact: true }).click();

    await expect(
      page.getByLabel(`${t("modificationTime")}: ${t("filter.timeFrom")}`)
    ).toHaveValue(formatTimeInput(timeOf(13, 45), context));
  });

  test("picks the modifying user by id, not by name", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // The account's own name is a term that is guaranteed to have a match. `user/autosearch`
    // searches the username as well, which every account has.
    const status = await page.request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const { username, lastName } = (
      (await status.json()) as {
        userData: { username: string; lastName?: string };
      }
    ).userData;
    const term = (lastName ?? username).slice(0, 3);

    await goto(page, "/book");
    await openHistoryPill(page, t);

    await page.getByRole("combobox", { name: t("modifiedBy") }).click();
    // Scoped to the popover the click opened, not by placeholder: `getByPlaceholder` matches a
    // substring, so "Suchen..." also hits the list's own "Liste durchsuchen..." box behind it.
    const popover = page.locator('[data-slot="popover-content"]');
    await popover.getByPlaceholder(t("filter.search")).fill(term);
    // Scoped to the suggestion list: the page-size select contributes options of its own.
    const suggestion = page.getByRole("listbox").getByRole("option").first();
    await expect(suggestion).toBeVisible();
    await suggestion.click();

    const request = listRequest(page);
    await page.getByRole("button", { name: t("save"), exact: true }).click();

    const user = (await request).entries.find(
      (entry) => entry.field === "modifiedByUser"
    );
    // `MagicFilterProcessor` reads `value.id`; a name in `value.value` yields null and is ignored.
    expect(typeof user?.value.id).toBe("number");
  });

  test("sends the history term without wildcards", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    await openHistoryPill(page, t);

    await page.getByLabel(t("modifiedHistoryValue")).fill("Titel");
    const request = listRequest(page);
    await page.getByRole("button", { name: t("save"), exact: true }).click();

    const value = (await request).entries.find(
      (entry) => entry.field === "historySearch"
    );
    // DBHistoryQuery.searchHistoryEntryByFullTextQuery appends the `*` itself.
    expect(value?.value.value).toBe("Titel");
  });

  test("applies the history filter from the all-filters panel", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");

    // The panel is the picker's second view, so it shares the "+" chip with the field list.
    await page.getByRole("button", { name: t("filter.addField") }).click();
    await page
      .getByRole("button", { name: t("filter.allFilters"), exact: true })
      .click();
    await choosePeriod(page, t, t("search.lastMinutes", { arg0: 30 }));

    const request = listRequest(page);
    await page.getByRole("button", { name: t("apply"), exact: true }).click();

    const interval = (await request).entries.find(
      (entry) => entry.field === "modifiedInterval"
    );
    expect(interval?.value.from).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/
    );
    // And the panel's result shows up as the grouped pill, exactly as the pill's own path does.
    await expect(
      page.getByRole("button", {
        name: t("filter.editEntry", { arg0: t("filter.history") }),
      })
    ).toHaveCount(1);
  });

  test("removes all three criteria at once", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    await openHistoryPill(page, t);

    await page.getByLabel(t("modifiedHistoryValue")).fill("Titel");
    await choosePeriod(page, t, t("search.lastMinutes", { arg0: 30 }));
    await page.getByRole("button", { name: t("save"), exact: true }).click();

    await page
      .getByRole("button", {
        name: t("filter.removeEntry", { arg0: t("filter.history") }),
      })
      .click();
    await expect(
      page.getByRole("button", { name: new RegExp(t("filter.history")) })
    ).toHaveCount(0);

    // The removal itself sends nothing: it restores the filter the page was loaded with, which
    // TanStack Query still holds in its cache. Typing into the list's search box is the cheapest way
    // to ask for a filter no cache has — and it carries the remaining entries along, which is what
    // this asserts on. (Turning a page would not: the page index is a client-side concern here.)
    const request = listRequest(page);
    await page.getByPlaceholder(t("filter.searchList")).fill("x");

    const fields = (await request).entries.map((entry) => entry.field);
    expect(fields).not.toContain("historySearch");
    expect(fields).not.toContain("modifiedInterval");
    expect(fields).not.toContain("modifiedByUser");
  });
});

/**
 * Picks a quick-select period from the interval field's dropdown, named after its placeholder — the
 * periods are a list rather than a row of chips, so there is no button of their own to click.
 */
async function choosePeriod(
  page: Page,
  t: Translate,
  name: string
): Promise<void> {
  await page.getByRole("combobox", { name: t("filter.periodChoose") }).click();
  await page.getByRole("option", { name, exact: true }).click();
}

/** Adds the history pill from the "+" chip and waits for its popover. */
async function openHistoryPill(page: Page, t: Translate): Promise<void> {
  await page.getByRole("button", { name: t("filter.addField") }).click();
  await page
    .getByRole("option", { name: t("filter.history"), exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: t("save"), exact: true })
  ).toBeVisible();
}

interface ListRequestBody {
  entries: {
    field: string;
    value: {
      value?: string;
      id?: number;
      from?: string;
      to?: string;
    };
  }[];
}

/** The body of the next list request the page sends — where the filter actually shows up. */
async function listRequest(page: Page): Promise<ListRequestBody> {
  const request = await page.waitForRequest(
    (candidate) =>
      candidate.url().includes("/rs/book/list") &&
      candidate.method() === "POST",
    { timeout: 15_000 }
  );
  return JSON.parse(request.postData() ?? "{}") as ListRequestBody;
}
