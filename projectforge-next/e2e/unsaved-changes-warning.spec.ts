import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import type { Page } from "@playwright/test";

/**
 * Leaving an edit form that holds unsaved changes asks first.
 *
 * The ways out are of two kinds, and both have to ask, which is what this spec is about: a link
 * (`GuardedLink` — the breadcrumb back to the list, a menu entry, a link into another entity) and a
 * `router.push` that is no link at all (the quick access palette). The bug this guards: only the three
 * links out of the form itself were guarded, so the whole menu took the user away without a word, and
 * the warning appeared to work only when the target was a legacy page — where the full page load makes
 * the *browser* ask (`beforeunload`), not this app.
 *
 * The question is the app's own dialog now, not `window.confirm` (see UnsavedChangesDialogHost /
 * ConfirmDialog, a Radix `role="alertdialog"`): its description is `question.leaveUnsavedChanges`, its
 * buttons `unsavedChanges.confirm` ("leave") and `unsavedChanges.stay` ("stay"). So the cases drive the
 * DOM dialog rather than a `page.on("dialog")` listener, which no longer fires.
 *
 * The form's own ways out ask nothing on purpose: cancel, save and clone are decisions the user just
 * made, and asking again would say the button hadn't been understood (see the round trip spec).
 *
 * Asserted on a *book*, whose form is cheapest to fill in; the guard sits in `EntityEditPage` and so
 * serves all four hand-built entities. Nothing is saved.
 */
test.describe("unsaved changes warning", () => {
  test("asks before a menu entry takes the user off the form", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, `/book/${seededBook.id}`);
    const title = page.getByRole("textbox", { name: /titel/i });
    await expect(title).toHaveValue(seededBook.title);
    await title.fill(`${seededBook.title} — unsaved`);

    // The breadcrumb back to the list, the shortest link out of the form. The click no longer blocks:
    // the app's own dialog appears in the DOM afterwards (see the file header).
    await page.getByRole("link", { name: t("book.title.list") }).click();
    const dialog = page.getByRole("alertdialog");
    await expect(dialog).toBeVisible();
    await expect(
      dialog,
      "leaving the form must be confirmed first"
    ).toContainText(t("question.leaveUnsavedChanges"));

    // Answer "stay" — the assertion is that the form is still there afterwards.
    await page.getByRole("button", { name: t("unsavedChanges.stay") }).click();
    await expect(dialog).toBeHidden();
    await expect(
      title,
      "a dismissed question must leave the form as it was"
    ).toHaveValue(`${seededBook.title} — unsaved`);
  });

  test("leaves once the question is accepted", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    // The counter-test: the question is a question, not a wall.
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}`);
    const title = page.getByRole("textbox", { name: /titel/i });
    await expect(title).toHaveValue(seededBook.title);
    await title.fill(`${seededBook.title} — unsaved`);

    await page.getByRole("link", { name: t("book.title.list") }).click();
    await expect(page.getByRole("alertdialog")).toBeVisible();
    await page
      .getByRole("button", { name: t("unsavedChanges.confirm") })
      .click();
    await expect(
      page.getByRole("heading", { name: t("book.title.list") })
    ).toBeVisible();
  });

  test("says nothing when the form was not touched", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    // Without this the warning would be noise, and noise is what gets clicked away unread.
    const { t } = await userFormat(page);
    await goto(page, `/book/${seededBook.id}`);
    await expect(page.getByRole("textbox", { name: /titel/i })).toHaveValue(
      seededBook.title
    );

    await page.getByRole("link", { name: t("book.title.list") }).click();
    // The heading first, so navigation has settled before the absence is asserted.
    await expect(
      page.getByRole("heading", { name: t("book.title.list") })
    ).toBeVisible();
    await expect(
      page.getByRole("alertdialog"),
      "an untouched form must be left without a word"
    ).toHaveCount(0);
  });

  test("asks before the quick access palette navigates away", async ({
    loggedInPage: page,
    seededBook,
  }) => {
    // Not a link: the palette calls `router.push`, which no `onNavigate` sees — so the guard has to be
    // in its own handler (see quick-access-results.tsx).
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, `/book/${seededBook.id}`);
    const title = page.getByRole("textbox", { name: /titel/i });
    await expect(title).toHaveValue(seededBook.title);
    await title.fill(`${seededBook.title} — unsaved`);

    // An entry this app serves, taken from the user's own menu rather than named here — its title is
    // the server's and not the list heading's (see quick-access.spec.ts).
    const entry = await internalMenuEntry(page);
    // Retried, as quick-access.spec.ts does: a click before hydration lands on nothing.
    const search = page.getByPlaceholder(t("menu.quickAccess.placeholder"));
    await expect(async () => {
      await page.getByRole("button", { name: t("menu.quickAccess._") }).click();
      await expect(search).toBeVisible({ timeout: 1000 });
    }).toPass({ timeout: 30_000 });
    await search.fill(entry);
    const option = page.getByRole("option", { name: entry, exact: true });
    await expect(option.first()).toBeVisible();

    await option.first().click();
    const dialog = page.getByRole("alertdialog");
    await expect(dialog).toBeVisible();
    await expect(dialog, "the palette must ask as a link does").toContainText(
      t("question.leaveUnsavedChanges")
    );
    await page.getByRole("button", { name: t("unsavedChanges.stay") }).click();
    await expect(dialog).toBeHidden();
    await expect(title).toHaveValue(`${seededBook.title} — unsaved`);
  });
});

/**
 * The title of a menu entry this app serves, taken from the user's own menu.
 *
 * Not spelled out here: the server names the entries (`MenuRest`, translated and access-filtered), and
 * a `next/` url is what makes the palette navigate client-side — which is the case under test. Only the
 * title is needed, so this is a fraction of quick-access.spec.ts's own flattening.
 */
async function internalMenuEntry(page: Page): Promise<string> {
  const res = await page.request.get("/rs/menu", {
    headers: { "X-PF-Frontend": "next" },
  });
  const menu = (await res.json()) as {
    mainMenu?: { menuItems?: MenuItem[] };
  };
  const found = leaves(menu.mainMenu?.menuItems).find((item) =>
    item.url?.startsWith("next/")
  );
  if (!found) throw new Error("No main menu entry served by this app.");
  return found.title;
}

function leaves(items: MenuItem[] | undefined): MenuItem[] {
  return (items ?? []).flatMap((item) =>
    item.subMenu?.length ? leaves(item.subMenu) : [item]
  );
}

interface MenuItem {
  title: string;
  url?: string;
  subMenu?: MenuItem[];
}
