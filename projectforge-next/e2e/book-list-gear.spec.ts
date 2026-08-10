import { test, expect, goto } from "./fixtures/auth";

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
    await goto(page, "/book");

    await page.getByRole("button", { name: /einstellungen/i }).click();

    const menu = page.getByRole("menu");
    await expect(
      menu.getByRole("menuitem", { name: "Suchindex reindizieren" })
    ).toBeVisible();
    await expect(
      menu.getByRole("menuitem", { name: "Filter zurücksetzen" })
    ).toBeVisible();

    // Tooltips come from the same bundle keys the legacy gear menu used.
    await expect(
      menu.getByRole("menuitem", { name: "Suchindex reindizieren" })
    ).toHaveAttribute("title", /seit gestern angelegt oder modifiziert/);
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

    await goto(page, "/book");
    await page.getByRole("button", { name: /einstellungen/i }).click();

    await expect(
      page.getByRole("menuitem", { name: "Suchindex voll indizieren" })
    ).toHaveCount(isAdmin ? 1 : 0);
  });
});
