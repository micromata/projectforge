import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import { formatDateInput } from "../lib/date-parse";
import { VISITORBOOK_METADATA } from "../lib/metadata/visitorbook.generated";
import { VISITORBOOK_PAGE } from "../components/features/visitorbook/visitorbook.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import { todayOf } from "../lib/user-zone";
import type { Page } from "@playwright/test";
import { writeHeaders } from "./fixtures/seed";

/**
 * The visitor book against the live backend (see VISITORBOOK_PAGE and VisitorbookEntityRest).
 *
 * Read-only: the database may be a copy of production, so nothing is created. The save of a visitor and the
 * writes of a visit are intercepted — what is asserted is what the form and the dialog post and how the dialog
 * shows the backend's answer, not the backend's rules (VisitorbookEntityRestTest).
 */
test.describe("visitorbook", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .post("/rs/visitorbook/filter/reset", {
        headers: await writeHeaders(page.request),
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under their labels", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/visitorbook");

    await expect(
      page.getByRole("heading", { name: format.t(VISITORBOOK_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of VISITORBOOK_PAGE.columns) {
      if ("hiddenByDefault" in column && column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, VISITORBOOK_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: startsWith(label(format, key)),
        }),
        `column ${columnIdOf(column)}`
        // The first list call of a session pays for the route being compiled and the caches being filled.
      ).toHaveCount(1, { timeout: 20_000 });
    }
  });

  test("posts a new visitor on save", async ({ loggedInPage: page }) => {
    const format = await userFormat(page);
    const posted: unknown[] = [];
    await page.route(
      "**/rs/visitorbook/{insert,update,saveorupdate}*",
      (route) => {
        posted.push(route.request().postDataJSON());
        return route.abort();
      }
    );

    await goto(page, "/visitorbook/new");
    await page
      .getByLabel(label(format, "orga.visitorbook.firstname"))
      .first()
      .fill("e2e");
    await page
      .getByLabel(label(format, "orga.visitorbook.lastname"))
      .first()
      .fill("Visitor");
    await page
      .getByRole("combobox", {
        name: label(format, "orga.visitorbook.visitortype"),
      })
      .click();
    await page
      .getByRole("option", {
        name: format.t("orga.visitorbook.visitortype.supplier"),
      })
      .click();
    await page.getByRole("button", { name: format.t("create") }).click();

    await expect
      .poll(() => posted.length, { message: "the save reaches the backend" })
      .toBeGreaterThan(0);
    expect(posted[0]).toMatchObject({
      data: { firstname: "e2e", lastname: "Visitor", visitortype: "SUPPLIER" },
    });
  });

  test("proposes today and now for a new visit and shows the backend's answer", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const id = await openFirstVisitor(page);

    const posted: unknown[] = [];
    await page.route("**/rs/visitorbook/entry/insert*", async (route) => {
      posted.push(route.request().postDataJSON());
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          entries: [
            {
              id: 999_999_999,
              dateOfVisit: "2000-01-03",
              arrived: "09:00",
              departed: "17:00",
              comment: "e2e visit",
              visitorbookId: id,
            },
          ],
        }),
      });
    });

    await page.getByRole("button", { name: t("add"), exact: true }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(
      label(format, "orga.visitorbook.timeofvisit")
    );
    await expect(
      dialog.getByLabel(label(format, "calendar.day"), { exact: true })
    ).toHaveValue(formatDateInput(todayOf(format.context), format.context));
    await expect(
      dialog.getByLabel(t("orga.visitorbook.timeofvisit.arrive"), {
        exact: true,
      })
    ).toHaveValue(/^\d{2}:\d{2}$/);
    await dialog.getByLabel(t("comment"), { exact: true }).fill("e2e visit");
    await dialog.getByRole("button", { name: t("save") }).click();

    await expect(dialog).toHaveCount(0);
    expect(posted[0]).toMatchObject({
      data: {
        dateOfVisit: todayOf(format.context),
        comment: "e2e visit",
        visitorbookId: id,
      },
    });
    // The section shows what the backend answered; a visit is no validity period, none is marked as current.
    const row = page.locator("tr", { hasText: "e2e visit" });
    await expect(row).toBeVisible();
    await expect(row).not.toHaveAttribute("data-validity", /.*/);
  });

  test("redirects the old React pages", async ({ loggedInPage: page }) => {
    // The redirects are Spring's (OrphanedLinkFilter); the Next dev server doesn't proxy /react.
    test.skip(
      process.env.E2E_DEV_SERVER === "1",
      "needs Spring, not the dev server"
    );
    await page.goto("/react/visitorbook");
    await expect(page).toHaveURL(/\/next\/visitorbook$/);
    const edit = await page.request.get("/react/visitorbook/edit/1", {
      maxRedirects: 0,
    });
    expect(edit.status()).toBeGreaterThanOrEqual(300);
    expect(edit.status()).toBeLessThan(400);
    expect(edit.headers()["location"]).toMatch(/\/next\/visitorbook\/1$/);
  });
});

/**
 * Opens the form of the first visitor of the list and answers its id. Skips when the list is empty (a fresh slot
 * database has no visitors) or the user may not write the visitor book.
 */
async function openFirstVisitor(page: Page): Promise<number> {
  await goto(page, "/visitorbook");
  await expect(page.locator("table")).toBeVisible({ timeout: 20_000 });
  const rows = listRows(page);
  const count = await rows
    .first()
    .waitFor({ state: "visible", timeout: 20_000 })
    .then(() => 1)
    .catch(() => 0);
  test.skip(count === 0, "no visitor in this database");
  await rows.first().click();
  await expect(page).toHaveURL(/\/visitorbook\/\d+$/, { timeout: 20_000 });
  const id = Number(page.url().match(/\/visitorbook\/(\d+)$/)?.[1]);
  const visitor = (await (
    await page.request.get(`/rs/visitorbook/${id}`, {
      headers: { "X-PF-Frontend": "next" },
    })
  ).json()) as { writeAccess?: boolean | null };
  test.skip(
    visitor.writeAccess === false,
    "no write access to the visitor book"
  );
  const { t } = await userFormat(page);
  await expect(
    page.getByText(t("orga.visitorbook.visits"), { exact: true }).first()
  ).toBeVisible({ timeout: 20_000 });
  return id;
}

function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(\\s|$)`);
}
