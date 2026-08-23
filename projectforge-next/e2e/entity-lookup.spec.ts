import type { Locator } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { LOOKUP_PAGE_SIZE } from "../lib/rs/autocomplete-url";

/**
 * An entity picker offers what there is: entries the moment it opens, without a term, and more of
 * them while the user scrolls (see useEntityLookup).
 *
 * The books list's "geändert durch" is the picker under test — an OBJECT filter against
 * `user/autosearch`, which has several hundred entries and therefore a second page. Worth an e2e test
 * because both halves are wiring: the lookup has to fire for an *empty* term, and the next page is
 * asked for by cmdk's own list scrolling, neither of which exists outside a browser.
 *
 * Reads only. The filter is stored per user and per entity, so it is reset around the test — nothing
 * is saved here, but a pill left open must not reach the other books specs either.
 */
test.describe("entity lookup", () => {
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

  test("shows entries on opening and loads more on scrolling", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");

    // The user picker of the grouped history filter (see history-filter.spec.ts for its path).
    await page.getByRole("button", { name: t("filter.addField") }).click();
    await page
      .getByRole("option", { name: t("filter.history"), exact: true })
      .click();
    await page.getByRole("combobox", { name: t("modifiedBy") }).click();

    const popover = page.locator('[data-slot="popover-content"]');
    const list = popover.getByRole("listbox");
    const entries = list.getByRole("option");

    // Nothing typed: this is the whole point — the box used to stay empty until two characters.
    await expect.poll(() => entries.count()).toBeGreaterThan(0);
    const first = await entries.count();
    // …and only the first page of them, not every user the installation has.
    expect(first).toBeLessThanOrEqual(LOOKUP_PAGE_SIZE);

    // Scrolling to the end asks for the next page. Skipped when this installation has fewer users
    // than one page, where there is nothing left to load.
    test.skip(
      first < LOOKUP_PAGE_SIZE,
      "this installation has less than one page of users"
    );
    await scrollToBottom(list);
    await expect.poll(() => entries.count()).toBeGreaterThan(first);

    // The term still narrows the list, and does so from the first page again.
    await popover.getByPlaceholder(t("filter.search")).fill("zzzzz");
    await expect.poll(() => entries.count()).toBe(0);
  });
});

/** cmdk's list is the scroll container itself, so the scroll event comes from it. */
async function scrollToBottom(list: Locator): Promise<void> {
  await list.evaluate((el) => {
    el.scrollTop = el.scrollHeight;
  });
}
