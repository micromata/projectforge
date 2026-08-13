import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { DEFAULT_PAGE_SIZE } from "../components/data-table/page-size-options";

/**
 * The structure tree page (`/next/taskTree`) against the live backend.
 *
 * What is under test is the part the tree does differently from every other table: its rows come from
 * `TaskServicesRest` rather than from a list layout, its expansion state lives in the user's prefs
 * instead of in the client, and a click means "expand" or "select" depending on the column it lands
 * in. The strongest of these is the reload: an expanded node that survives it can only have been
 * stored server-side.
 *
 * Read-only — nothing is written but the two preferences the page exists to keep (the open nodes and
 * the column state), and both are reset before each case.
 */
const PAGE = "/taskTree";

/** The tree column, pinned left; the one whose click expands rather than selects. */
const TREE_CELL = "tbody tr td:nth-child(1)";

/**
 * Narrows the tree to the seeded task and answers its row together with the number of rows left.
 *
 * Necessary, not merely tidy: the tasks of every run stay in the database (see fixtures/seed.ts), so
 * the root's children outgrow a page of the table, and the newest of them — this run's — lands on the
 * last one. A search asks the backend for the matching subtrees, which brings the row onto page one.
 *
 * The count comes from here rather than from the caller, and only after the *filtered* answer has
 * arrived: the search is debounced, so for a while the table still shows the unfiltered page — a count
 * taken then is a count of the wrong list, and a later "more rows than before" can never reach it.
 * Keyed on the response rather than on "the row count stopped changing", because between the
 * keystrokes and the answer the table sits still at the old number for longer than any poll interval.
 */
async function narrowToSeeded(page: Page, t: UserFormat["t"], title: string) {
  // The last word of the title, not the whole one: the client builds the query with URLSearchParams,
  // which writes a space as "+" rather than "%20" — matching the encoded title would never hit.
  const term = title.split(" ").at(-1) ?? title;
  const filtered = page.waitForResponse(
    (response) =>
      response.url().includes("/rs/task/tree") &&
      response.url().includes(term) &&
      response.status() === 200,
    { timeout: 20_000 }
  );
  await page.getByLabel(t("search._")).fill(title);
  const { nodes = [] } = (await (await filtered).json()) as {
    nodes?: unknown[];
  };
  const rows = page.locator("tbody tr");
  // The answer says how many rows the table will have, so waiting for that number is waiting for the
  // rendering of *this* answer rather than for an arbitrary moment of quiet. Capped at a page, since
  // a wider result would be paginated — the seeded subtree is far below one page.
  await expect(rows).toHaveCount(Math.min(nodes.length, DEFAULT_PAGE_SIZE), {
    timeout: 20_000,
  });
  const row = rows.filter({ hasText: title }).first();
  await expect(row).toBeVisible();
  return { row, count: await rows.count() };
}

