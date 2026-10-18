import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import { formatDateInput } from "../lib/date-parse";
import { EMPLOYEE_METADATA } from "../lib/metadata/employee.generated";
import { EMPLOYEE_PAGE } from "../components/features/employee/employee.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { Page } from "@playwright/test";

/**
 * The employee page against the live backend (see EMPLOYEE_PAGE and EmployeeEntityRest).
 *
 * Read-only: the database is a copy of production and an employee needs a user of its own, so nothing is
 * created. The writes of the valid-since entries are intercepted and answered here — what is asserted is
 * how the dialog shows the backend's answers, not the backend's rules.
 */
test.describe("employee", { tag: "@parallel" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get("/rs/employee/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of EmployeeDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/employee");

    await expect(
      page.getByRole("heading", { name: format.t(EMPLOYEE_PAGE.titleKey) })
    ).toBeVisible();
    // Through `label`: a key with children of its own (`fibu.employee.status`) holds its text under `_`.
    for (const column of EMPLOYEE_PAGE.columns) {
      if ("hiddenByDefault" in column && column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, EMPLOYEE_METADATA);
      await expect(
        // The label at the start: "Name" is part of "Vorname" too, and the header's name goes on with its
        // buttons (filter, column width).
        page.getByRole("columnheader", {
          name: startsWith(label(format, key)),
        }),
        `column ${columnIdOf(column)}`
        // The first list call of a session pays for the route being compiled and the caches being filled.
      ).toHaveCount(1, { timeout: 20_000 });
    }
  });

  test("edits the valid-since entries of an employee in a dialog", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { t } = format;
    const id = await openFirstEmployee(page);

    const posted: unknown[] = [];
    let answer: "invalid" | "ok" = "invalid";
    await page.route("**/rs/employee/validSinceAttr/insert*", async (route) => {
      posted.push(route.request().postDataJSON());
      if (answer === "invalid") {
        await route.fulfill({
          status: 406,
          contentType: "application/json",
          body: JSON.stringify({
            validationErrors: [
              { fieldId: "value", message: "e2e: out of range" },
            ],
          }),
        });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          entries: [
            {
              id: 999_999_999,
              validSince: "2099-01-01",
              value: "30",
              comment: "e2e entry",
              type: "ANNUAL_LEAVE",
              employeeId: id,
            },
          ],
        }),
      });
    });

    // The annual leave section is the last of the three, so its add button is the last one.
    await page
      .getByRole("button", { name: t("add"), exact: true })
      .last()
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(
      t("fibu.employee.urlaubstage")
    );

    const date = dialog.getByLabel(t("attr.validSince"), { exact: true });
    await date.fill(formatDateInput("2099-01-01", format.context));
    await date.press("Tab");
    await dialog.getByLabel(t("days"), { exact: true }).pressSequentially("30");
    await dialog.getByRole("button", { name: t("save") }).click();

    // A field error of the backend stands at the field it names; the dialog stays open.
    await expect(dialog.getByText("e2e: out of range")).toBeVisible();
    expect(posted[0]).toMatchObject({
      data: {
        validSince: "2099-01-01",
        value: "30",
        type: "ANNUAL_LEAVE",
        employeeId: id,
      },
    });

    answer = "ok";
    await dialog.getByRole("button", { name: t("save") }).click();
    await expect(dialog).toHaveCount(0);
    // The section shows what the backend answered; an entry still ahead is marked as such.
    const row = page.locator('tr[data-validity="future"]', {
      hasText: "e2e entry",
    });
    await expect(row).toBeVisible();
  });

  test("redirects the old React pages", async ({ loggedInPage: page }) => {
    // The redirects are Spring's (OrphanedLinkFilter); the Next dev server doesn't proxy /react.
    test.skip(
      process.env.E2E_DEV_SERVER === "1",
      "needs Spring, not the dev server"
    );
    await page.goto("/react/employee");
    await expect(page).toHaveURL(/\/next\/employee$/);
    const edit = await page.request.get("/react/employee/edit/1", {
      maxRedirects: 0,
    });
    expect(edit.status()).toBeGreaterThanOrEqual(300);
    expect(edit.status()).toBeLessThan(400);
    expect(edit.headers()["location"]).toMatch(/\/next\/employee\/1$/);
  });
});

/**
 * Opens the form of the first employee of the list and answers its id. Skips when the list is empty: an
 * employee can't be created here (it needs a user of its own), so an empty list is the database's state.
 */
async function openFirstEmployee(page: Page): Promise<number> {
  await goto(page, "/employee");
  await expect(page.locator("table")).toBeVisible({ timeout: 20_000 });
  const rows = listRows(page);
  const count = await rows
    .first()
    .waitFor({ state: "visible", timeout: 20_000 })
    .then(() => 1)
    .catch(() => 0);
  test.skip(count === 0, "no employee in this database");
  await rows.first().click();
  await expect(page).toHaveURL(/\/employee\/\d+$/, { timeout: 20_000 });
  const id = Number(page.url().match(/\/employee\/(\d+)$/)?.[1]);
  // Without write access (no HR group) the sections offer no add button.
  const { t } = await userFormat(page);
  await expect(
    page.getByText(t("fibu.employee.urlaubstage"), { exact: true }).first()
  ).toBeVisible({ timeout: 20_000 });
  const addButtons = await page
    .getByRole("button", { name: t("add"), exact: true })
    .count();
  test.skip(addButtons !== 3, "no write access to the employee");
  return id;
}

function startsWith(text: string): RegExp {
  return new RegExp(`^${text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}(\\s|$)`);
}
