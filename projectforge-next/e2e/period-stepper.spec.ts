import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import {
  boundsOfPeriod,
  currentAnchorOf,
  periodUnitsOf,
} from "../lib/date-period";
import { zonedPartsOf } from "../lib/user-zone";
import type { FilterElement } from "../lib/rs/types";

/** The message lookup of [userFormat], the only source of expected texts. */
type Translate = UserFormat["t"];

/** The one granularity the filters offer today; both fields default to it. */
const MONTH = periodUnitsOf(["month"])[0];

/**
 * Quick access to a whole period next to a date range, against the live backend — Wicket's
 * `QuickSelectPanel`.
 *
 * Both filter kinds are on the books list: a DATE field (`lendOutDate`) and the grouped
 * change-history TIMESTAMP field. The assertions are on the *wire format*, because that is where the
 * two differ and where a mistake is invisible on screen — a DATE bound is `yyyy-MM-dd`, a TIMESTAMP
 * bound an instant, and `PFDateTimeUtils` parses a date-only timestamp bound to null and drops the
 * criterion silently.
 *
 * Nothing is spelled out: the DATE field's label comes from `listMeta`, the texts from the account's
 * catalog, and every expected date from the very helpers the component uses. A hard coded
 * "August 2026" or `22:00Z` would pass for one account in one month and hide a real bug for the rest.
 *
 * Read-only apart from the filter itself, which is stored per user and per entity, so it is reset
 * around each case and cannot leak into the other books specs.
 */
test.describe("period stepper", () => {
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

  test("pages a DATE filter back by a whole month", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();

    // The month before the one today falls in, through the same helpers the component uses — so the
    // case cannot go stale and holds in any time zone.
    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(bounds.from)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(bounds.to)
    );
    // And the label now names a period in effect rather than hinting at the current one.
    await expect(
      page.getByRole("button", { name: t(MONTH.tooltipCurrentKey) })
    ).toHaveText(MONTH.label(previous, context));
  });

  test("sends the month as two dates, and only once saved", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Paging must neither fetch nor close the popover: an arrow that did would make stepping twice
    // impossible, which is the whole point of the panel.
    let fetched = false;
    page.on("request", (candidate) => {
      if (candidate.url().includes("/rs/book/list")) fetched = true;
    });
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    await expect(saveButton(page, t)).toBeVisible();
    expect(fetched).toBe(false);

    const request = listRequest(page);
    await saveButton(page, t).click();

    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    const entry = (await request).entries.find(
      (candidate) => candidate.field === field.id
    );
    expect(entry?.value.from).toBe(bounds.from);
    expect(entry?.value.to).toBe(bounds.to);
  });

  test("sends a TIMESTAMP month as the instants bounding the user's days", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    await goto(page, "/book");
    await openPill(page, t, t("filter.history"));

    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    const request = listRequest(page);
    await saveButton(page, t).click();

    const entry = (await request).entries.find(
      (candidate) => candidate.field === "modifiedInterval"
    );
    // An instant, not a bare date — the regression the history filter already guards.
    expect(entry?.value.from).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/
    );
    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    // Read back in the account's zone instead of compared against a fixed offset: the two ends of a
    // month carry different offsets whenever a DST switch falls inside it.
    expect(zonedPartsOf(entry?.value.from, context)).toEqual({
      date: bounds.from,
      time: "00:00",
    });
    expect(zonedPartsOf(entry?.value.to, context)).toEqual({
      date: bounds.to,
      time: "23:59",
    });
  });

  test("jumps to the current month and opens no unit menu", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Two steps away first, so landing on the current month is a move that can be seen.
    const back = page.getByRole("button", {
      name: t(MONTH.tooltipPreviousKey),
    });
    await back.click();
    await back.click();

    const label = page.getByRole("button", {
      name: t(MONTH.tooltipCurrentKey),
    });
    await label.click();
    // One granularity is offered, so the label is a plain button and nothing opens.
    await expect(page.getByRole("menu")).toHaveCount(0);

    const current = currentAnchorOf(MONTH, context);
    await expect(label).toHaveText(MONTH.label(current, context));
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(boundsOfPeriod(MONTH, current, context).from)
    );
  });

  test("names the month of a date typed by hand", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    const label = page.getByRole("button", {
      name: t(MONTH.tooltipCurrentKey),
    });
    // A month other than the current one, so following the input is distinguishable from ignoring it.
    const typed = MONTH.shift(currentAnchorOf(MONTH, context), -4, context);
    const day = MONTH.endOf(typed, context);
    await bound(page, format, field, "value").fill(format.date(day));
    await expect(label).toHaveText(MONTH.label(typed, context));

    // And one step back is the month before *that*, not the one before today's.
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    const previous = MONTH.shift(typed, -1, context);
    await expect(label).toHaveText(MONTH.label(previous, context));
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(MONTH.endOf(previous, context))
    );
  });

  test("names the month of an end date given on its own", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Only the upper bound: with no start date to go by, that is what the label has to follow.
    const typed = MONTH.shift(currentAnchorOf(MONTH, context), -4, context);
    await bound(page, format, field, "valueTo").fill(
      format.date(MONTH.beginOf(typed, context))
    );
    await expect(
      page.getByRole("button", { name: t(MONTH.tooltipCurrentKey) })
    ).toHaveText(MONTH.label(typed, context));
  });

  test("opens without a calendar over the pill", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, format.t, field.label!);

    // A date field opens its calendar as soon as it has the focus, so autofocusing the first bound
    // covered the second one and the stepper below it. Nothing is focused now, and the whole pill is
    // there to be read — the field is one click away.
    await expect(bound(page, format, field, "value")).not.toBeFocused();
    await expect(page.getByRole("grid")).toBeHidden();
    // Both quick-access halves are reachable straight away.
    await expect(
      page.getByRole("button", { name: format.t(MONTH.tooltipPreviousKey) })
    ).toBeVisible();
  });

  test("still puts the cursor into a text filter", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const field = await textField(page);
    await goto(page, "/book");
    await openPill(page, format.t, field.label!);

    // The other half of the rule: it is the *field* that decides, so opting a date range out must not
    // cost every other pill its autofocus — a filter opened to be typed into starts in its input.
    // By role: the pill's own trigger and its remove button carry the field's label as well.
    await expect(
      page.getByRole("textbox", { name: field.label!, exact: true })
    ).toBeFocused();
  });
});

