import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { isPluginActive } from "./fixtures/plugins";
import { listRows } from "./fixtures/list-table";
import { MEMO_METADATA } from "../lib/metadata/memo.generated";
import { MEMO_PAGE } from "../components/features/memo/memo.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The memos against the live backend (see MEMO_PAGE and MemoEntityRest).
 *
 * The database may be a copy of production: nothing is written. The save and the delete test intercept
 * their requests. That a memo is its owner's only is the backend's (MemoEntityRestTest).
 */
test.describe("memo", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    test.skip(
      !(await isPluginActive(page.request, "memo")),
      "the memo plugin is not active on this instance"
    );
  });

  test("shows the declared columns under the labels of MemoDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/memo");

    await expect(
      page.getByRole("heading", { name: format.t(MEMO_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of MEMO_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, MEMO_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("posts subject, text and owner on save", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let posted: Record<string, unknown> | undefined;
    await page.route("**/rs/memo/{insert,update}*", (route) => {
      posted = route.request().postDataJSON() as Record<string, unknown>;
      return route.abort();
    });

    await goto(page, "/memo/new");
    await page
      .getByRole("textbox", { name: label(format, "plugins.memo.subject") })
      .fill("e2e memo");
    await page
      .getByRole("textbox", { name: label(format, "plugins.memo.memo") })
      .fill("first line\nsecond line");
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => posted, { message: "the save reaches the backend" })
      .toBeDefined();
    const body = (posted?.data ?? posted) as Record<string, unknown>;
    expect(body.subject).toBe("e2e memo");
    expect(body.memo).toBe("first line\nsecond line");
    // The logged-in user, preset by the backend (MemoEntityRest.newBaseDO).
    expect((body.owner as { id?: number } | undefined)?.id).toBeDefined();
  });

  test("deletes a memo for good, it has no history", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/memo");
    const firstRow = listRows(page).first();
    const hasRows = await firstRow
      .waitFor({ timeout: 30_000 })
      .then(() => true)
      .catch(() => false);
    test.skip(!hasRows, "The logged-in user has no memos.");

    await firstRow.click();
    await expect(page).toHaveURL(/\/memo\/\d+$/);
    await page.route("**/rs/memo/{delete,markAsDeleted}", (route) =>
      route.abort()
    );
    const deleted = page.waitForRequest(
      (request) =>
        /\/rs\/memo\/(delete|markAsDeleted)$/.test(request.url()) &&
        request.method() === "DELETE"
    );
    await page
      .getByRole("button", { name: format.t("markAsDeleted"), exact: true })
      .click();
    const dialog = page.getByRole("alertdialog");
    await expect(dialog).toContainText(format.t("question.deleteQuestion"));
    await dialog
      .getByRole("button", { name: format.t("markAsDeleted"), exact: true })
      .click();
    expect((await deleted).url()).toMatch(/\/rs\/memo\/delete$/);
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
