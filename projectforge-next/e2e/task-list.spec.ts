import type { APIRequestContext, Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRow, waitForRows } from "./fixtures/list-table";
import { TASK_PAGE } from "../components/features/task/task.page";
import { TASK_METADATA } from "../lib/metadata/task.generated";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import type { TaskListRow } from "../components/features/task/types";
import type { SeededTask } from "./fixtures/seed";

/**
 * The task list — step 4 of projectforge-next/MIGRATION.md, the second perspective on the same tasks
 * the structure tree shows.
 *
 * Its inventory is `TaskListPage.createColumns`: ten columns, three of which show a value that is not
 * on `TaskDO` and is computed per row (the consumption bar, the cost units in wild card form, the
 * orders), and three of which exist only where their subject does — cost units configured, orders
 * booked, the finance group. Which of those three this account has is *asked* (`listMeta.variables`)
 * rather than assumed, so the spec stays true on an installation or an account where one is false.
 *
 * The computed columns are compared against the *tree's* answer for the same task rather than against
 * text spelled out here: both come from `TaskTree` through the same functions, so a divergence is a
 * real one — and no content of the database (which is a copy of production) enters the source.
 *
 * Read-only apart from the seeded task, which the search narrows to; nothing about a particular row of
 * the database is asserted.
 */
const PAGE = "/task";

/** The three flags `TaskPagesRest.addVariablesForListPage` answers, keyed as the declarations read them. */
type ListVariables = Record<string, unknown>;

test.describe("task list", () => {
  let seeded: SeededTask;
  let variables: ListVariables;

  test.beforeEach(async ({ loggedInPage: page, seededTask }) => {
    seeded = seededTask;
    variables = await listVariables(page.request);
    // A criterion another run left in the account's stored filter would empty the list under test.
    await page.request
      .get("/rs/task/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the columns of TaskListPage, each under the label its field declares", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    await expect(
      page.getByRole("heading", { name: label(format, TASK_PAGE.titleKey) })
    ).toBeVisible();
    await waitForRows(page);

    // Against the declaration and the generated metadata, never against literals: the label of a field
    // column is the `i18nKey` of that field in `TaskDO`, which the declaration deliberately does not
    // repeat. A column whose `visible` predicate the backend denies must be absent, which is the same
    // assertion with the count flipped — that is what keeps this honest where a flag is false.
    for (const column of TASK_PAGE.columns) {
      const id = columnIdOf(column);
      const shown = column.visible?.({ variables }) ?? true;
      await expect(
        headerCell(page, columnLabel(format, column)),
        `column ${id} must ${shown ? "be shown" : "be absent"}`
      ).toHaveCount(shown ? 1 : 0);
    }
  });

  test("computes the three columns that are not on TaskDO from the same tree the tree page reads", async ({
    loggedInPage: page,
  }) => {
    // Both perspectives on one task: the row of the list and the node of the tree. Compared as data
    // rather than as rendered text — the bar has no text, and the orders are links whose labels would
    // have to be re-joined here.
    const row = await fetchRow(page.request, seeded.id);
    expect(row, "the seeded task must be in the list").toBeTruthy();
    const node = await fetchNode(page.request, seeded.id);

    expect(row!.kost2WildCard ?? null).toEqual(node.kost2WildCard ?? null);
    expect(row!.kost2ListAsLines ?? null).toEqual(
      node.kost2ListAsLines ?? null
    );
    // The bar is painted from `barPercentage` and coloured from `status`, so those two are what has to
    // agree; `title` is the tooltip and is built from the user's number format in both. Compared as
    // "the same or absent in both": `Consumption.create` answers nothing for a task with neither
    // planned nor booked effort, which the seeded one is — and the cell has to render that as an empty
    // column rather than as an empty bar.
    const consumption = (key: "status" | "barPercentage") => [
      (row!.consumption as Record<string, unknown> | undefined)?.[key] ?? null,
      (node.consumption as Record<string, unknown> | undefined)?.[key] ?? null,
    ];
    expect(consumption("status")[0]).toEqual(consumption("status")[1]);
    expect(consumption("barPercentage")[0]).toEqual(
      consumption("barPercentage")[1]
    );
    // The seeded task has no order booked against it, which is the case the cell has to render as
    // nothing rather than as an empty link.
    expect(row!.orderList ?? []).toEqual([]);
  });

  test("offers no sorting on a column the backend cannot order by", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);
    await waitForRows(page);

    // The list goes through `MagicFilterProcessor`, i.e. it sorts by entity property — the three
    // computed values are none, and Wicket's list says the same by passing them no sort property. The
    // header of such a column offers no sorting, which is what `sortable: false` produces (see
    // useDeclaredColumns, which turns it into TanStack's `enableSorting: false`).
    //
    // Asserted on the sort affordance of the header, not on a button: this table sorts by a click on
    // the whole header cell, since a button around the label would compete with the filter icon for
    // space (see DataTable and DataTableColumnHeader). The affordance is the indicator the header
    // renders while it can sort — the hint icon while unsorted, the direction arrow once sorted, so a
    // column the stored layout already sorts by counts too.
    for (const column of TASK_PAGE.columns) {
      if ((column.visible?.({ variables }) ?? true) === false) continue;
      const header = headerCell(page, columnLabel(format, column));
      // A period column cannot opt out at all (PeriodColumn omits `sortable`), so it is one of the
      // sorting ones — hence the `in` rather than a plain read of the union.
      const sortable = !("sortable" in column) || column.sortable !== false;
      await expect(
        header.locator(sortIndicator(format)),
        `column ${columnIdOf(column)} must ${sortable ? "sort" : "not sort"}`
      ).toHaveCount(sortable ? 1 : 0);
    }
  });

  test("opens a task from the list and comes back to the list", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, PAGE);
    // The run's suffix alone, not the whole title: the backend's search matches a row on any word of
    // the term, so "ZZ e2e task …" would pull in every task an earlier run left as well — and the
    // seeded one need not then be on the page the list serves. The suffix leaves this run's pair.
    await page.getByPlaceholder(t("filter.searchList")).fill(seeded.suffix);
    const row = await waitForRow(page, seeded.title);

    // The title cell, not the row: a row's centre may be the orders column, whose entries are links of
    // their own and would navigate elsewhere.
    await row.getByRole("cell", { name: seeded.title, exact: true }).click();

    // `returnTo` is the list, so cancelling comes back here rather than to the tree — the two are
    // separate `returnTargets` of the same form (see EditDef.returnTargets).
    await expect(page).toHaveURL(
      new RegExp(`/task/${seeded.id}\\?returnTo=%2Ftask$`),
      { timeout: 20_000 }
    );
    await page.getByRole("button", { name: t("cancel"), exact: true }).click();
    await expect(page).toHaveURL(/\/task(\?|$)/, { timeout: 20_000 });
  });

  test("links to the tree, and the tree back to the list", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, PAGE);

    // Wicket's pair of perspective buttons (see TaskPerspectiveLink). The list's own title is the label
    // of the way back, since Wicket's button there reads an untranslated model.
    await page
      .getByRole("link", {
        name: label(format, "task.tree.perspective"),
        exact: true,
      })
      .click();
    await expect(page).toHaveURL(/\/taskTree$/, { timeout: 20_000 });

    await page
      .getByRole("link", {
        name: label(format, "task.title.list"),
        exact: true,
      })
      .click();
    await expect(page).toHaveURL(/\/task$/, { timeout: 20_000 });
  });
});

