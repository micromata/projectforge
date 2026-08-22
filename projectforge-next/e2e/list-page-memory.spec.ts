import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { Locator, Page } from "@playwright/test";

/**
 * Looking at an entry and coming back returns to the page of the pagination the list was left on, to
 * the offset it was scrolled to, and marks the entry that was looked at.
 *
 * Worth an e2e test because nothing smaller can see it: the page index is remembered for as long as
 * the document lives (see use-list-view-memory.ts), so what is under test is precisely what happens
 * across a navigation and a browser-back — a unit test would have to fake the one thing that can go
 * wrong, the unmount.
 *
 * Reads only — no entry is created or changed.
 */
/**
 * A row of the loaded list. The `data-row-id` is what tells it from the eight skeleton rows a table
 * shows while its first response is in flight (see DataTable) — those are `tbody tr` as well, and
 * waiting for one of them would measure the loading state.
 */
const ROW = "tbody tr[data-row-id]";

/**
 * The least a page has to overflow by to be worth scrolling: a couple of rows above and below what is
 * on screen, so the offset is a place the list can come back to and not simply the bottom.
 */
const MIN_OVERFLOW = 60;

test.describe("list page memory", () => {
  // A criterion left behind by another spec would cut the list down to a single page, and there is
  // then no page to return to. Resets what the server stores for this account, as the other list
  // specs do.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("returns to the page, the offset and the entry the list was left with", async ({
    loggedInPage: page,
  }) => {
    test.setTimeout(90_000);
    // The button labels are the user's, as everywhere in these tests (see fixtures/format.ts).
    const { t } = await userFormat(page);
    // A short window, so the table overflows whatever page size is stored for this account — the
    // offset is the second half of the feature and needs room to exist.
    await page.setViewportSize({ width: 1280, height: 400 });
    await goto(page, "/book");
    const column = tableColumn(page);
    await expect(page.locator(ROW).first()).toBeVisible({
      timeout: 30_000,
    });

    // Any page but the first, and whichever one the list offers rather than a chosen number: page one
    // is what a list that forgot its pagination shows, so a test carried out on it would pass against
    // the very bug — while *which* other page it is says nothing, and the pagination only ever shows a
    // window of numbers around the current one (see pageNumbers).
    //
    // Polled until it holds twice over: the list also brings the entry this account edited last into
    // view, which lands after the rows do and takes the table to *that* row's page (see
    // useHighlightedRow) — a page stepped to before that has happened would be left again.
    const next = page.getByRole("button", { name: t("table.nextPage") });
    let left = "1";
    await expect
      .poll(
        async () => {
          const before = await activePage(page);
          if (before === "1") await next.click();
          left = await activePage(page);
          return left !== "1" && left === before;
        },
        { timeout: 30_000, intervals: [250] }
      )
      .toBe(true);

    const room = await overflow(column);
    test.skip(
      room < MIN_OVERFLOW,
      "this page of the list holds too few rows to scroll"
    );
    await watchScrolling(page);
    // Halfway down what the page can be scrolled: rows stay above and below, so the offset says
    // something the top or the bottom would not.
    await column.evaluate(
      (el, top) => el.scrollTo({ top }),
      Math.floor(room / 2)
    );

    const rowId = await centreRowId(column);
    const target = page.locator(`tbody tr[data-row-id="${rowId}"]`);
    await target.click();
    await expect(page).toHaveURL(new RegExp(`/book/${rowId}/?$`), {
      timeout: 30_000,
    });
    // Read from the watcher instead of from the table before the click, and this is why the watcher
    // exists: the column keeps moving by a few px right up to the navigation — the logo row collapses
    // in answer to the scroll and grows it (see useCollapseOnScroll), and Playwright brings the row it
    // is about to click fully into view. Any earlier read is a stale offset, and the assertion below
    // would then be about a place the list never was.
    const offset = await lastScrollTop(page);
    expect(offset, "the table must have room to scroll").toBeGreaterThan(0);

    await page.goBack();

    await expect.poll(() => activePage(page), { timeout: 30_000 }).toBe(left);
    // The row that was clicked, back where it was: the assertion is on the offset rather than on the
    // row being visible, because a table reset to the top shows rows too — just not these.
    await expect
      .poll(() => scrollTop(column), { timeout: 30_000 })
      .toBe(offset);
    const opened = page.locator(`tbody tr[data-row-id="${rowId}"]`);
    await expect(opened).toBeInViewport();
    // And it says which entry that was, as returning from a save or a cancel does — here without the
    // backend knowing anything about it, since looking at an entry writes nothing (see
    // recallMarkedRowId).
    await expect(opened).toHaveClass(/row-highlighted/);
  });
});

/** The scroll column of a list: the table's own scrollable parent (see DataTable). */
function tableColumn(page: Page): Locator {
  return page.locator("table").locator("..");
}

function scrollTop(column: Locator): Promise<number> {
  return column.evaluate((el) => el.scrollTop);
}

/**
 * The number the pagination shows as the current page, told apart by the style it gives that one
 * button. Filtered to a number, so a primary-styled button elsewhere on the page cannot be mistaken
 * for it.
 */
async function activePage(page: Page): Promise<string> {
  const active = page
    .locator("button.bg-primary")
    .filter({ hasText: /^\d+$/ })
    .first();
  return (await active.innerText({ timeout: 10_000 })).trim();
}

/** How far the column can be scrolled at all. */
function overflow(column: Locator): Promise<number> {
  return column.evaluate((el) => el.scrollHeight - el.clientHeight);
}

/** Where the watcher keeps the offset of the last scroll it saw. */
const WATCHER = "__lastScrollTop";

/**
 * Records the offset of every scroll of a table column, so the last one before a navigation can be
 * read afterwards.
 *
 * On `window` and in the capture phase: a `scroll` event does not bubble, but it does travel down to
 * its target, and the column the test cares about is replaced on every mount — a listener on the
 * element itself would go with it. The value survives the navigation because the app never leaves the
 * document (client-side routing), which is the same reason the feature under test works at all.
 */
function watchScrolling(page: Page): Promise<void> {
  return page.evaluate((key) => {
    window.addEventListener(
      "scroll",
      (event) => {
        const target = event.target;
        if (!(target instanceof HTMLElement) || !target.querySelector("table"))
          return;
        (window as unknown as Record<string, number>)[key] = target.scrollTop;
      },
      true
    );
  }, WATCHER);
}

function lastScrollTop(page: Page): Promise<number> {
  return page.evaluate(
    (key) => (window as unknown as Record<string, number>)[key] ?? 0,
    WATCHER
  );
}

/** The id of the row halfway down what the column shows. */
function centreRowId(column: Locator): Promise<string | null> {
  return column.evaluate((el) => {
    const middle = el.getBoundingClientRect().top + el.clientHeight / 2;
    const distance = (row: Element) => {
      const box = row.getBoundingClientRect();
      return Math.abs(box.top + box.height / 2 - middle);
    };
    const rows = Array.from(
      el.querySelectorAll<HTMLElement>("tbody tr[data-row-id]")
    );
    const closest = rows.reduce((best, row) =>
      distance(row) < distance(best) ? row : best
    );
    return closest?.dataset.rowId ?? null;
  });
}
