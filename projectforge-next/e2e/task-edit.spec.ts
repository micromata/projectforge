import type { Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRows } from "./fixtures/list-table";
import { resetTreeState } from "./fixtures/task-tree";

/**
 * The task edit page (`/next/task/:id`) against the live backend — the hand-built page of step 2 of
 * projectforge-next/MIGRATION.md.
 *
 * Three things here exist nowhere else and are what these cases are about: the two sections that start
 * folded and unfold when their tab is clicked, the return to the *caller* (`?returnTo=`, the tree
 * rather than the task list, which has no page yet), and the cost unit block, whose every derived value
 * is the backend's answer to `POST /rs/task/kost2Preview`.
 *
 * Read-only apart from the seeded task's own short description: the database is a copy of production,
 * so no case may touch a task it did not create. That the block's numbers agree with Wicket's is
 * settled elsewhere — both go through `TaskTree.getKost2List`, and a real task carrying a black/white
 * list is one these tests may not edit. What is checked here is the block on a task *without* a
 * project, which is the state it has to survive.
 *
 * Field labels go through [label], not through `t()` directly: `task.title` is a text *and* the parent
 * of `task.title.add`, so its own text sits at the `._` leaf — the same lookup the page makes.
 */
const TREE = "/taskTree";

/**
 * The card of a collapsed section, in declaration order — Gantt settings first, finance second.
 *
 * Located by the Collapsible root rather than by heading text, because that root is where Radix
 * reports the fold: `data-state` is the component's own state, not a guess from what is on screen.
 */
function collapsedSection(page: Page, which: "gantt" | "finance") {
  return page
    .locator('[data-slot="collapsible"]')
    .nth(which === "gantt" ? 0 : 1);
}

/** The tab strip's entry for a section, by its label. */
function tab(page: Page, name: string) {
  return page.getByRole("tab", { name });
}

/** The title box of the form — the proof that the page loaded the entity rather than a 404. */
function titleBox(page: Page, format: UserFormat) {
  return page.getByRole("textbox", { name: label(format, "task.title") });
}

/**
 * The breadcrumb back to the caller.
 *
 * Scoped to the page's own content: the menu carries a "Strukturbaum" link of its own (to Wicket,
 * until step 5 of the migration flips it), and an unscoped lookup would match both.
 */
function breadcrumb(page: Page, format: UserFormat) {
  return page
    .getByRole("main")
    .getByRole("link", { name: format.t("menu.taskTree") });
}

