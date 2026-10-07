import { test, expect, goto } from "./fixtures/auth";

/**
 * Opening an entry from a list creates its history entry within the click, and back returns to the
 * list (see navigateInGesture).
 *
 * The bug itself is Safari's and none of the engines here shows it: Safari 27 skips on back an entry
 * that `pushState` added outside a user gesture once it hands out no activation for the click. What
 * can be checked anywhere is the cause — that the entry is pushed while the click is being processed,
 * not when Next commits the navigation afterwards — and that doing so adds exactly one entry.
 *
 * Reads only — no entry is created or changed.
 */
const ROW = "tbody tr[data-row-id]";

test.describe("back navigation", { tag: "@lane-book" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
    // Records for every pushState whether a click was being dispatched at that moment.
    await page.addInitScript(() => {
      const w = window as unknown as { __pushes: boolean[] };
      w.__pushes = [];
      let inClick = false;
      addEventListener(
        "click",
        () => {
          inClick = true;
          setTimeout(() => (inClick = false), 0);
        },
        true
      );
      const original = History.prototype.pushState;
      History.prototype.pushState = function (...args) {
        w.__pushes.push(inClick);
        return original.apply(this, args);
      };
    });
  });

  test("a row click pushes the entry within the click, and back returns to the list", async ({
    loggedInPage: page,
  }) => {
    test.setTimeout(60_000);
    await goto(page, "/book");
    const row = page.locator(ROW).first();
    await expect(row).toBeVisible({ timeout: 30_000 });
    const listUrl = page.url();
    const before = await page.evaluate(() => {
      const w = window as unknown as { __pushes: boolean[] };
      w.__pushes = [];
      return history.length;
    });

    await row.click();
    await expect(page).toHaveURL(/\/book\/\d+/);
    const pushes = await page.evaluate(
      () => (window as unknown as { __pushes: boolean[] }).__pushes
    );
    expect(pushes).toEqual([true]);
    expect(await page.evaluate(() => history.length)).toBe(before + 1);

    await page.goBack();
    await expect(page).toHaveURL(listUrl);
    await expect(page.locator(ROW).first()).toBeVisible({ timeout: 30_000 });
  });
});
