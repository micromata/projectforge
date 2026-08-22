import type { APIRequestContext } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { waitForRows } from "./fixtures/list-table";
import { MARKER, uniqueSuffix } from "./fixtures/seed";
import { narrowToSeeded, resetTreeState } from "./fixtures/task-tree";

/**
 * The structure wizard — step 4b of projectforge-next/MIGRATION.md.
 *
 * What it does is Wicket's `TaskWizardPage.create`, now `TaskWizardService`: the picked element gets
 * the group's role recursively, every ancestor below the root read access on the tasks alone. The rules
 * themselves are covered by `TaskWizardServiceTest` in projectforge-business; the cases here are about
 * the way through the page — that the admin entry leads to it, that Finish stays shut without an
 * element, and that a full run actually writes the entry the announcement promises.
 *
 * The one writing case grants a *local* group of the tests' own (see createGroup) rights on the seeded
 * task, and marks the entry deleted again afterwards. The seeded task hangs below the root, so no
 * ancestor entry is written — the root is skipped — and the run leaves nothing on a task anyone else
 * uses.
 */
const PAGE = "/taskWizard";

/** The access entries of one group, read the way the access management list reads them. */
async function accessEntriesOf(
  request: APIRequestContext,
  groupId: number
): Promise<{ id: number; task?: { id?: number }; recursive?: boolean }[]> {
  const res = await request.get("/rs/access/initialList");
  if (!res.ok()) {
    throw new Error(`Could not read the access list: HTTP ${res.status()}`);
  }
  const body = (await res.json()) as {
    data?: {
      resultSet?: {
        id: number;
        group?: { id?: number };
        task?: { id?: number };
        recursive?: boolean;
        deleted?: boolean;
      }[];
    };
  };
  return (body.data?.resultSet ?? []).filter(
    (row) => row.group?.id === groupId && row.deleted !== true
  );
}

