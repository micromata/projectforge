import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import type { Page } from "@playwright/test";

/**
 * The server-laid-out list page (`components/dynamic/components/grid/`) against the live backend.
 *
 * `vacation` is the check page for it: its layout carries every part the adapter has to translate —
 * `valueGetter` (`data?.employee?.displayName`), two DATE formatter columns, a
 * SHOW_LIST_OF_DISPLAYNAMES one, explicit widths, a `getRowClass` with two branches and a
 * `rowClickRedirectUrl` ending in `/id`. The list is read-only, so no test data is created; the
 * persistence test does write the account's column state and resets it again at the end.
 *
 * A vacation entry cannot be created from here (it needs an employee, a leave account and an
 * approver, none of which exist on a fresh database), so the cases that need a row skip when the list
 * is empty rather than failing — see [requireRows]. Nothing of the rows is named in this file: what is
 * asserted is their *shape* (a date is a date, a nested displayName is a name), never a value.
 */
const LIST = "/vacation";

/**
 * The layout sends its column labels as text (`headerName`), already translated — so the expectation
 * translates the same keys rather than spelling out one language's word (see projectforge-next/CLAUDE.md).
 * These are the keys `VacationDO` carries as `@PropertyInfo(i18nKey = …)`, which is where `lc` takes
 * the header names from.
 */
const EMPLOYEE_KEY = "vacation.employee";
const START_DATE_KEY = "vacation.startdate";
const COMMENT_KEY = "comment";

/**
 * Skips the case when the list has no row, and answers the rows otherwise.
 *
 * Only a spec whose entity it can create may insist on data. Vacation entries are not among those, so
 * "the list is empty" is a statement about the database rather than about the code under test.
 */
async function requireRows(page: Page) {
  const rows = listRows(page);
  // A page of the list has to have arrived first: straight after the navigation every list is empty.
  await expect(page.locator("table")).toBeVisible({ timeout: 20_000 });
  const count = await rows
    .first()
    .waitFor({ state: "visible", timeout: 20_000 })
    .then(() => 1)
    .catch(() => 0);
  test.skip(count === 0, "no vacation entry in this database");
  return rows;
}

async function headerCell(page: Page, label: string) {
  const cell = page.locator("th").filter({ hasText: label }).first();
  await expect(cell).toBeVisible({ timeout: 20_000 });
  return cell;
}

/**
 * Resolves once the backend has answered a `setColumnStates` post carrying `expected`.
 *
 * The dev server answers the trailing-slash-less url with a 308 first, so the body is matched rather
 * than the request counted: only the redirected request actually reaches Spring.
 */
function columnStateStored(page: Page, expected: string) {
  return page.waitForResponse(
    (response) =>
      response.url().includes("/setColumnStates") &&
      response.status() === 200 &&
      (response.request().postData() ?? "").includes(expected),
    { timeout: 20_000 }
  );
}

