import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRows } from "./fixtures/list-table";
import { fetchRootTaskId } from "./fixtures/seed";
import { narrowToSeeded, resetTreeState } from "./fixtures/task-tree";
import { LUCENE_QUERY_DOCS_URL } from "../lib/docs-links";

/**
 * The action bar of the structure tree page — step 3 of projectforge-next/MIGRATION.md.
 *
 * The inventory under test is Wicket's `TaskTreePage` content menu plus the reset button of its form:
 * add a task, add a *subtask* per row, re-index, reset the filter, and the handbook link beside the
 * search field. One entry of that menu is deliberately absent and therefore not asserted here — the
 * favourites (`UserPrefArea.TASK_FAVORITE`), still Wicket-only and reachable through the legacy link in
 * the header. The wizard's entry has its own spec (`e2e/task-wizard.spec.ts`).
 *
 * What each case is really about is a difference from every list page: the tree's filter is a
 * `TaskFilter` in the session and not the entity's stored `MagicFilter`, so "reset" happens in the
 * client and the endpoint the gear menu calls would not touch it; and "add" exists twice, because only
 * the per-row variant can name a parent — which travels as a parameter of the backend's preset rather
 * than being filled in here (see EditDef.newEntryParams).
 *
 * Read-only apart from the tree's own filter and the seeded task; no case creates a task, they only
 * follow the links that would.
 */
const PAGE = "/taskTree";

/** The gear menu's trigger — `exact`, since the column panel sits in the same row. */
function gear(page: Page, format: UserFormat) {
  return page.getByRole("button", { name: format.t("settings"), exact: true });
}

/** The status pill of the filter bar, whose text lists the flags that are on. */
function statusPill(page: Page, format: UserFormat, keys: string[]) {
  return page.getByRole("button", {
    name: keys.map((key) => format.t(key)).join(", "),
    exact: true,
  });
}