/**
 * The filter fields the list offers, read from `listMeta` rather than named here — which fields a list
 * has follows from its DAO, and the labels are the backend's.
 */
async function filterElements(page: Page): Promise<FilterElement[]> {
  const response = await page.request.get("/rs/book/listMeta", {
    headers: { "X-PF-Frontend": "next" },
  });
  const meta = (await response.json()) as { filterElements?: FilterElement[] };
  return meta.filterElements ?? [];
}

/** The books list's first DATE filter field — the one the stepper is checked on. */
async function dateField(page: Page): Promise<FilterElement> {
  const field = (await filterElements(page)).find(
    (element) =>
      element.filterType === "DATE" && element.label && !element.group
  );
  if (!field) {
    throw new Error(
      "No DATE filter field on the books list. Does BookDao still index lendOutDate?"
    );
  }
  return field;
}

/** A STRING filter field of the list — a pill that does still start in its input. */
async function textField(page: Page): Promise<FilterElement> {
  const field = (await filterElements(page)).find(
    (element) =>
      element.filterType === "STRING" &&
      element.label &&
      !element.group &&
      !element.technical
  );
  if (!field) throw new Error("No plain text filter field on the books list.");
  return field;
}

/** One end of the range, labelled as [RangeField] labels it. */
function bound(
  page: Page,
  format: UserFormat,
  field: FilterElement,
  part: "value" | "valueTo"
) {
  return page.getByRole("textbox", {
    name: `${field.label}: ${format.t(`filter.${part}`)}`,
  });
}

function saveButton(page: Page, t: Translate) {
  return page.getByRole("button", { name: t("save"), exact: true });
}

/** Adds a filter pill from the "+" chip and waits for its popover. */
async function openPill(page: Page, t: Translate, name: string): Promise<void> {
  await page.getByRole("button", { name: t("filter.addField") }).click();
  await page.getByRole("option", { name, exact: true }).click();
  await expect(saveButton(page, t)).toBeVisible();
}

interface ListRequestBody {
  entries: { field: string; value: { from?: string; to?: string } }[];
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