test.describe("dynamic grid", () => {
  // The column state lives in the account's prefs, i.e. it outlives the browser context — a test
  // that hides a column would otherwise dictate what the next one (or the next run) sees. Resetting
  // through the endpoint rather than through the UI keeps it independent of a failed assertion.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request.get("/rs/vacation/resetGridState/");
  });

  test("renders the layout's columns, formatted values and row colours", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, LIST);

    // The header proves the column defs survived the adapter, including the one whose value comes
    // from a valueGetter rather than from its own field.
    await headerCell(page, t(EMPLOYEE_KEY));
    await headerCell(page, t(START_DATE_KEY));
    await headerCell(page, t(COMMENT_KEY));

    const rows = await requireRows(page);

    // valueGetter: the employee cell must show the nested displayName, not "[object Object]" or
    // JSON — that is what the replaced hand-written table did.
    const firstEmployee = rows.first().locator("td").first();
    await expect(firstEmployee).not.toHaveText(/object|\{/i);
    await expect(firstEmployee).toHaveText(/\p{L}/u);

    // The DATE formatter renders through Intl with the layout's dd.MM.yyyy, so every start date is
    // a German date and never an ISO string.
    const startDates = listRows(page).locator("td:nth-child(2)");
    for (const text of await startDates.allInnerTexts()) {
      expect(text.trim()).toMatch(/^\d{2}\.\d{2}\.\d{4}$/);
    }

    // getRowClass, translated by row-class.ts: a row the layout's rule does not match gets no class
    // at all, so what can be asserted without knowing the data is that every class a row *does* carry
    // is one the stylesheet defines — an unrecognised rule (or a class name the parser passed through)
    // would show up here as a row-* that globals.css never styles.
    await page.getByRole("combobox").last().selectOption("100");
    const classes = await listRows(page).evaluateAll((rows) =>
      rows.flatMap((row) => Array.from(row.classList))
    );
    for (const className of classes.filter((name) => name.startsWith("row-"))) {
      expect(className).toMatch(/^row-(deleted|red|green|blue|grey)$/);
    }
  });

  test("row click opens the entry", async ({ loggedInPage: page }) => {
    await goto(page, LIST);
    const rows = await requireRows(page);

    await rows.first().click();
    // `rowClickRedirectUrl` is "/react/vacation/edit/id": vacation is not in NextMigration.MIGRATED,
    // so the row leads into the legacy frontend — with the row's id substituted for the trailing
    // "/id" segment (applyRowId).
    await expect(page).toHaveURL(/\/react\/vacation\/edit\/\d+/, {
      timeout: 20_000,
    });
  });

  test("keeps sorting and a hidden column across a reload", async ({
    loggedInPage: page,
  }) => {
    // Login, two full list loads and two round trips to the user prefs don't fit into the default
    // 30s against a dev server that compiles on demand.
    test.setTimeout(90_000);
    const { t } = await userFormat(page);
    await goto(page, LIST);
    // The sorted-first-cell assertion below needs a row; the column state itself would persist
    // without one.
    await requireRows(page);

    // Sorting: a click on the header cell sorts (DataTable sorts on the whole cell), which posts to
    // onColumnStatesChangedUrl.
    await (await headerCell(page, t(EMPLOYEE_KEY))).click();
    const sortedFirst = await listRows(page)
      .first()
      .locator("td")
      .first()
      .innerText();

    // Hiding: through the column panel, the same path the book list uses. The write is debounced, so
    // the test has to see it land before reloading — otherwise it races the client and the reload
    // shows the state from before the click.
    const panel = page.getByRole("button", { name: t("columns._") });
    await panel.click();
    const checkbox = page.locator("#col-comment");
    await expect(checkbox).toHaveAttribute("data-state", "checked");
    const stored = columnStateStored(page, '"comment":false');
    await checkbox.click();
    await expect(checkbox).toHaveAttribute("data-state", "unchecked");
    await page.keyboard.press("Escape");
    await expect(
      page.locator("th").filter({ hasText: t(COMMENT_KEY) })
    ).toHaveCount(0);
    await stored;

    // The reload is the actual assertion: the state has to come back through the *layout* response,
    // which the backend rewrites in restoreColumnsFromUserPref — nothing is stored in the client.
    await page.reload({ waitUntil: "domcontentloaded" });
    await headerCell(page, t(EMPLOYEE_KEY));
    await expect(
      page.locator("th").filter({ hasText: t(COMMENT_KEY) })
    ).toHaveCount(0);
    await expect(listRows(page).first().locator("td").first()).toHaveText(
      sortedFirst
    );

    // "Reset columns" (useGridStateReset) is the counterpart: it drops the stored preference and
    // applies the answer's default columnDefs to the table state, so the column comes back without a
    // reload. That also leaves the account as it was found.
    await panel.click();
    await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes("/resetGridState") &&
          response.status() === 200
      ),
      page.getByRole("button", { name: t("columns.reset") }).click(),
    ]);
    await expect(page.locator("#col-comment")).toHaveAttribute(
      "data-state",
      "checked"
    );
    await page.keyboard.press("Escape");
    await headerCell(page, t(COMMENT_KEY));
  });
});
