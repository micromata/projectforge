import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { TO_DO_METADATA } from "../lib/metadata/to-do.generated";
import { TO_DO_PAGE } from "../components/features/todo/todo.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import {
  fetchEntity,
  insert,
  MARKER,
  markAsDeleted,
  uniqueSuffix,
  writeHeaders,
} from "./fixtures/seed";

/**
 * The to-do page of the todo plugin against the live backend (see TO_DO_PAGE and ToDoEntityRest).
 *
 * The database is a copy of production: the column and validation tests write nothing (their save is
 * intercepted), the close/template test works on a to-do and a template of its own, assigned to the
 * test user (whose address is a dead end, so the notification goes nowhere), and removes both again.
 */
test.describe("todo", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The plugin may be deactivated in this instance: its REST endpoints are there all the same, but
    // without its registered right every write fails ("Cannot find UserRightId: PLUGIN_TODO"). Only an
    // activated plugin registers its menu entry.
    const menu = await page.request.get("/rs/menu", {
      headers: { "X-PF-Frontend": "next" },
    });
    test.skip(
      !/next\/todo["?]/.test(await menu.text()),
      "the todo plugin isn't activated in this instance"
    );
    await page.request
      .get("/rs/todo/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of ToDoDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    await goto(page, "/todo");

    await expect(
      page.getByRole("heading", { name: t(TO_DO_PAGE.titleKey) })
    ).toBeVisible();
    // Through `label`: a key with children of its own (`plugins.todo.status`) holds its text under `_`.
    for (const column of TO_DO_PAGE.columns) {
      const key = columnHeaderKeyOf(column, TO_DO_METADATA);
      await expect(
        page.getByRole("columnheader", { name: label(format, key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("presets the reporter and refuses to save without subject and assignee", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    let saveAttempted = false;
    await page.route("**/rs/todo/saveorupdate*", (route) => {
      saveAttempted = true;
      return route.abort();
    });

    await goto(page, "/todo/new");
    // The templates bar is there on a new to-do, the close button isn't: there is nothing to close yet.
    await expect(
      page.getByRole("button", { name: t("plugins.todo.templates") })
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: t("plugins.todo.button.close") })
    ).toHaveCount(0);
    await page.getByRole("button", { name: t("save") }).click();

    for (const key of ["plugins.todo.subject", "plugins.todo.assignee"]) {
      await expect(
        page.getByText(t("validation.error.fieldRequired", { arg0: t(key) }))
      ).toHaveCount(1);
    }
    expect(saveAttempted, "an incomplete to-do must not reach the server").toBe(
      false
    );
  });

  test("applies a template and closes a to-do with a comment", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const request = page.request;
    const status = await request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const userId = ((await status.json()) as { userData: { userId: number } })
      .userData.userId;
    const subject = `${MARKER} todo ${uniqueSuffix()}`;
    const templateName = `${MARKER} template ${uniqueSuffix()}`;
    const id = await insert(request, "todo", {
      subject,
      status: "OPENED",
      assignee: { id: userId },
      reporter: { id: userId },
      sendNotification: false,
    });
    try {
      // Save the to-do on screen as template, then apply it to a new one: it fills the subject.
      await goto(page, `/todo/${id}`);
      await page
        .getByRole("button", { name: t("plugins.todo.templates") })
        .click();
      await page.getByPlaceholder(t("favorite.addNew")).fill(templateName);
      await page.getByPlaceholder(t("favorite.addNew")).press("Enter");
      await expect(page.getByText(templateName)).toBeVisible();
      await page.keyboard.press("Escape");

      await goto(page, "/todo/new");
      await page
        .getByRole("button", { name: t("plugins.todo.templates") })
        .click();
      await page.getByText(templateName).click();
      await expect(page.getByLabel(t("plugins.todo.subject"))).toHaveValue(
        subject
      );

      // Close the to-do through the dialog, which edits its comment.
      await goto(page, `/todo/${id}`);
      await page
        .getByRole("button", { name: t("plugins.todo.button.close") })
        .click();
      const dialog = page.getByRole("dialog");
      await dialog.getByLabel(t("comment")).fill(`${MARKER} closed`);
      await dialog
        .getByRole("button", { name: t("plugins.todo.button.close") })
        .click();
      await expect
        .poll(
          async () =>
            (
              await fetchEntity<{ status?: string; comment?: string }>(
                request,
                "todo",
                id
              )
            ).status
        )
        .toBe("CLOSED");
      const closed = await fetchEntity<{ comment?: string }>(
        request,
        "todo",
        id
      );
      expect(closed.comment).toBe(`${MARKER} closed`);
    } finally {
      const list = await request.get("/rs/todo/templates/list", {
        headers: { "X-PF-Frontend": "next" },
      });
      const { templates } = (await list.json()) as {
        templates: { id: number; name: string }[];
      };
      for (const template of templates.filter((f) =>
        f.name.startsWith(MARKER)
      )) {
        await request.get(`/rs/todo/templates/delete?id=${template.id}`, {
          headers: await writeHeaders(request),
        });
      }
      await markAsDeleted(request, "todo", id);
    }
  });
});
