import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The gear menu of the book list against the live backend.
 *
 * Read-only: the menu is opened and its entries are checked, but none of the actions is triggered —
 * re-indexing affects the whole system and resetting the filter would destroy the account's stored
 * one. What the test guards is that the entries exist and carry their texts from the backend bundle
 * (see ListGearMenu, which declares them in the frontend instead of reading UILayout.pageMenu).
 */
test.describe("book list gear menu", () => {
  test("offers the maintenance actions of the list", async ({
    loggedInPage: page,
  }) => {
    // Every text through the catalogs the menu itself reads, never spelled out: an assertion on
    // "Suchindex reindizieren" passes only for a German account (see projectforge-next/CLAUDE.md).
    const { t } = await userFormat(page);
    await goto(page, "/book");

    await page
      .getByRole("button", { name: t("settings"), exact: true })
      .click();

    const menu = page.getByRole("menu");
    const reindexNewest = menu.getByRole("menuitem", {
      name: t("menu.reindexNewestDatabaseEntries._"),
    });
    await expect(reindexNewest).toBeVisible();
    await expect(
      menu.getByRole("menuitem", { name: t("menu.resetFilter._") })
    ).toBeVisible();

    // Tooltips come from the same bundle keys the legacy gear menu used.
    await expect(reindexNewest).toHaveAttribute(
      "title",
      t("menu.reindexNewestDatabaseEntries.tooltip.content")
    );
  });

  test("shows the full reindex only to an admin", async ({
    loggedInPage: page,
  }) => {
    // The entry hangs off `userStatus.adminUser`, so the request tells the test what to expect —
    // whether the account running it is an admin isn't the test's business.
    const status = await page.request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const isAdmin = ((await status.json()) as { adminUser?: boolean })
      .adminUser;

    const { t } = await userFormat(page);
    await goto(page, "/book");
    await page
      .getByRole("button", { name: t("settings"), exact: true })
      .click();

    await expect(
      page.getByRole("menuitem", {
        name: t("menu.reindexAllDatabaseEntries._"),
      })
    ).toHaveCount(isAdmin ? 1 : 0);
  });
});
