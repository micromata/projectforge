import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { kindName, pickKind, picker } from "./fixtures/period-kind";
import { periodKindsOf } from "../lib/date-period";
import { boundsOfPeriod, currentAnchorOf } from "../lib/date-period-bounds";
import { plusDays } from "../lib/date-period-math";
import { zonedPartsOf } from "../lib/user-zone";
import type { FilterElement } from "../lib/rs/types";
import {
  bound,
  cancelButton,
  filterField,
  listRequest,
  openPill,
  reopenPill,
  resetFilter,
} from "./fixtures/filter-pill";

/** The books list is where both filter kinds live; its DATE field is `lendOutDate`. */
const ENTITY = "book";

/** The books list's DATE filter field — the one the stepper is checked on. */
function dateField(page: Page): Promise<FilterElement> {
  return filterField(
    page,
    ENTITY,
    "DATE",
    "Does BookDao still index lendOutDate?"
  );
}

/** A STRING filter field of the list — a pill that does still start in its input. */
function textField(page: Page): Promise<FilterElement> {
  return filterField(page, ENTITY, "STRING", "");
}

/** The calendar section a list filter starts in — the art both fields fall back to. */
const MONTH = periodKindsOf(["month"])[0];

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
    await resetFilter(page, ENTITY);
  });

  test.afterEach(async ({ loggedInPage: page }) => {
    await resetFilter(page, ENTITY);
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
    // And the art the arrows page in stands in the trigger between them, spelled out — a filter has room
    // for the full name, unlike the form grid (see [PeriodQuickSelect] `longLabel`).
    await expect(picker(page, format)).toHaveText(kindName(format, MONTH));
  });

  test("sends the month as two dates once stepping settles", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Stepping applies to the list on its own (no save click), but must not close the popover: an
    // arrow that did would make stepping twice impossible, which is the whole point of the panel.
    const request = listRequest(page, ENTITY);
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    await expect(cancelButton(page, t)).toBeVisible();

    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    const entry = (await request).entries.find(
      (candidate) => candidate.field === field.id
    );
    expect(entry?.value.from).toBe(bounds.from);
    expect(entry?.value.to).toBe(bounds.to);
  });

  test("keeps a stepped range when the popover is dismissed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Step back, wait for the live apply to land, then dismiss with Escape: auto-save is the default,
    // so closing keeps what was applied — there is no save to forget.
    const request = listRequest(page, ENTITY);
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    await request;
    await page.keyboard.press("Escape");
    await expect(cancelButton(page, t)).toHaveCount(0);

    // Reopening reads the committed value back: the stepped month is still in effect, which it would
    // not be had dismissing dropped it (an unapplied draft would have left the pill empty and gone).
    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    await reopenPill(page, t, field.label!);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(bounds.from)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(bounds.to)
    );
  });

  test("takes a stepped range back to the open-time state on Abbrechen", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Commit one month back and close, so the pill has an open-time state worth restoring — the revert
    // is asserted on the range itself, not a list request, because restoring a range already fetched
    // is served from TanStack's cache and sends nothing.
    const back = page.getByRole("button", {
      name: t(MONTH.tooltipPreviousKey),
    });
    const committed = listRequest(page, ENTITY);
    await back.click();
    await committed;
    await page.keyboard.press("Escape");

    // Reopen and step once more — the draft moves to two months back — then Abbrechen throws that step
    // away, restoring the one-month-back range the popover opened with.
    await reopenPill(page, t, field.label!);
    await back.click();
    await cancelButton(page, t).click();

    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    await reopenPill(page, t, field.label!);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(bounds.from)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(bounds.to)
    );
  });

  test("sends a TIMESTAMP month as the instants bounding the user's days", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    await goto(page, "/book");
    await openPill(page, t, t("filter.history"));

    const request = listRequest(page, ENTITY);
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();

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

  test("jumps to the current month when its art is picked again", async ({
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

    // The art that is already in effect, picked again — where the button naming the period used to be.
    await pickKind(page, format, MONTH);

    const bounds = boundsOfPeriod(
      MONTH,
      currentAnchorOf(MONTH, context),
      context
    );
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(bounds.from)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(bounds.to)
    );
  });

  test("pages from the pill with its popover closed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Put a whole month in effect and close the popover — the pill grows its own arrows only once a
    // period is on it, and their whole point is to page with the popover shut, so the statistics line
    // above the list stays in view instead of being covered by the popover. Wait for the pick to apply
    // (it is debounced) before dismissing, or Escape would close over an uncommitted draft.
    const applied = listRequest(page, ENTITY);
    await pickKind(page, format, MONTH);
    await applied;
    await page.keyboard.press("Escape");
    await expect(cancelButton(page, t)).toHaveCount(0);

    // The previous-period arrow now on the pill — the popover being shut, it is the only one by that
    // name — pages live, and paging must not reopen the popover it is meant to spare the user.
    const request = listRequest(page, ENTITY);
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    await expect(cancelButton(page, t)).toHaveCount(0);

    const previous = MONTH.shift(currentAnchorOf(MONTH, context), -1, context);
    const bounds = boundsOfPeriod(MONTH, previous, context);
    const entry = (await request).entries.find(
      (candidate) => candidate.field === field.id
    );
    expect(entry?.value.from).toBe(bounds.from);
    expect(entry?.value.to).toBe(bounds.to);

    // Reopening reads the stepped month back: the pill applied and kept what the pill arrow paged to.
    await reopenPill(page, t, field.label!);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(bounds.from)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(bounds.to)
    );
  });

  test("pages from the month of a date typed by hand", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // A month other than the current one, so following the input is distinguishable from ignoring it.
    const typed = MONTH.shift(currentAnchorOf(MONTH, context), -4, context);
    await bound(page, format, field, "value").fill(
      format.date(MONTH.endOf(typed, context))
    );

    // One step back is the month before *that*, not the one before today's.
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();
    const previous = MONTH.shift(typed, -1, context);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(previous)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(MONTH.endOf(previous, context))
    );
  });

  test("keeps the art and drags the end when the begin is typed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // A whole month in effect, then a begin typed by hand into a different, mid-month day. The art holds
    // and the end follows it — for the calendar month the begin snaps to the first, the end to the last —
    // where a begin typed with no art in effect would just be that one date (see [editedDateValue]).
    await pickKind(page, format, MONTH);
    const other = MONTH.shift(currentAnchorOf(MONTH, context), -4, context);
    await bound(page, format, field, "value").fill(
      format.date(plusDays(other, 14))
    );

    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(MONTH.beginOf(other, context))
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(MONTH.endOf(other, context))
    );
    // The art is still the one between the arrows (spelled out in a filter), so paging goes on from where
    // the begin put it.
    await expect(picker(page, format)).toHaveText(kindName(format, MONTH));
  });

  test("pages from an end date given on its own", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t, context } = format;
    const field = await dateField(page);
    await goto(page, "/book");
    await openPill(page, t, field.label!);

    // Only the upper bound: with no start date to go by, that is the month the arrows have to page from.
    const typed = MONTH.shift(currentAnchorOf(MONTH, context), -4, context);
    await bound(page, format, field, "valueTo").fill(
      format.date(MONTH.beginOf(typed, context))
    );
    await page
      .getByRole("button", { name: t(MONTH.tooltipPreviousKey) })
      .click();

    const previous = MONTH.shift(typed, -1, context);
    await expect(bound(page, format, field, "value")).toHaveValue(
      format.date(previous)
    );
    await expect(bound(page, format, field, "valueTo")).toHaveValue(
      format.date(MONTH.endOf(previous, context))
    );
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