test.describe("task edit", () => {
  test("opens from the tree and leads back to it", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(
      page,
      `/task/${seededTask.id}?returnTo=${encodeURIComponent(TREE)}`
    );
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    // The breadcrumb names the caller — the tree, not the task list. Its label is the tree's menu
    // entry, which is what `returnTargets` declares.
    const back = breadcrumb(page, format);
    await expect(back).toBeVisible();
    await back.click();
    await expect(page).toHaveURL(new RegExp(`${TREE}$`), { timeout: 20_000 });
  });

  test("cancel returns to the caller, not to the task list", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(
      page,
      `/task/${seededTask.id}?returnTo=${encodeURIComponent(TREE)}`
    );
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    await page.getByRole("button", { name: format.t("cancel") }).click();
    await expect(page).toHaveURL(new RegExp(`${TREE}$`), { timeout: 20_000 });
  });

  test("a returnTo the declaration does not name is ignored", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    // The whitelist is the whole protection against an open redirect: an unlisted target must fall
    // back to the entity's own route rather than being followed.
    await goto(page, `/task/${seededTask.id}?returnTo=%2Fbook`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    await expect(
      page.getByRole("link", { name: format.t("book.title.list") })
    ).toHaveCount(0);
  });

  test("both extra sections start folded and unfold when their tab is clicked", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    const gantt = collapsedSection(page, "gantt");
    const finance = collapsedSection(page, "finance");
    await expect(gantt).toHaveAttribute("data-state", "closed");
    await expect(finance).toHaveAttribute("data-state", "closed");

    await tab(page, format.t("task.gantt.settings")).click();
    await expect(gantt).toHaveAttribute("data-state", "open");
    // A field of the section, so the body is really rendered and not merely marked open.
    await expect(
      page.getByRole("textbox", { name: label(format, "task.progress") })
    ).toBeVisible();

    await tab(page, format.t("financeAdministration")).click();
    await expect(finance).toHaveAttribute("data-state", "open");

    // Scrolling on does not fold a card the user opened — see CollapsedSection.
    await tab(page, format.t("task.title.heading")).click();
    await expect(gantt).toHaveAttribute("data-state", "open");
  });

  test("a section named in the hash is open on arrival", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    // What the history page's tabs link back to: without this the form would always start at its
    // first section and the link would appear to do nothing.
    await goto(page, `/task/${seededTask.id}#financeAdministration`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    await expect(collapsedSection(page, "finance")).toHaveAttribute(
      "data-state",
      "open",
      { timeout: 20_000 }
    );
  });

  test("the parent is picked from the tree, not from an autocomplete", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    // `dataType: "TASK"` dispatches to TaskSelectField, whose picker is the tree in a dialog — the
    // one control that can express a task's place in a hierarchy. A plain entity autocomplete would be
    // a combobox and nothing else; the type-ahead beside this button is the second way in, not the
    // only one (see the case below).
    await page
      .getByRole("button", {
        name: `${format.t("task.tree.title.select")} ${label(format, "task.parentTask")}`,
      })
      .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    await waitForRows(dialog);
    await page.keyboard.press("Escape");
  });

  test("a task is found by typing, and the hit names its whole path", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    // `TaskServicesRest.autosearch`, not the `task/autosearch` every category inherits: that one has no
    // search fields configured and answers an error (see AbstractEntityRest.getAutoCompleteObjects).
    const asked = page.waitForRequest(
      (request) => request.url().includes("/rs/task/tree/autosearch"),
      { timeout: 20_000 }
    );
    await page
      .getByRole("button", {
        name: `${format.t("search._")} ${label(format, "task.parentTask")}`,
      })
      .click();
    await page
      .getByPlaceholder(format.t("filter.search"))
      .fill(seededTask.child.title);
    await asked;

    // The hit is labelled with the path from the root down, joined as Wicket's own type-ahead joins it
    // — the only thing that tells two structure elements of the same name apart.
    const hit = page.getByRole("option", {
      name: `${seededTask.title} | ${seededTask.child.title}`,
      exact: true,
    });
    await expect(hit).toBeVisible({ timeout: 20_000 });
    await hit.click();

    // And it landed in the field: the breadcrumb of the parent now ends in the picked task. Nothing is
    // saved — the value is only in the form, as everywhere else in this file.
    await expect(
      page.getByRole("navigation", {
        name: format.t("task.path.pleaseSelectTask"),
      })
    ).toContainText(seededTask.child.title, { timeout: 20_000 });
  });

  test("the cost unit block survives a task without a project", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}#financeAdministration`);
    const finance = collapsedSection(page, "finance");
    await expect(finance).toHaveAttribute("data-state", "open", {
      timeout: 20_000,
    });

    // The seeded task hangs under the root and inherits no project, so there is no cost number to
    // show. The block is still there — cost units are configured in this installation, and Wicket
    // builds the fieldset from that setting rather than from the task.
    await expect(
      finance.getByRole("textbox", { name: label(format, "fibu.kost2") })
    ).toBeVisible();
    await expect(finance.getByText("—")).toBeVisible();
  });

  test("the picker is narrowed to the project's cost units", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}#financeAdministration`);
    const finance = collapsedSection(page, "finance");
    await expect(finance).toHaveAttribute("data-state", "open", {
      timeout: 20_000,
    });

    // An unresolved project must simply not narrow rather than sending `projektId=null` — see
    // fetchAutoCompletion, which drops null params. The seeded task has none, which is the case that
    // would produce that url.
    const asked = page.waitForRequest(
      (request) => request.url().includes("/rs/cost2/autosearch"),
      { timeout: 20_000 }
    );
    await finance
      .getByRole("combobox", {
        name: `${format.t("add")}: ${label(format, "fibu.kost2")}`,
      })
      .click();
    await page.getByPlaceholder(format.t("filter.search")).fill("4.0");
    const url = (await asked).url();
    expect(url, "a null project must not be sent as a parameter").not.toContain(
      "projektId"
    );
  });

  test("a new subtask is judged by the rights on its parent", async ({
    page,
    seededTask,
  }) => {
    // The admin account, because it is the one that reaches the interesting half: it may open the form
    // (PF_Admin) and has no finance rights, so the four fields of the finance section are refused to it.
    // With the default account everything is writable and the case could not fail.
    test.skip(
      !hasRole("admin-user"),
      "this instance has no admin-user account (see fixtures/credentials.ts)"
    );
    test.setTimeout(90_000);
    await login(page, "/next/", "admin-user");
    const format = await userFormat(page);

    // A task that has no id yet still has a parent, and Wicket asks the rights question on that parent
    // (`TaskEditForm.onBeforeRender`). Before the preset answered it, this form guessed "writable": the
    // fields looked editable and only the save was refused.
    await goto(page, `/task/new?parentTaskId=${seededTask.id}`);
    await expect(titleBox(page, format)).toBeVisible({ timeout: 30_000 });
    await tab(page, format.t("financeAdministration")).click();
    const finance = collapsedSection(page, "finance");
    await expect(finance).toHaveAttribute("data-state", "open");

    await expect(
      finance.getByRole("combobox", {
        name: label(format, "task.timesheetBooking"),
      })
    ).toBeDisabled();
    await expect(
      finance.getByRole("textbox", {
        name: label(format, "task.protectTimesheetsUntil"),
      })
    ).toBeDisabled();
    await expect(
      finance.getByRole("checkbox", {
        name: label(format, "task.protectionOfPrivacy"),
      })
    ).toBeDisabled();

    // Disabled *and* explained, with the backend's own refusal message — the divergence from Wicket,
    // which simply leaves the field uneditable (see FinanceSection).
    await finance
      .getByRole("button", {
        name: `${format.t("form.hint")}: ${label(format, "task.timesheetBooking")}`,
      })
      .hover();
    // By role: Radix keeps a second, hidden copy of the text for screen readers, so the sentence is in
    // the document twice and only one of the two is the tooltip.
    await expect(page.getByRole("tooltip")).toContainText(
      format.t("task.error.timesheetBookingStatus2Readonly"),
      { timeout: 20_000 }
    );
  });

  test("saves a change and returns to the tree, which marks the element", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(90_000);
    const format = await userFormat(page);
    const changed = `edited ${new Date().toISOString()}`;
    // The tree's filter is session-scoped and shared with the Wicket page: a search string another
    // case left in it would empty the tree the assertion below looks for its row in.
    await resetTreeState(page);
    await goto(
      page,
      `/task/${seededTask.id}?returnTo=${encodeURIComponent(TREE)}`
    );
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    const shortDescription = page.getByRole("textbox", {
      name: label(format, "shortDescription"),
    });
    await shortDescription.fill(changed);
    await page.getByRole("button", { name: format.t("save") }).click();

    // A save leaves for the caller, which is the whole point of `returnTargets` — the task list has
    // no page yet, so landing there would be a 404 of the static export. And it names what was saved
    // (`?highlightId=`, see HIGHLIGHT_ID_PARAM), which is the only thing the tree could mark a row by:
    // its rows are the visible nodes of a tree, not a result set with a stored highlight.
    await expect(page).toHaveURL(
      new RegExp(`${TREE}\\?highlightId=${seededTask.id}$`),
      { timeout: 20_000 }
    );

    // Marked, and reachable: the server opens the ancestors of the highlighted task, so the row is
    // among the visible nodes although nothing was searched for — Wicket's `PARAMETER_HIGHLIGHTED_ROW`.
    const row = page.locator(`tbody tr[data-row-id="${seededTask.id}"]`);
    await expect(row).toHaveClass(/row-highlighted/, { timeout: 30_000 });
    await expect(row).toBeInViewport({ timeout: 30_000 });

    // And it was really written, not merely accepted.
    await goto(page, `/task/${seededTask.id}`);
    await expect(
      page.getByRole("textbox", { name: label(format, "shortDescription") })
    ).toHaveValue(changed, { timeout: 20_000 });
  });

  test("the history is a tab of the form", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    // The seeded task has a history because it was inserted — one attribute per property, written by
    // `HistoryBaseDaoAdapter` as a side effect of the save. So no task of the database is needed, and
    // none may be touched (see the note at the top).
    //
    // The history is a tab of the edit page, reached by `?tab=history`; it is no longer a route of its
    // own (the legacy `/{id}/history` deep-link was dropped, see history.spec.ts).
    await goto(page, `/task/${seededTask.id}?tab=history`);
    await expect(
      page.getByRole("listitem").first().getByRole("button")
    ).toBeVisible({ timeout: 20_000 });

    // A section tab is the way back to the form, and it closes the panel rather than navigating —
    // which is the reason the history stopped being a route: the form tree stays mounted, so a
    // half-filled field survives the detour. The shared mechanism is asserted on the book (see
    // edit-tab-round-trip.spec.ts); what is checked here is that the task's own tabs take part in
    // it, since its two folded sections make its tab strip unlike any other page's.
    await tab(page, format.t("task.title.heading")).click();
    const changed = `${seededTask.title} unsaved`;
    await titleBox(page, format).fill(changed);
    await tab(page, format.t("label.historyOfChanges")).click();
    await expect(page.getByRole("listitem").first()).toBeVisible();
    await tab(page, format.t("task.title.heading")).click();
    // Not saved, and not lost either: the value is still in the input the user typed it into.
    await expect(titleBox(page, format)).toHaveValue(changed);
  });

  test("the five cross links of the Wicket form are in the header, the two used most as buttons", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);

    const header = page.getByRole("main");
    // Beside the heading without a click, on this viewport: the two an open task is left for
    // (`CrossLinkDef.prominent`). Below `md` they are menu entries like the rest, so this half of the
    // assertion belongs to the desktop viewport the suite runs in.
    for (const [key, href] of [
      ["task.menu.addSubTask", `/task/new?parentTaskId=${seededTask.id}`],
      ["task.menu.showTimesheets", `/wa/timesheetList?taskId=${seededTask.id}`],
    ] as const) {
      await expect(
        header.getByRole("link", { name: format.t(key) })
      ).toHaveAttribute("href", new RegExp(`${escapeRegExp(href)}$`));
    }

    await header.getByRole("button", { name: format.t("more") }).click();
    const menu = page.getByRole("menu");
    // The remaining entries and their order are `TaskEditPage.addTopMenuPanel`'s; every url names this
    // task, which is what makes them cross links and not menu items (see CrossLinkDef).
    for (const [key, href] of [
      ["task.menu.addTimesheet", `/wa/timesheetEdit?taskId=${seededTask.id}`],
      ["gantt.title.add", `/wa/ganttEdit?task=${seededTask.id}`],
      ["task.menu.showAccessRights", `/wa/accessList?taskId=${seededTask.id}`],
    ] as const) {
      await expect(
        menu.getByRole("menuitem", { name: format.t(key) })
        // Ends with, not equals: this app's own routes carry the base path in front of them.
      ).toHaveAttribute("href", new RegExp(`${escapeRegExp(href)}$`));
    }
    // The two that became buttons are still in the markup of the menu, hidden from `md` up — so a
    // reader of the a11y tree finds each target exactly once, not twice.
    for (const key of ["task.menu.addSubTask", "task.menu.showTimesheets"]) {
      await expect(
        menu.getByRole("menuitem", { name: format.t(key) })
      ).toHaveCount(0);
    }
    await page.keyboard.press("Escape");
  });

  test("a task not saved yet offers no cross links", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // Every target needs the entry's id, so before the first save there is nothing to link to —
    // Wicket builds the same menu only for `isNew() == false`.
    await goto(page, "/task/new");
    await expect(titleBox(page, format)).toBeVisible({ timeout: 20_000 });
    await expect(
      page.getByRole("main").getByRole("button", { name: format.t("more") })
    ).toHaveCount(0);
  });

  test("an out-of-range value is reported on its own field", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, `/task/${seededTask.id}`);
    await expect(titleBox(page, format)).toHaveValue(seededTask.title);
    await tab(page, format.t("task.gantt.settings")).click();

    const progress = page.getByRole("textbox", {
      name: label(format, "task.progress"),
    });
    await progress.fill("101");
    await progress.blur();
    await page.getByRole("button", { name: format.t("save") }).click();

    // The bound is `TaskDO.progress`'s own `@PropertyInfo(max = "100")`: the form spares the round
    // trip, the backend enforces the same rule, and neither spells the range out a second time.
    //
    // The client names the *broken* bound (`integerToHigh`), where the server's own check reports the
    // whole range (`integerOutOfRange`, see ValidationUtils): only one of the two was violated, so
    // saying which is the more useful sentence — and the value never reaches the server anyway.
    await expect(
      page.getByText(
        format.t("validation.error.range.integerToHigh", { arg0: 100 })
      )
    ).toBeVisible();
    // Nothing was saved, so the page stays; a successful save would have left for the tree.
    await expect(page).toHaveURL(new RegExp(`/task/${seededTask.id}`));
  });
});

