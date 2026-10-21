import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat } from "./fixtures/format";
import { writeHeaders } from "./fixtures/seed";
import { MERLIN_TEMPLATE_METADATA } from "../lib/metadata/merlin-template.generated";
import { MERLIN_PAGE } from "../components/features/merlin/merlin.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The Word® templates against the live backend (see MERLIN_PAGE, MerlinEntityRest and MerlinExecuteRest).
 *
 * The execution tests create the example letter of the list (`merlin/example/letter`, a template of the
 * logged-in user with its Word® template) and delete it afterwards. Who may read, execute and change a
 * template is the backend's (MerlinEntityRestTest).
 */
test.describe("merlin", { tag: "@parallel" }, () => {
  test("shows the declared columns under the labels of MerlinTemplateDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/merlin");

    await expect(
      page.getByRole("heading", { name: format.t(MERLIN_PAGE.titleKey) })
    ).toBeVisible();
    for (const column of MERLIN_PAGE.columns) {
      if (column.hiddenByDefault) continue;
      const key = columnHeaderKeyOf(column, MERLIN_TEMPLATE_METADATA);
      await expect(
        page.getByRole("columnheader", {
          name: new RegExp(`^${escape(label(format, key))}`),
        }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("executes the example letter, a missing required value is shown at its field", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await createLetter(page.request);
    try {
      await goto(page, `/merlin/${id}`);
      const receiver = field(page, "Receiver").getByRole("textbox");
      await expect(receiver).toBeVisible();
      await receiver.fill("");
      await executeButton(page, format.t).click();
      await expect(field(page, "Receiver")).toContainText(
        format.t("validation.error.fieldRequired", { arg0: "Receiver" })
      );

      await field(page, "Gender").getByRole("combobox").click();
      await page.getByRole("option", { name: "female", exact: true }).click();
      await field(page, "Date")
        .getByRole("textbox")
        .fill(format.date("2026-10-22"));
      await receiver.fill("Jane Doe");
      await field(page, "Receiver_Address")
        .getByRole("textbox")
        .fill("Street 1");
      await field(page, "Receiver_City").getByRole("textbox").fill("Town");
      const download = page.waitForEvent("download");
      await executeButton(page, format.t).click();
      expect((await download).suggestedFilename()).toMatch(/\.docx$/);
    } finally {
      await deleteTemplate(page.request, id);
    }
  });

  test("the form lists the variables of the Word® template and edits one in the dialog", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const id = await createLetter(page.request);
    try {
      await goto(page, `/merlin/${id}/edit`);
      await expect(
        page.getByLabel(format.t("plugins.merlin.wordTemplateFile._"), {
          exact: true,
        })
      ).toHaveText("LetterTemplate.docx");
      await page.getByRole("row", { name: /Receiver_City/ }).click();
      const dialog = page.getByRole("dialog");
      await expect(dialog).toContainText(
        format.t("plugins.merlin.variable.edit")
      );
      await dialog
        .getByRole("textbox", {
          name: format.t("plugins.merlin.variable.sortName._"),
        })
        .fill("zz-city");

      const update = page.waitForResponse(/\/rs\/merlin\/variable\/update$/);
      await dialog.getByRole("button", { name: format.t("update") }).click();
      expect((await update).ok()).toBe(true);
      await expect(dialog).toBeHidden();
      await expect(page.getByRole("row", { name: /zz-city/ })).toBeVisible();
    } finally {
      await deleteTemplate(page.request, id);
    }
  });

  test("redirects the old React pages", async ({ loggedInPage: page }) => {
    // OrphanedLinkFilter lives in Spring; the Next dev server does not serve /react at all.
    const port = new URL(test.info().project.use.baseURL ?? "").port;
    test.skip(
      process.env.E2E_DEV_SERVER === "1" || port === "3000",
      "needs Spring"
    );
    await page.goto("/react/merlin");
    await expect(page).toHaveURL(/\/next\/merlin$/);
    await page.goto("/react/merlinexecution/dynamic/42");
    await expect(page).toHaveURL(/\/next\/merlin\/42$/);
  });
});

function field(page: Page, variable: string) {
  return page.locator(`[data-field="inputVariables.${variable}"]`);
}

function executeButton(page: Page, t: (key: string) => string) {
  return page.getByRole("button", {
    name: t("plugins.merlin.templateExecutor.execute"),
    exact: true,
  });
}

async function createLetter(request: APIRequestContext): Promise<number> {
  const response = await request.post("/rs/merlin/example/letter", {
    headers: await writeHeaders(request),
  });
  expect(response.ok(), "the example letter is created").toBe(true);
  return ((await response.json()) as { id: number }).id;
}

/** Removes the example again — for good, as the form's delete does: a template has no history. */
async function deleteTemplate(request: APIRequestContext, id: number) {
  const headers = await writeHeaders(request);
  const stored = await request.get(`/rs/merlin/${id}`, { headers });
  if (!stored.ok()) return;
  await request.delete("/rs/merlin/delete", {
    headers,
    data: { data: await stored.json() },
  });
}

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