test.describe("task tree actions", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await resetTreeState(page);
  });

  test("the add button opens the new task form with the tree as its caller", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);
    await waitForRows(page);

    // The same button and the same shortcut every list page carries (see AddEntryButton), which is why
    // its accessible name is the generic one rather than "new task".
    await page
      .getByRole("link", { name: format.t("menu.addNewEntry") })
      .first()
      .click();

    // No parent: the form asks for one, as Wicket's `+` does — its page passes no
    // `PARAM_PARENT_TASK_ID` either, and a task without a parent is refused. `returnTo` is what brings a
    // cancel or a save back to the tree instead of to the task list, which has no page.
    await expect(page).toHaveURL(/\/task\/new\?returnTo=%2FtaskTree/, {
      timeout: 20_000,
    });
    await expect(
      page.getByRole("textbox", { name: label(format, "task.title") })
    ).toBeVisible({ timeout: 20_000 });
  });

  test("a row adds a subtask, and the backend presets its parent", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(60_000);
    const format = await userFormat(page);
    await goto(page, PAGE);
    const { row } = await narrowToSeeded(page, format.t, seededTask.title);

    // Behind the title, revealed on hover (`group-hover:opacity-100`), so the row's own click keeps
    // its meaning — the action is a link and stops the event itself (see TreeCell).
    await row
      .getByRole("link", {
        name: `${format.t("task.title.add")}: ${seededTask.title}`,
      })
      .click();

    await expect(page).toHaveURL(
      new RegExp(`/task/new\\?parentTaskId=${seededTask.id}&returnTo=`),
      { timeout: 20_000 }
    );

    // The parameter is not read into the form here: it is forwarded to `task/newEntry`, and what fills
    // the field is the parent the backend resolved (`TaskPagesRest.newBaseDO`). Which is why this
    // assertion is the interesting half of the row action — the url alone would prove nothing.
    await expect(
      page.getByRole("navigation", {
        name: format.t("task.path.pleaseSelectTask"),
      })
    ).toContainText(seededTask.title, { timeout: 20_000 });
  });

  test("the root row offers no subtask action", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(60_000);
    const format = await userFormat(page);
    // Located by id rather than by title: the root's name is the installation's own, and its id is 1
    // only by convention (see TaskServicesRest.Task.root).
    const rootId = await fetchRootTaskId(page.request);
    // Whether the row is there at all is the backend's decision — it appends the root for admins and
    // financial staff only. Asked before the page is opened, so an account without the right skips
    // instead of waiting 20 s for a row that will never come.
    const answer = await page.request.get(
      "/rs/task/tree?table=true&showRootForAdmins=true&initial=true",
      { headers: { "X-PF-Frontend": "next" } }
    );
    const { nodes = [] } = (await answer.json()) as {
      nodes?: { root?: boolean }[];
    };
    test.skip(
      !nodes.some((node) => node.root === true),
      "The account does not see the tree's root node."
    );

    await goto(page, PAGE);
    // Narrowed first: the root is *appended* to the answer (`TaskTreeProvider`'s comparator puts it
    // last), so on a tree wider than a page it sits on the last one and no row of it is in the DOM.
    // A search that matches the seeded task keeps the result well below one page — the root comes
    // along regardless, since the flag has nothing to do with the filter.
    await narrowToSeeded(page, format.t, seededTask.title);
    const rootRow = page.locator(`tbody tr[data-row-id="${rootId}"]`);
    await expect(rootRow).toBeVisible({ timeout: 20_000 });

    // Adding below the root is what the bar's own `+` does, so the row would offer the same thing
    // twice — and the root is the tree's anchor, not a task one hangs work under by accident.
    await expect(
      rootRow.getByRole("link", {
        name: new RegExp(format.t("task.title.add")),
      })
    ).toHaveCount(0);
  });

  test("the gear menu offers the re-index entries and the filter reset", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const status = await page.request.get("/rs/userStatus", {
      headers: { "X-PF-Frontend": "next" },
    });
    const isAdmin = ((await status.json()) as { adminUser?: boolean })
      .adminUser;

    await goto(page, PAGE);
    await gear(page, format).click();

    const menu = page.getByRole("menu");
    await expect(
      menu.getByRole("menuitem", {
        name: format.t("menu.reindexNewestDatabaseEntries._"),
      })
    ).toBeVisible();
    // The full rebuild includes the history and hits the whole system, so it is an admin's entry — the
    // request says whether this account is one, which is not the test's business to decide.
    await expect(
      menu.getByRole("menuitem", {
        name: format.t("menu.reindexAllDatabaseEntries._"),
      })
    ).toHaveCount(isAdmin ? 1 : 0);
    await expect(
      menu.getByRole("menuitem", { name: format.t("menu.resetFilter._") })
    ).toBeVisible();
  });

  test("the filter reset puts the status flags and the search string back", async ({
    loggedInPage: page,
  }) => {
    test.setTimeout(60_000);
    const format = await userFormat(page);
    await goto(page, PAGE);
    await waitForRows(page);

    // Away from the defaults first, in both halves of the filter: `closed` is off by default, and a
    // search string is what a reset has to clear as well (`TaskFilter.reset`).
    const search = page.getByLabel(format.t("search._"), { exact: true });
    await search.fill("zzz-nothing-matches");
    await statusPill(page, format, [
      "task.status.opened",
      "task.status.notOpened",
    ]).click();
    await page.getByLabel(format.t("task.status.closed")).check();
    await page.keyboard.press("Escape");
    await expect(
      statusPill(page, format, [
        "task.status.opened",
        "task.status.notOpened",
        "task.status.closed",
      ])
    ).toBeVisible();

    // Every call to the endpoint that would reset the *list* perspective, collected over the reset: it
    // stores an empty `MagicFilter` and drops the category's grid state (`AbstractEntityRest`), neither
    // of which has anything to do with this page's filter — so on the tree it must not be called at all
    // (see ListGearMenu.filterScope, `"own"`).
    const listResets: string[] = [];
    page.on("request", (request) => {
      if (request.url().includes("/rs/task/filter/reset")) {
        listResets.push(request.url());
      }
    });

    await gear(page, format).click();
    await page
      .getByRole("menuitem", { name: format.t("menu.resetFilter._") })
      .click();

    // The client resets the tree's own filter instead, which is a `TaskFilter` in the session.
    await expect(search).toHaveValue("", { timeout: 20_000 });
    await expect(
      statusPill(page, format, ["task.status.opened", "task.status.notOpened"])
    ).toBeVisible({ timeout: 20_000 });

    // And the backend kept it: it reads every parameter of a non-initial call as the user's new
    // filter, so a reload has to come back with the defaults rather than with what was typed above.
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(
      page.getByLabel(format.t("search._"), { exact: true })
    ).toHaveValue("", {
      timeout: 20_000,
    });

    expect(
      listResets,
      "resetting the tree's filter must not reset the task list's stored filter and columns"
    ).toEqual([]);
  });

  test("the search field links to the handbook", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    // Wicket's help icon in the search fieldset, with the bundle's own text — which overstates what the
    // field does (`TaskFilter.isVisibleBySearchString` is a `containsIgnoreCase` over seven columns,
    // not a Lucene query). Parity is deliberate; see TaskTreeFilterBar.
    const help = page.getByRole("link", {
      name: format.t("tooltip.lucene.link"),
    });
    await expect(help).toHaveAttribute("href", LUCENE_QUERY_DOCS_URL);
    await expect(help).toHaveAttribute("target", "_blank");
  });

  test("the select panel has neither the page's actions nor its hint", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(60_000);
    const format = await userFormat(page);
    // The same panel, without `pageMode`: the parent picker of the task form. Its actions would either
    // leave the form the user is in or offer to create what they came to pick.
    await goto(page, `/task/${seededTask.id}`);
    await page
      .getByRole("button", {
        name: `${format.t("task.tree.title.select")} ${label(format, "task.parentTask")}`,
      })
      .click();

    const dialog = page.getByRole("dialog");
    await waitForRows(dialog);
    // The hint is rendered as markdown (MarkdownText), so the catalog string is split across elements —
    // its lead-in sentence, terminated here by an escaped hard break (`\` then newline), is the one <p> a
    // single-node match can land on. Taken from the catalog through `t()` so it stays locale-independent.
    const hintLeadIn = format
      .t("task.selectPanel.info")
      .split(/[\n\\*]/)[0]
      .trim();
    await expect(dialog.getByText(hintLeadIn)).toBeVisible();
    await expect(
      dialog.getByRole("link", { name: format.t("menu.addNewEntry") })
    ).toHaveCount(0);
    await expect(
      dialog.getByRole("link", { name: format.t("tooltip.lucene.link") })
    ).toHaveCount(0);
    await expect(
      dialog.getByRole("button", { name: format.t("settings"), exact: true })
    ).toHaveCount(0);
    await page.keyboard.press("Escape");
  });
});
