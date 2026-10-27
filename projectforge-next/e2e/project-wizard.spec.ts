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

  test("all steps are on one page, and Finish tells a missing parent element at its step", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    const finish = page.getByRole("button", {
      name: format.t("project.wizard.finish._"),
    });
    await expect(finish).toBeEnabled({ timeout: 20_000 });
    // Numbered „1. Kunde“ etc. (WizardStepCard), matched whole: „Kunde“ is part of other headings too.
    for (const heading of [
      format.t("fibu.kunde._"),
      format.t("fibu.projekt._"),
      format.t("project.wizard.parentTask.heading"),
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
    // The tab bar of the edit pages, a tab per step, the three groups under one.
    for (const tab of [
      format.t("fibu.kunde._"),
      format.t("fibu.projekt._"),
      format.t("task._"),
      format.t("group.groups"),
      format.t("task.wizard.action._"),
    ]) {
      await expect(
        page.getByRole("tab", { name: tab, exact: true })
      ).toBeVisible();
    }
    // The action step tells at once what is missing, before Finish is pressed.
    await expect(
      page.getByRole("listitem").filter({
        hasText: format
          .t("project.wizard.action.projectNotCreated", { arg0: "" })
          .trim(),
      })
    ).toBeVisible({ timeout: 20_000 });
    // Nothing is filled in, so nothing is created: the server names the missing element at step 3.
    // The answer is held back a little, so the overlay of the running request can be seen.
    await page.route("**/rs/projectWizard/execute", async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 1_500));
      await route.continue();
    });
    await finish.click();
    const running = page
      .getByRole("status")
      .filter({ hasText: format.t("project.wizard.running._") });
    await expect(running).toBeVisible();
    await expect(running).toBeHidden({ timeout: 20_000 });
    await expect(
      page
        .getByRole("alert")
        .filter({ hasText: format.t("task.parentTask") })
        .and(page.locator("p"))
    ).toBeVisible({ timeout: 20_000 });
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

  test("the elements of customer and project may be existing ones, which Finish asks for", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    await page
      .getByRole("radio", { name: format.t("project.wizard.new.customer") })
      .click();
    // One choice for the customer's element, one for the project's, both new by default.
    const existing = page.getByRole("radio", {
      name: format.t("project.wizard.element.existing"),
    });
    await expect(existing).toHaveCount(2, { timeout: 20_000 });
    await expect(
      page.getByRole("radio", { name: format.t("project.wizard.element.new") })
    ).toHaveCount(2);
    // An existing element for the project, but none picked: the server names it at its choice.
    await page
      .getByRole("radiogroup", {
        name: format.t("project.wizard.result.projectElement"),
      })
      .getByRole("radio", { name: format.t("project.wizard.element.existing") })
      .click();
    await page
      .getByRole("button", { name: format.t("project.wizard.finish._") })
      .click();
    await expect(
      page
        .getByRole("alert")
        .filter({ hasText: format.t("project.wizard.result.projectElement") })
        .and(page.locator("p"))
    ).toBeVisible({ timeout: 20_000 });
  });
});

function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
