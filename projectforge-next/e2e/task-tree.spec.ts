import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";

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

    // Every row shows its title rather than an object or an empty cell.
    await expect(page.locator(TREE_CELL).first()).toHaveText(/\p{L}/u);
  });

  test("keeps an expanded node across a reload", async ({
    loggedInPage: page,
  }) => {
    // Login, three tree loads and two round trips to the user prefs don't fit into the default 30s
    // against a dev server that compiles on demand.
    test.setTimeout(90_000);
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });
    const before = await rows.count();

    // The first collapsed node — its chevron carries the accessible name of the action it offers.
    const folder = rows
      .filter({ has: page.getByRole("img", { name: t("expand") }) })
      .first();
    // Its title, so the cleanup below collapses *this* node again. Taking "the first collapse icon"
    // instead would hit whichever node happens to sit highest, and any node the account had open
    // before this run is above it.
    const title = (await folder.locator("td").first().innerText()).trim();
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

  test("searching narrows the tree", async ({ loggedInPage: page }) => {
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });
    const before = await rows.count();

    // A term matching a title of the demo data. Searched server-side, so the answer is the matching
    // subtrees rather than a filtered page.
    await page.getByLabel(t("search._")).fill("Business");
    await expect
      .poll(() => rows.count(), { timeout: 20_000 })
      .toBeLessThanOrEqual(before);
    for (const text of await page.locator(TREE_CELL).allInnerTexts()) {
      expect(text.trim().length).toBeGreaterThan(0);
    }
  });

  test("a click outside the tree column selects, inside it expands", async ({
    loggedInPage: page,
  }) => {
    test.setTimeout(60_000);
    const { t } = await userFormat(page);
    await goto(page, PAGE);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });
    const before = await rows.count();

    // A folder row: only those have a chevron, and only for them do the two columns differ.
    const folder = rows
      .filter({ has: page.getByRole("img", { name: t("expand") }) })
      .first();

    // Inside the tree column: expands, and the url stays.
    await folder.locator("td").first().click();
    await expect
      .poll(() => rows.count(), { timeout: 20_000 })
      .toBeGreaterThan(before);
    expect(page.url()).toContain(PAGE);

    // Outside it: selects, which on this page means opening the task's (legacy) edit page.
    await page
      .locator("tbody tr")
      .filter({ has: page.getByRole("img", { name: t("collapse") }) })
      .first()
      .locator("td")
      .nth(3)
      .click();
    await expect(page).toHaveURL(/task/i, { timeout: 20_000 });
  });
});
