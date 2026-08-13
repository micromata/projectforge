import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The escape hatch back to the legacy React page (see LegacyPageLink).
 *
 * The url is the server's (`UILayout.legacyUrl` / `InitialListData.legacyEditPage`, both from
 * `NextMigration`), so what is checked here is that it arrives and is rendered as a real link out of
 * this app - not a client-side route, which would never load the other frontend.
 *
 * Read-only: the link is inspected, never followed. Leaving the Next dev server for `/react/...`
 * would land on its 404, since only Spring serves that app.
 */
test.describe("legacy page link", () => {
  test("leads from the books list to /react/book", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");

    const link = page.getByRole("link", { name: t("goreact.menu.classics") });
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute("href", "/react/book");
  });

  test("leads from a book to its legacy edit page", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}`);

    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveAttribute("href", `/react/book/edit/${seededBook.id}`);
  });

  test("leads to Wicket for a page the React migration never reached", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // cost1 came straight from Wicket, whose mount points differ from React's: `<category>List`, and
    // an id as a query parameter (see NextMigration.LegacyApp). The add url is therefore not the edit
    // url with the id dropped, which is why the server sends it as its own field.
    await goto(page, "/cost1");
    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveAttribute("href", "/wa/cost1List");

    await goto(page, "/cost1/new");
    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveAttribute("href", "/wa/cost1Edit");
  });

  test("leads from a server-laid-out list to its own legacy page", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // Not migrated, so the link points at the page the user is effectively looking at already. It is
    // rendered all the same: the frontend doesn't decide which pages have a way back.
    await goto(page, "/vacation");

    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveAttribute("href", "/react/vacation");
  });
});
