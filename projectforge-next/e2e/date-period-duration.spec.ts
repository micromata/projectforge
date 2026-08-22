import type { Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import {
  durationOf,
  endOfDuration,
  shiftBounds,
  type Duration,
} from "../lib/date-duration";
import { todayIso } from "../lib/date-parse";

const MONTH = durationOf("month") as Duration;
const THREE_MONTHS = durationOf("threeMonths") as Duration;

// A form of dozens of fields against a live backend, and the first navigation to a route additionally
// waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

/**
 * The term beside a Leistungszeitraum, on the order edit page: pick "3 Monate" and the end is filled in
 * from the begin, and from then on moving the begin moves the end with it.
 *
 * Read-only — nothing is saved, so no order is left behind. Every expected date comes from
 * [endOfDuration], the very function the component computes with, and every text from the account's
 * catalog: a spelled-out "14.06.2026" or "3 Monate" would pass for one account and hide a real bug for
 * the rest.
 */
test.describe("period of performance as a term", () => {
  test("fills the end in from the begin", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(
      begin(page, format),
      "the form has to be hydrated before it is filled"
    ).toBeVisible();

    await type(begin(page, format), format.date("2026-03-15"));
    await pick(page, format, THREE_MONTHS);

    await expect(end(page, format)).toHaveValue(
      format.date(endOfDuration("2026-03-15", THREE_MONTHS))
    );
  });

  test("moves the end when only the begin changes", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    await type(begin(page, format), format.date("2026-03-15"));
    await pick(page, format, THREE_MONTHS);
    await type(begin(page, format), format.date("2026-03-20"));

    await expect(end(page, format)).toHaveValue(
      format.date(endOfDuration("2026-03-20", THREE_MONTHS))
    );
    // And the term is still the one in effect, so it can be moved again.
    await expect(picker(page, format)).toHaveText(
      name(format, THREE_MONTHS, true)
    );
  });

  test("keeps the term across a begin that was emptied", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    await type(begin(page, format), format.date("2026-03-15"));
    await pick(page, format, THREE_MONTHS);

    // The path the one bit of state exists for: with the begin cleared there is nothing left to measure
    // the term off, so a term read purely off the two dates would be gone by the time the next one is
    // typed — and the end would stay where it was.
    //
    // Focused first: the clear button of a date box stands in the calendar button's place and shows only
    // while the box has the focus (see DateInput), and `type` above left it.
    await begin(page, format).focus();
    await page
      .getByRole("button", {
        name: `${format.t("reset")}: ${label(format, "fibu.periodOfPerformance.from")}`,
      })
      .click();
    await expect(begin(page, format)).toHaveValue("");
    await type(begin(page, format), format.date("2026-04-01"));

    await expect(end(page, format)).toHaveValue(
      format.date(endOfDuration("2026-04-01", THREE_MONTHS))
    );
  });

  test("dissolves the term when the end is edited by hand", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    await type(begin(page, format), format.date("2026-03-15"));
    await pick(page, format, THREE_MONTHS);
    // A date that is no term from this begin.
    await type(end(page, format), format.date("2026-04-20"));

    // Nothing in effect: the trigger is down to its icon, and icons are no text.
    await expect(picker(page, format)).toHaveText("");
    // And from here the end is the user's again: moving the begin must not overwrite it.
    await type(begin(page, format), format.date("2026-03-20"));
    await expect(end(page, format)).toHaveValue(format.date("2026-04-20"));
  });

  test("starts a term with no begin today", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    // Both ends empty: a term has to begin somewhere, and a term remembered against an empty begin
    // would be state the user cannot see.
    await pick(page, format, MONTH);

    await expect(begin(page, format)).toHaveValue(format.date(todayIso()));
    await expect(end(page, format)).toHaveValue(
      format.date(endOfDuration(todayIso(), MONTH))
    );
  });
});

/**
 * The arrows beside the same period: they move it on by its own length, so what one click does depends on
 * the term in effect — three months at a time with "3 Monate", and by the days it spans with none.
 */
