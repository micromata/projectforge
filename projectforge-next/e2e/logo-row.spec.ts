import {
  test as base,
  expect,
  type Locator,
  type Page,
} from "@playwright/test";
import { test, goto, waitForHydration } from "./fixtures/auth";
import { locales, translate, userFormat } from "./fixtures/format";
import { LOGO_ROW_HEIGHT } from "../hooks/use-collapse-on-scroll";

/**
 * The logo row at the very top gives way when the user scrolls, and comes back at the top again.
 *
 * Worth an e2e test because the mechanism cannot be seen anywhere else: the page itself does not
 * scroll (PageShell is h-screen/overflow-hidden), so the row is driven by whichever column inside it
 * does — and what makes the trade worthwhile is precisely what must *not* move with it, the toolbar
 * and the table's sticky header. A unit test can only pin the arithmetic (see
 * hooks/use-collapse-on-scroll.test.ts); that the wiring reaches the row is this test's half.
 *
 * Reads only — no entry is created or changed.
 */
test.describe("logo row", () => {
  // A criterion left behind by another spec would cut the list down to a handful of rows, and a column
  // with no room to scroll declines to collapse on purpose (see nextCollapsed) - so the test would
  // fail on the state of the shared account rather than on the feature. Resets what the server stores
  // for it, as the other list specs do.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("collapses when the table scrolls and comes back at the top", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // A short window rather than a long list: the column needs room to scroll, and how many rows it
    // holds is the account's business - the page size *and* the page last visited are stored per user,
    // and the last page of a list holds only its remainder (13 of 1963). Shrinking the viewport makes
    // the overflow follow from the window instead, which no earlier spec can have changed.
    await page.setViewportSize({ width: 1280, height: 400 });
    await goto(page, "/book");
    // No waitForHydration here: it waits for a hydrated <form>, which a list page has none of. The
    // assertions below are web-first and wait for the rows on their own.
    const row = logoRow(page);
    // The table's own column, not the window: `window.scrollBy` would do nothing at all here, which
    // is the whole point of the design and would make a naive test pass against a broken feature.
    const column = tableColumn(page);
    // Room to scroll, and by more than the row's height: a column that overflows by less declines to
    // collapse on purpose (see nextCollapsed).
    await expect(page.locator("tbody tr").first()).toBeVisible();
    await expect
      .poll(() => column.evaluate((el) => el.scrollHeight - el.clientHeight))
      .toBeGreaterThan(200);

    // Put the column at the top first, and keep putting it there: coming back from an edit page the
    // list brings the entry the backend remembers into view (see useHighlightedRow), which lands after
    // the rows do - so a single reset can be undone right after it, and the row would still be
    // collapsed with no expanded height to measure. Re-asserting the top on every poll costs one
    // iteration and settles it.
    //
    // The state attribute, not the height: mid-animation a collapsing row is still tens of pixels
    // tall, so a height above zero says nothing about which state it is heading for - and while it is
    // heading for collapsed it is aria-hidden, which the assertion below would then fail on.
    await expect
      .poll(() =>
        scrollToTop(column).then(() => row.getAttribute("data-collapsed"))
      )
      .toBe("false");
    await expect(byRole(page, t("logo.projectforge"))).toBeVisible();
    // Settled, so the height is the row's own and not a frame of the transition.
    await expect.poll(() => height(row)).toBe(LOGO_ROW_HEIGHT);

    await column.evaluate((el) => {
      el.scrollTop = 400;
    });

    await expect.poll(() => height(row)).toBe(0);
    // And it is gone for a screen reader as well, rather than a zero-height image still being
    // announced. Which is also why the row is measured by its data attribute rather than found through
    // the image: once collapsed, the image has no role to be located by.
    await expect(byRole(page, t("logo.projectforge"))).toHaveCount(0);

    // What the collapse is for: the row goes, these stay. A sticky header that scrolled away with it
    // would make the whole trade a loss.
    await expect(page.locator("thead th").first()).toBeInViewport();
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();

    await scrollToTop(column);
    await expect.poll(() => height(row)).toBe(LOGO_ROW_HEIGHT);
    await expect(byRole(page, t("logo.projectforge"))).toBeVisible();
  });
});

/**
 * The login page carries the row too, and there it never collapses: the page does not scroll, so there
 * is nothing for it to get out of the way of — and arriving from a scrolled list must not leave it
 * hidden either.
 *
 * `base` rather than the logged-in fixture: this page is the one before a session exists.
 */
base("the logo row stays on the login page", async ({ page }) => {
  await goto(page, "/login");
  await waitForHydration(page);
  // No session, so no user language to derive from it: the label reads the same in every catalog, but
  // it is still looked up rather than spelled out.
  await expect(
    byRole(page, translate(locales()[0])("logo.projectforge"))
  ).toBeVisible();
  expect(await height(logoRow(page))).toBe(LOGO_ROW_HEIGHT);
});

/** The ProjectForge wordmark, as the accessibility tree exposes it. */
function byRole(page: Page, name: string): Locator {
  return page.getByRole("img", { name });
}

/**
 * The row itself, by the attribute it publishes its state through — not through the image, which is
 * aria-hidden once the row is collapsed and so has no role left to be found by.
 */
function logoRow(page: Page): Locator {
  return page.locator("[data-collapsed]");
}

/** The scroll column of a list: the table's own scrollable parent (see DataTable). */
function tableColumn(page: Page): Locator {
  return page.locator("table").locator("..");
}

async function scrollToTop(column: Locator): Promise<void> {
  await column.evaluate((el) => {
    el.scrollTop = 0;
  });
}

async function height(locator: Locator): Promise<number> {
  return locator.evaluate((el) =>
    Math.round(el.getBoundingClientRect().height)
  );
}