/** Escapes what a url contains that a regular expression would read as syntax (`?`, `.`). */
function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * The tree's row click opens the next page and tells it where the user came from.
 *
 * Its own describe block because it starts on the tree rather than on the form, and because it is the
 * one case that proves the two halves of `?returnTo=` fit together: the tree writes the parameter and
 * the edit page reads it.
 */
test.describe("task tree row click", () => {
  test("opens the next edit page with the tree as its caller", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const format = await userFormat(page);
    await goto(page, TREE);
    const rows = await waitForRows(page);

    // The last word of the title: the client builds the query with URLSearchParams, which writes a
    // space as "+" rather than "%20" (see task-tree.spec.ts, which narrows the same way).
    const term = seededTask.title.split(" ").at(-1) ?? seededTask.title;
    const filtered = page.waitForResponse(
      (response) =>
        response.url().includes("/rs/task/tree?") &&
        response.url().includes(term) &&
        response.status() === 200,
      { timeout: 20_000 }
    );
    await page
      .getByLabel(format.t("search._"), { exact: true })
      .fill(seededTask.title);
    await filtered;
    const row = rows.filter({ hasText: seededTask.title }).first();
    await expect(row).toBeVisible({ timeout: 20_000 });

    // Outside the tree column, which is where a click means "select" — and selecting on this page
    // means editing.
    await row.locator("td").nth(3).click();
    await expect(page).toHaveURL(
      new RegExp(`/task/${seededTask.id}\\?returnTo=`),
      { timeout: 20_000 }
    );
    await expect(breadcrumb(page, format)).toBeVisible();
  });
});
