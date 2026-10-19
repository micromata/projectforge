import type { Locator } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import {
  EMPTY_TERM_MAX_ENTRIES,
  LOOKUP_PAGE_SIZE,
} from "../lib/rs/autocomplete-url";

/**
 * An entity picker lists a short list the moment it opens, but asks to type for a long one — its first
 * entries in alphabetical order are hardly ever the one wanted — and loads more of the matches of a
 * term while the user scrolls (see useEntityLookup).
 *
 * The books list's "geändert durch" is the picker under test — an OBJECT filter against
 * `user/autosearch`, which has several hundred entries and therefore a second page. Worth an e2e test
 * because both halves are wiring: the lookup has to fire for an *empty* term, and the next page is
 * asked for by cmdk's own list scrolling, neither of which exists outside a browser.
 *
 * Reads only. The filter is stored per user and per entity, so it is reset around the test — nothing
 * is saved here, but a pill left open must not reach the other books specs either.
 */
test.describe("entity lookup", { tag: "@lane-book" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test.afterAll(async ({ seedRequest }) => {
    // The logged-in context of the seeds: the plain `request` fixture has no session, so its reset was
    // refused and left the filter behind.
    await seedRequest
      .get("/rs/book/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("asks to type for a long list and loads more matches on scrolling", async ({
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

    // Nothing typed: a short list is shown whole, a long one not at all — the hint asks to type.
    const hint = popover.getByText(t("filter.typeToSearch"));
    await expect
      .poll(async () => (await hint.isVisible()) || (await entries.count()) > 0)
      .toBe(true);
    if (await hint.isVisible()) {
      expect(await entries.count()).toBe(0);
    } else {
      expect(await entries.count()).toBeLessThan(EMPTY_TERM_MAX_ENTRIES);
    }

    // A term lists its matches, the first page of them. The users are matched by the prefixes of their
    // words, so the term is taken from the short list where it is shown — a handful of seeded users need
    // not hold a word starting with "er", which a long list of names is full of.
    const term = (await hint.isVisible())
      ? "er"
      : (await entries.first().innerText()).trim().slice(0, 2);
    const search = popover.getByPlaceholder(t("filter.search"));
    await search.fill(term);
    await expect.poll(() => entries.count()).toBeGreaterThan(0);
    const first = await entries.count();
    expect(first).toBeLessThanOrEqual(LOOKUP_PAGE_SIZE);

    // Scrolling to the end asks for the next page. Skipped when this installation has fewer matches
    // than one page, where there is nothing left to load.
    test.skip(
      first < LOOKUP_PAGE_SIZE,
      "this installation has less than one page of matching users"
    );
    await scrollToBottom(list);
    await expect.poll(() => entries.count()).toBeGreaterThan(first);

    // The term still narrows the list, and does so from the first page again.
    await search.fill("zzzzz");
    await expect.poll(() => entries.count()).toBe(0);
  });
});

/** cmdk's list is the scroll container itself, so the scroll event comes from it. */
async function scrollToBottom(list: Locator): Promise<void> {
  await list.evaluate((el) => {
    el.scrollTop = el.scrollHeight;
  });
}
