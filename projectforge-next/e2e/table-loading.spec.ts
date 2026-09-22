import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { Locator, Page } from "@playwright/test";

/**
 * A table says that the rows on screen are not the answer to the question just asked: the loading
 * overlay (see TableLoadingOverlay) appears while a new result set is on its way.
 *
 * Worth an e2e test because nothing smaller can see it. What is under test is the wiring between a
 * page's fetch and the table below it, and the two pages fetch in entirely different ways — the
 * hand-built list through its own query, a server-laid-out one through an action of the layout.
 * That is exactly where the overlay was missing: the same wait looked like two different things
 * depending on which page the user was on.
 *
 * Reads only — no entry is created or changed.
 */
const ROW = "tbody tr[data-row-id]";

/**
 * How long the answer is held back. Well above the overlay's own 300 ms delay plus its fade, so what
 * is asserted is the overlay of a noticeable wait and not a race with the animation.
 */
const HOLD_MS = 2500;

/** Delays every POST to the entity's endpoints, so the wait is long enough to be seen. */
async function holdBackPosts(page: Page, entity: string): Promise<void> {
  await page.route(`**/rs/${entity}/**`, async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    await new Promise((resolve) => setTimeout(resolve, HOLD_MS));
    await route.continue();
  });
}

/**
 * The overlay, fully faded in. Its opacity is asserted and not only its presence: it is in the DOM
 * from the first render of the fetch and made visible by a CSS animation 300 ms later, so a check on
 * presence alone would also pass for an overlay nobody ever sees.
 */
async function expectVisibleOverlay(overlay: Locator): Promise<void> {
  await expect(overlay).toBeVisible();
  await expect
    .poll(() => overlay.evaluate((el) => Number(getComputedStyle(el).opacity)))
    .toBeGreaterThan(0.5);
}

test.describe("table loading overlay", () => {
  test("the hand-built list shows it while a new result set is fetched", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/book");
    await page.locator(ROW).first().waitFor();

    await holdBackPosts(page, "book");
    // Sorting is the server's, so a click on a column header is a new list call.
    await page.getByRole("columnheader").nth(1).click();

    const overlay = page.getByRole("status").filter({
      hasText: format.t("loading"),
    });
    await expectVisibleOverlay(overlay);
    // And gone again once the rows are the answer.
    await expect(overlay).toHaveCount(0, { timeout: 2 * HOLD_MS });
  });

  test("a server-laid-out list page shows it while its search action runs", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/vacation");
    await page.locator(ROW).first().waitFor();

    await holdBackPosts(page, "vacation");
    // These pages fetch through the layout's actions, not through a query of the table's own.
    await page
      // `search._` is the bundle's plain "Suchen"; the button's own label is the server's, from the
      // same key (the generator nests a dotted key, see i18n/config.ts).
      .getByRole("button", { name: format.t("search._"), exact: true })
      .first()
      .click();

    const overlay = page.getByRole("status").filter({
      hasText: format.t("loading"),
    });
    await expectVisibleOverlay(overlay);
    await expect(overlay).toHaveCount(0, { timeout: 2 * HOLD_MS });
  });

  test("shows it over the skeleton of a first load, not only over rows", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // Held back before the page is opened, so what is under test is the very first list call - the
    // longest wait a list has, and the one a reload of the order book runs into.
    await holdBackPosts(page, "book");
    await goto(page, "/book");

    // The skeleton is up: rows without a `data-row-id`, since there is no row yet.
    await expect(page.locator("tbody tr").first()).toBeVisible();
    await expect(page.locator(ROW)).toHaveCount(0);

    const overlay = page.getByRole("status").filter({
      hasText: format.t("loading"),
    });
    await expectVisibleOverlay(overlay);
    await expect(page.locator(ROW).first()).toBeVisible({
      timeout: 2 * HOLD_MS,
    });
    await expect(overlay).toHaveCount(0);
  });
});