test.describe("task tree", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The filter is session-scoped, so a search string left behind by a manual session (or by the
    // Wicket page, which shares it) would empty the tree under test. A non-initial call is what sets
    // it — the same request the panel sends when the filter changes.
    await page.request
      .get(
        "/rs/task/tree?table=true&searchString=&opened=true&notOpened=true&closed=false&deleted=false"
      )
      .catch(() => undefined);
    // The column state outlives the browser context (it is in the account's prefs), so a hidden
    // column from another run must not decide what this one sees.
    await page.request
      .get("/rs/task/tree/resetGridState/")
      .catch(() => undefined);
  });

  test("renders the backend's columns and the selection hint", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    await expect(
      page.getByRole("heading", { name: t("menu.taskTree") })
    ).toBeVisible({ timeout: 20_000 });

    // The headers prove the column defs of createDefaultColumnDefs survived the adapter. They arrive
    // already translated (`headerName`), so the expectation goes through the same catalogs.
    for (const key of ["task._", "task.consumption", "shortDescription"]) {
      await expect(
        page.getByRole("columnheader", { name: t(key) }),
        `column ${key}`
      ).toHaveCount(1);
    }

    // The hint below the table is what makes the cell-level click discoverable at all.
    await expect(page.getByText(t("task.selectPanel.info"))).toBeVisible();

    // A row shows its title rather than an object or an empty cell. The seeded task's row, not the
    // first one: the root is appended only for admins and financial staff (`showRootForAdmins`), so on
    // a fresh database with an ordinary account there would be no guaranteed row at all.
    const { row } = await narrowToSeeded(page, t, seededTask.title);
    await expect(row.locator("td").first()).toHaveText(/\p{L}/u);
  });

  test("keeps an expanded node across a reload", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    // Login, three tree loads and two round trips to the user prefs don't fit into the default 30s
    // against a dev server that compiles on demand.
    test.setTimeout(90_000);
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });

    // The seeded task, not "the first collapsed node": it has a child, so it is certainly a folder,
    // and a database without one (a fresh one, or one whose folders the account has all open) offers
    // no chevron to click at all. Its chevron carries the accessible name of the action it offers.
    const title = seededTask.title;
    // Counted by the helper, after the search: the filter is session-scoped, so it survives the
    // reload below and both counts are of the same list.
    const { row: folder, count: before } = await narrowToSeeded(page, t, title);
    await folder.getByRole("img", { name: t("expand") }).click();
    // More rows: the children the server added, since the client has no expansion model to unfold.
    await expect
      .poll(() => rows.count(), { timeout: 20_000 })
      .toBeGreaterThan(before);
    // The reload is the assertion: the open set is in the user's prefs
    // (TaskTree.USER_PREFS_KEY_OPEN_TASKS), so the tree has to come back unfolded.
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });
    // The node itself, rather than the row count: `open=<id>` opens the ancestors too, and any node
    // the account had open before this run adds rows this test never asked for. That it offers
    // "collapse" now is what says the server remembered it.
    await expect(
      rows
        .filter({ hasText: title })
        .first()
        .getByRole("img", {
          name: t("collapse"),
        })
    ).toBeVisible({ timeout: 20_000 });
    await expect
      .poll(() => rows.count(), { timeout: 20_000 })
      .toBeGreaterThan(before);

    // Collapsing the same node again leaves the account as the test found it.
    await rows
      .filter({ hasText: title })
      .first()
      .getByRole("img", { name: t("collapse") })
      .click();
    await expect.poll(() => rows.count(), { timeout: 20_000 }).toBe(before);
  });

  test("searching narrows the tree", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });
    const before = await rows.count();

    // The seeded task's own title, not a term of the database: every title of this instance is
    // production content (see fixtures/seed.ts), and on a fresh database none of them exists.
    // Searched server-side, so the answer is the matching subtrees rather than a filtered page.
    await page.getByLabel(t("search._")).fill(seededTask.title);
    await expect
      .poll(() => rows.count(), { timeout: 20_000 })
      .toBeLessThanOrEqual(before);
    // The task and its child, plus the ancestors the backend sends along to place them.
    await expect(rows.filter({ hasText: seededTask.title })).toHaveCount(1);
    for (const text of await page.locator(TREE_CELL).allInnerTexts()) {
      expect(text.trim().length).toBeGreaterThan(0);
    }
  });

  test("a click outside the tree column selects, inside it expands", async ({
    loggedInPage: page,
    seededTask,
  }) => {
    test.setTimeout(60_000);
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });

    // The seeded task: a folder (it has a child), and only for a folder do the two columns differ. Any
    // other row would be one of the database's own — and on a fresh database there is none.
    const { row: folder, count: before } = await narrowToSeeded(
      page,
      t,
      seededTask.title
    );

    // Inside the tree column: expands, and the url stays.
    await folder.locator("td").first().click();
    await expect
      .poll(() => rows.count(), { timeout: 20_000 })
      .toBeGreaterThan(before);
    expect(page.url()).toContain(PAGE);

    // Outside it: selects, which on this page means opening the task's (legacy) edit page.
    await folder.locator("td").nth(3).click();
    await expect(page).toHaveURL(/task/i, { timeout: 20_000 });
  });
});