/**
 * The header a declared column carries: its own key if it names one, else the `i18nKey` of the field
 * in `TaskDO` — resolved through the same `<key>._` fallback the app's own lookup uses.
 */
function columnLabel(
  format: UserFormat,
  column: (typeof TASK_PAGE.columns)[number]
): string {
  return label(format, columnHeaderKeyOf(column, TASK_METADATA));
}

/**
 * The header cell carrying a label, found by that label's own element rather than by the accessible
 * name of the cell: the cell also holds the sort indicator, the filter button and the resize handle,
 * and those contribute to its name — which is why an exact match on the role never resolves. The
 * label's element is the one `DataTableColumnHeader` marks with `data-overflow-text`, for the same
 * reason (its glyph neighbours are not part of the text).
 */
function headerCell(page: Page, name: string): Locator {
  return page.locator("thead th").filter({
    has: page
      .locator("[data-overflow-text]")
      .filter({ hasText: new RegExp(`^${escapeForRegExp(name)}$`) }),
  });
}

/**
 * What a header shows while it can be sorted: the hint icon it carries unsorted, or the direction
 * arrow once it is sorted. Both hang off their tooltip text, since they are icons with no text of
 * their own — and the texts are the catalog's, not spelled out here.
 */
function sortIndicator(format: UserFormat): string {
  return ["sort", "sortAscending", "sortDescending"]
    .map((key) => `[data-tooltip="${format.t(`columns.${key}`)}"]`)
    .join(",");
}

function escapeForRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** What this installation and this account have of the three conditional columns. */
async function listVariables(
  request: APIRequestContext
): Promise<ListVariables> {
  const res = await request.get("/rs/task/listMeta", {
    headers: { "X-PF-Frontend": "next" },
  });
  const meta = (await res.json()) as { variables?: ListVariables };
  return meta.variables ?? {};
}

/** The seeded task's row as the list serves it, found by the search the page's box sends. */
async function fetchRow(
  request: APIRequestContext,
  id: number
): Promise<TaskListRow | undefined> {
  const res = await request.post("/rs/task/list", {
    headers: await writeHeaders(request),
    data: { searchString: "" },
  });
  const body = (await res.json()) as { resultSet?: TaskListRow[] };
  return (body.resultSet ?? []).find((row) => row.id === id);
}

/** The same task as the tree's own endpoint answers it. */
async function fetchNode(request: APIRequestContext, id: number) {
  const res = await request.get(`/rs/task/info/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  return (await res.json()) as {
    kost2WildCard?: string;
    kost2ListAsLines?: string;
    consumption?: unknown;
  };
}

/** A list call is a POST and therefore needs the CSRF token, read per call (see fixtures/seed.ts). */
async function writeHeaders(
  request: APIRequestContext
): Promise<Record<string, string>> {
  const status = await request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}
