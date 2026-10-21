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
import { filterElements } from "./fixtures/filter-pill";

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

  test("offers the incomplete visits filter as a pinned toggle pill", async ({
    loggedInPage: page,
  }) => {
    // The label is the backend's (VisitorbookEntityRest.addMagicFilterElements), so it is asked for first.
    const element = (await filterElements(page, "visitorbook")).find(
      (e) => e.id === "incompleteVisits"
    );
    expect(element?.filterType).toBe("BOOLEAN");
    expect(element?.defaultFilter).toBe(true);
    const posted: unknown[] = [];
    page.on("request", (request) => {
      if (/\/rs\/visitorbook\/list/.test(request.url())) {
        posted.push(request.postDataJSON());
      }
    });

    await goto(page, "/visitorbook");
    const pill = page.getByRole("button", {
      name: element?.label ?? "",
      exact: true,
    });
    await expect(pill).toBeVisible({ timeout: 20_000 });
    await expect(pill).toHaveAttribute("aria-pressed", "false");
    await pill.click();
    await expect(pill).toHaveAttribute("aria-pressed", "true");
    await expect
      .poll(() => JSON.stringify(posted.at(-1) ?? null), {
        message: "the list is asked with the filter",
      })
      .toMatch(/"field":"incompleteVisits".*"value":"true"/);
    // Toggled back, so the stored filter is left as it was found.
    await pill.click();
    await expect(pill).toHaveAttribute("aria-pressed", "false");
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

  test("proposes today and now for a new visit and returns to the list after saving it", async ({
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
    // A saved visit is done: the list is shown again, for the next visitor.
    await expect(page).toHaveURL(/\/visitorbook$/, { timeout: 20_000 });
  });

  test("offers a stored visitor for a new one's name and opens its visit dialog", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await openFirstVisitor(page);
    const asked: string[] = [];
    await page.route("**/rs/visitorbook/existing*", (route) => {
      asked.push(route.request().url());
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            id,
            firstname: "e2e",
            lastname: "Existing",
            company: "e2e Company",
            lastDateOfVisit: "2000-01-03",
            numberOfVisits: 3,
          },
        ]),
      });
    });

    await goto(page, "/visitorbook/new");
    await page
      .getByLabel(label(format, "orga.visitorbook.lastname"))
      .first()
      .fill("Exi");
    const option = page.getByRole("option", { name: /Existing, e2e/ });
    await expect(option).toBeVisible({ timeout: 20_000 });
    expect(asked.at(-1)).toContain("lastname=Exi");
    await option.click();

    // The visitor opens with the dialog of a new visit; the parameter is dropped from the url.
    await expect(page).toHaveURL(new RegExp(`/visitorbook/${id}$`), {
      timeout: 20_000,
    });
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("heading")).toHaveText(
      label(format, "orga.visitorbook.timeofvisit")
    );
    await expect(
      dialog.getByLabel(label(format, "calendar.day"), { exact: true })
    ).toHaveValue(formatDateInput(todayOf(format.context), format.context));
    await page.keyboard.press("Escape");
    await expect(dialog).toHaveCount(0);
  });

  test("completes the company from the ones in use", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await page.route("**/rs/visitorbook/autocomplete*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(["e2e Company GmbH"]),
      })
    );

    await goto(page, "/visitorbook/new");
    const company = page
      .getByLabel(label(format, "orga.visitorbook.company"))
      .first();
    await company.fill("e2e");
    await page.getByRole("option", { name: "e2e Company GmbH" }).click();
    await expect(company).toHaveValue("e2e Company GmbH");
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