test.describe("task wizard", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await resetTreeState(page);
  });

  test("the gear menu of the tree leads to the wizard", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/taskTree");
    await waitForRows(page);

    // Admins only, as in Wicket (`TaskTreePage.init`) — the test account is one, so the entry is there.
    await page
      .getByRole("button", { name: format.t("settings"), exact: true })
      .click();
    await page
      .getByRole("menuitem", { name: format.t("task.wizard.pageTitle") })
      .click();

    await expect(page).toHaveURL(/\/taskWizard$/, { timeout: 20_000 });
    await expect(
      page.getByRole("heading", { name: format.t("task.wizard.pageTitle") })
    ).toBeVisible({ timeout: 20_000 });
  });

  test("without a structure element nothing can be finished", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    const finish = page.getByRole("button", {
      name: format.t("task.wizard.finish"),
    });
    await expect(finish).toBeDisabled({ timeout: 20_000 });
    await expect(
      page.getByText(format.t("task.wizard.action.noactionRequired"))
    ).toBeVisible();
    // All four steps are on the page at once; it is a form, not a sequence of screens.
    for (const key of ["team", "managerGroup", "externalGroup"]) {
      await expect(
        page.getByRole("heading", {
          name: new RegExp(escape(format.t(`task.wizard.${key}._`))),
        })
      ).toBeVisible();
    }
  });

  test("an element without a group announces that there is nothing to do", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(60_000);
    const format = await userFormat(page);
    await goto(page, PAGE);
    await pickSeededTask(page, format, seededTask.title);

    // The difference from Wicket, whose `actionRequired()` announces the rights as soon as an element is
    // picked — contradicting its own text, which says why there is nothing to do.
    await expect(
      page.getByText(format.t("task.wizard.action.noactionRequired"))
    ).toBeVisible();
    // Finish is open all the same: an element alone is a valid, if pointless, run — the backend then
    // writes nothing (TaskWizardService.grantAccess).
    await expect(
      page.getByRole("button", { name: format.t("task.wizard.finish") })
    ).toBeEnabled();
  });

  test("a group created in the wizard is the one the step goes on with", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(90_000);
    const format = await userFormat(page);
    await goto(page, PAGE);
    // For the suggested name, which is built from the picked element's title.
    await pickSeededTask(page, format, seededTask.title);

    // The team's step, the first of the three (see GROUP_STEPS).
    await page
      .getByRole("button", {
        name: format.t("task.wizard.button.createGroup._"),
      })
      .first()
      .click();
    const dialog = page.getByRole("dialog");
    const name = dialog.getByLabel(format.t("name"), { exact: true });
    // Wicket only *showed* this name to be copied by hand; here it is an actual prefill. The team's
    // group is named after the element itself, the other two get a suffix (see suggestGroupName).
    await expect(name).toHaveValue(seededTask.title, { timeout: 20_000 });

    const created = `${MARKER} group ${uniqueSuffix()}`;
    await name.fill(created);
    // Local, so no group of a test run reaches an LDAP the installation may be attached to — the same
    // reason `createGroup` sets it.
    // By role: the field's hint button repeats the label in its `aria-label` („Hinweis: …").
    await dialog
      .getByRole("checkbox", { name: format.t("group.localGroup._") })
      .check();
    // The group page's own save button: the dialog renders GROUP_PAGE's declared form now, not a
    // layout the backend laid out (see EntityEditDialog).
    await dialog
      .getByRole("button", { name: format.t("save"), exact: true })
      .click();
    await expect(dialog).toBeHidden({ timeout: 20_000 });

    // The point of creating it here: the new group is the value of the step it was created from, which
    // is what the wizard would go on with. Nothing is finished, so no rights are granted.
    await expect(
      page.getByRole("combobox", {
        name: `${format.t("group._")}: ${format.t("task.wizard.team._")}`,
      })
    ).toHaveText(new RegExp(escape(created)));
  });

  test("an element plus a group grants that group its rights", async ({
    loggedInPage: page,
    seededTask,
    seededGroup,
  }) => {
    test.setTimeout(90_000);
    const format = await userFormat(page);
    await goto(page, PAGE);
    await pickSeededTask(page, format, seededTask.title);

    // The team, whose step is the second one — `employee()` recursively on the element. Its picker is
    // named by role as well as by „Group", since all three carry the same visible label.
    await page
      .getByRole("combobox", {
        name: `${format.t("group._")}: ${format.t("task.wizard.team._")}`,
      })
      .click();
    await page
      .getByPlaceholder(format.t("filter.search"))
      .fill(seededGroup.name);
    await page
      .getByRole("option", { name: seededGroup.name })
      .click({ timeout: 20_000 });

    await expect(
      page.getByText(format.t("task.wizard.action.taskAndgroupsGiven"))
    ).toBeVisible();
    await page
      .getByRole("button", { name: format.t("task.wizard.finish") })
      .click();

    // Back to the tree the wizard was started from, with the rights set.
    await expect(page).toHaveURL(/\/taskTree$/, { timeout: 20_000 });

    const entries = await accessEntriesOf(page.request, seededGroup.id);
    const onTask = entries.filter((row) => row.task?.id === seededTask.id);
    expect(onTask).toHaveLength(1);
    expect(onTask[0].recursive).toBe(true);
    // No ancestor entry: the seeded task's parent is the root, and the root never gets one.
    expect(entries).toHaveLength(1);

    // Take the rights back: they are the only effect of this spec that outlives the run. `markAsDeleted`
    // and not `forceDelete` — a `GroupTaskAccessDO` is historizable, so the flag is all there is (see
    // AbstractEntityRest.markAsDeleted), and the whole row has to be posted back, not just its id.
    await revokeAccess(page.request, onTask[0].id);
    const left = await accessEntriesOf(page.request, seededGroup.id);
    expect(
      left,
      "The granted rights are still in effect after the run."
    ).toEqual([]);
  });
});

/** Opens the tree of step 1 and picks the seeded task in it. */
async function pickSeededTask(
  page: Parameters<typeof narrowToSeeded>[0],
  format: Awaited<ReturnType<typeof userFormat>>,
  title: string
) {
  await page
    .getByRole("button", {
      name: `${format.t("task.tree.title.select")} ${format.t("task._")}`,
    })
    .click();
  const { row } = await narrowToSeeded(page, format.t, title);
  await row.click();
  // The path of the picked element replaces "please select a task".
  await expect(page.getByText(title, { exact: false }).first()).toBeVisible({
    timeout: 20_000,
  });
}

/** Marks one access entry as deleted, so the spec grants nothing that outlives it. */
async function revokeAccess(
  request: APIRequestContext,
  id: number
): Promise<void> {
  const stored = await request.get(`/rs/access/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!stored.ok()) {
    throw new Error(
      `Could not read access entry ${id}: HTTP ${stored.status()}`
    );
  }
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  const removed = await request.delete("/rs/access/markAsDeleted", {
    headers: {
      "X-PF-Frontend": "next",
      "X-PF-CSRF-Token": csrfToken,
      "Content-Type": "application/json",
    },
    data: { data: await stored.json() },
  });
  if (!removed.ok()) {
    throw new Error(
      `Could not take the granted rights back (access entry ${id}): HTTP ${removed.status()}`
    );
  }
}

/** Escapes a message for use inside a `RegExp` — a label may carry brackets („(optional)"). */
function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
