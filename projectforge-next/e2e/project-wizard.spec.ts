import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { expectedGroupName } from "./fixtures/group-nomenclature";
import { waitForRows } from "./fixtures/list-table";
import { resetTreeState } from "./fixtures/task-tree";

/**
 * The project wizard: customer, project, structure element, groups and access rights in one go.
 *
 * What it writes is covered by `ProjectWizardServiceTest` (projectforge-business) and its checks by
 * `ProjectWizardRestTest` (projectforge-rest). The cases here are about the way through the page, and
 * none of them writes anything: a full run would leave a customer behind, which can't be deleted.
 *
 * The wizard is only offered to users who may create customers or projects and structure elements
 * (`ProjectWizardRest.init`); for a test account that may not, the cases are skipped.
 */
const PAGE = "/projectWizard";

test.describe("project wizard", { tag: "@lane-task" }, () => {
  test.beforeEach(async ({ loggedInPage: page, seedRequest }) => {
    const res = await seedRequest.get("/rs/projectWizard/init");
    const init = (await res.json().catch(() => null)) as {
      allowed?: boolean;
    } | null;
    test.skip(
      !res.ok() || init?.allowed !== true,
      "The test account may not create customers, projects and structure elements."
    );
    await resetTreeState(page);
  });

  test("the tree's button leads to the wizard", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/taskTree");
    await waitForRows(page);

    await page
      .getByRole("link", { name: format.t("project.wizard.pageTitle") })
      .click();

    await expect(page).toHaveURL(/\/projectWizard$/, { timeout: 20_000 });
    await expect(
      page.getByRole("heading", { name: format.t("project.wizard.pageTitle") })
    ).toBeVisible({ timeout: 20_000 });
  });

  test("all steps are on one page, and Finish waits for the parent element", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    const finish = page.getByRole("button", {
      name: format.t("project.wizard.finish._"),
    });
    await expect(finish).toBeDisabled({ timeout: 20_000 });
    // Numbered „1. Kunde“ etc. (WizardStepCard), matched whole: „Kunde“ is part of other headings too.
    // An existing customer is the default, which names the third step.
    for (const heading of [
      format.t("fibu.kunde._"),
      format.t("fibu.projekt._"),
      format.t("project.wizard.parentTask.existingCustomer._"),
      format.t("task.wizard.team._"),
      format.t("task.wizard.managerGroup._"),
      format.t("task.wizard.externalGroup._"),
    ]) {
      await expect(
        page.getByRole("heading", {
          name: new RegExp(`^\\d+\\. ${escape(heading)}$`),
        })
      ).toBeVisible();
    }
  });

  test("a new customer is entered with the fields of the customer form", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    await page
      .getByRole("radio", { name: format.t("project.wizard.new.customer") })
      .click();
    await expect(
      page.getByRole("textbox", { name: format.t("fibu.kunde.nummer") })
    ).toBeVisible({ timeout: 20_000 });

    // The suggested group names follow the project's name as it is typed, shaped by the configured
    // nomenclature (suffix, letter case) if there is one. No group is preselected, so a new one is picked.
    await page
      .getByRole("radio", { name: format.t("project.wizard.new.group") })
      .first()
      .click();
    const name = `E2E wizard ${Date.now()}`;
    await page
      .getByRole("textbox", {
        name: format.t("fibu.projekt.name"),
        exact: true,
      })
      .fill(name);
    await expect(
      page.getByRole("textbox", {
        name: `${format.t("name")}: ${format.t("task.wizard.team._")}`,
      })
    ).toHaveValue(
      await expectedGroupName(page, "team", { project: { name } }),
      {
        timeout: 20_000,
      }
    );
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
