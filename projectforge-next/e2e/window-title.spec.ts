import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

/**
 * The browser window is named after the page — `<page> – ProjectForge` — instead of every tab reading
 * "ProjectForge" (see useDocumentTitle). Read-only, against the test's own book (fixtures/seed.ts).
 */
test.describe("window title", { tag: "@parallel" }, () => {
  test("a list page carries its heading", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book");
    await expect(page).toHaveTitle(`${t("book.title.list")} – ProjectForge`);
  });

  test("an edit page carries the entry it edits", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    await goto(page, `/book/${seededBook.id}`);
    await expect(page).toHaveTitle(`${seededBook.title} – ProjectForge`);
  });

  test("a new entry carries the add heading", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/book/new");
    await expect(page).toHaveTitle(
      `${t("books.edit.newTitle")} – ProjectForge`
    );
  });

  // Its heading is set at once, unlike the pages behind the login: the one that Next's own metadata
  // <title> used to reset right after (see app/layout.tsx). Not logged in, so no user locale to
  // translate with; any title in front of the name will do.
  test("the login page carries its heading", async ({ page }) => {
    await goto(page, "/login");
    await expect(page).toHaveTitle(/^.+ – ProjectForge$/);
  });
});
