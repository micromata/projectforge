import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The escape hatch back to the legacy page (see LegacyPageLink).
 *
 * The url is the server's (`UILayout.legacyUrl` / `ListMetaData.legacyListPage` /
 * `legacyEditPage`, all from `NextMigration`), so what is checked here is that it arrives and is
 * rendered as a real link out of this app - not a client-side route, which would never load the
 * other frontend.
 *
 * Read-only: the link is inspected, never followed. Leaving the Next dev server for `/react/...`
 * would land on its 404, since only Spring serves that app.
 */
test.describe("legacy page link", () => {
  test("is absent for a page whose legacy counterpart is gone", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    const { t } = await userFormat(page);
    // book is fully migrated: BookEntityRest extends AbstractDTOEntityRest and serves no layout, so
    // its React page no longer exists and NextMigration answers with no legacy url at all.
    await goto(page, "/book");
    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveCount(0);

    await goto(page, `/book/${seededBook.id}`);
    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveCount(0);
  });

  test("leads to Wicket for a page the React migration never reached", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    // cost1 came straight from Wicket, whose mount points differ from React's: `<category>List`, and
    // an id as a query parameter (see NextMigration.LegacyApp). The add url is therefore not the edit
    // url with the id dropped, which is why the server sends it as its own field.
    // The link carries the `?legacyEscape` marker the backend appends (NextMigration): it is what the
    // orphaned-link redirect filter reads to let the way back through instead of bouncing it to Next.
    await goto(page, "/cost1");
    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveAttribute("href", "/wa/cost1List?legacyEscape");

    await goto(page, "/cost1/new");
    await expect(
      page.getByRole("link", { name: t("goreact.menu.classics") })
    ).toHaveAttribute("href", "/wa/cost1Edit?legacyEscape");
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
    ).toHaveAttribute("href", "/react/vacation?legacyEscape");
  });
});