test.describe("paging a period of performance", () => {
  test("moves a term on and back by the term", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    await type(begin(page, format), format.date("2026-03-15"));
    await pick(page, format, THREE_MONTHS);
    await arrow(page, format, 1).click();

    await expectPeriod(page, format, "2026-03-15", THREE_MONTHS, 1);
    // The term is unchanged by paging — the moved period is the same length, measured off its new begin.
    await expect(picker(page, format)).toHaveText(
      name(format, THREE_MONTHS, true)
    );

    // Twice back, to show the arrows page relative to wherever the period is now rather than to where it
    // was typed.
    await arrow(page, format, -1).click();
    await arrow(page, format, -1).click();
    await expectPeriod(page, format, "2026-03-15", THREE_MONTHS, -1);
  });

  test("moves a range that is no term by the days it spans", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    await type(begin(page, format), format.date("2026-03-15"));
    await type(end(page, format), format.date("2026-04-20"));
    await arrow(page, format, 1).click();

    const moved = shiftBounds("2026-03-15", "2026-04-20", null, 1);
    await expect(begin(page, format)).toHaveValue(format.date(moved?.from));
    await expect(end(page, format)).toHaveValue(format.date(moved?.to));
    // And still no term: paging a hand-entered range must not invent one.
    await expect(picker(page, format)).toHaveText("");
  });

  test("keeps the two dates and the quick access on one line", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    // What the whole width of a date box is about: two of them, the dash and this group of three have to
    // share one column of the form grid. Overlapping vertically rather than a shared top edge — the two
    // controls are not exactly the same height — but wrapped underneath they would not overlap at all.
    const box = (await begin(page, format).boundingBox())!;
    const quickAccess = (await picker(page, format).boundingBox())!;
    expect(
      quickAccess.y < box.y + box.height &&
        box.y < quickAccess.y + quickAccess.height,
      "the quick access has wrapped below the two dates"
    ).toBe(true);
  });

  test("has nothing to page while an end is missing", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await expect(begin(page, format)).toBeVisible();

    // Both empty, and then half a period: neither has a length.
    await expect(arrow(page, format, 1)).toBeDisabled();
    await type(begin(page, format), format.date("2026-03-15"));
    await expect(arrow(page, format, -1)).toBeDisabled();
  });
});

/** Both ends after `steps` clicks, as [shiftBounds] computes them off the period the term makes. */
async function expectPeriod(
  page: Page,
  format: UserFormat,
  from: string,
  duration: Duration,
  steps: number
): Promise<void> {
  const moved = shiftBounds(
    from,
    endOfDuration(from, duration),
    duration,
    steps
  );
  await expect(begin(page, format)).toHaveValue(format.date(moved?.from));
  await expect(end(page, format)).toHaveValue(format.date(moved?.to));
}

/** One of the two paging arrows: back for a negative step, on for a positive one. */
function arrow(page: Page, format: UserFormat, steps: number): Locator {
  return page.getByRole("button", {
    name: format.t(steps < 0 ? "duration.previous" : "duration.next"),
  });
}

/**
 * Types a date and leaves the box: focusing a DateInput opens its calendar, and an open calendar would
 * sit over the picker beside it (see DateInput's `onFocus`).
 */
async function type(box: Locator, value: string): Promise<void> {
  await box.fill(value);
  await box.blur();
}

/**
 * How the term is named to this account — "3 Monate" is `duration.months` with its count filled in, and
 * the short form the trigger shows once it is picked is the same statement in two characters.
 */
function name(format: UserFormat, duration: Duration, short = false): string {
  return format.t(
    short ? duration.shortLabelKey : duration.labelKey,
    duration.labelArg == null ? {} : { arg0: duration.labelArg }
  );
}

/** The order's own period, whose two boxes are named by the fields of `AuftragDO`. */
function begin(page: Page, format: UserFormat): Locator {
  return page.getByLabel(label(format, "fibu.periodOfPerformance.from"), {
    exact: true,
  });
}

function end(page: Page, format: UserFormat): Locator {
  return page.getByLabel(label(format, "fibu.periodOfPerformance.to"), {
    exact: true,
  });
}

function picker(page: Page, format: UserFormat): Locator {
  return page.getByRole("combobox", { name: format.t("duration.choose") });
}

async function pick(
  page: Page,
  format: UserFormat,
  duration: Duration
): Promise<void> {
  await picker(page, format).click();
  await page
    .getByRole("option", { name: name(format, duration), exact: true })
    .click();
}
