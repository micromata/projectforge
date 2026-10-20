import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import type { Page } from "@playwright/test";
import { writeHeaders } from "./fixtures/seed";

/**
 * The server-laid-out list page (`components/dynamic/components/grid/`) against the live backend.
 *
 * `outgoingMail` is the check page for it (the vacation list was, then the employee list, then the contracts,
 * until each was migrated): its layout carries the parts the adapter has to translate — a DATE formatter column
 * and a `rowClickRedirectUrl` ending in `/id`. The list is read-only, so no test data is created; the persistence
 * test does write the account's column state and resets it again at the end.
 *
 * No outgoing mail is created from here, so the cases that need a row skip when the list is empty rather than
 * failing — see [requireRows]. Nothing of the rows is named in this file: what is asserted is their
 * *shape* (a date is a date, a text is a text), never a value.
 */
const LIST = "/outgoingMail";

/**
 * The layout sends its column labels as text (`headerName`), already translated — so the expectation
 * translates the same keys rather than spelling out one language's word (see projectforge-next/CLAUDE.md).
 * These are the keys `PostausgangDO` carries as `@PropertyInfo(i18nKey = …)`, which is where `lc` takes the
 * header names from.
 */
const TITLE_KEY = "orga.post.inhalt";
// `date` is a text *and* a parent key, so the text itself is `date._` (the generator nests it).
const DATE_KEY = "date._";
const RECEIVER_KEY = "orga.postausgang.empfaenger";

/**
 * Skips the case when the list has no row, and answers the rows otherwise.
 *
 * Only a spec whose entity it creates may insist on data. This one creates no outgoing mail, so "the list is
 * empty" is a statement about the database rather than about the code under test.
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
  test.skip(count === 0, "no outgoing mail in this database");
  return rows;
}

async function headerCell(page: Page, label: string) {
  const cell = page.locator("th").filter({ hasText: label }).first();
  await expect(cell).toBeVisible({ timeout: 20_000 });
  return cell;
}

/**
 * The 1-based position (for `nth-child`) of the first column whose header holds `label`, matched as
 * headerCell matches it.
 */
async function columnIndex(page: Page, label: string) {
  const index = await page
    .locator("thead th")
    .evaluateAll(
      (cells, text) =>
        cells.findIndex((cell) => cell.textContent?.includes(text)),
      label
    );
  expect(index, `column "${label}"`).toBeGreaterThanOrEqual(0);
  return index + 1;
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

test.describe("dynamic grid", { tag: "@lane-book" }, () => {
  // The column state lives in the account's prefs, i.e. it outlives the browser context — a test
  // that hides a column would otherwise dictate what the next one (or the next run) sees. Resetting
  // through the endpoint rather than through the UI keeps it independent of a failed assertion.
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request.post("/rs/outgoingMail/resetGridState", {
      headers: await writeHeaders(page.request),
    });
    // And the list filter: a filter stored by an earlier visit would narrow the list, and an empty list skips the
    // cases that need a row.
    await page.request.post("/rs/outgoingMail/filter/reset", {
      headers: await writeHeaders(page.request),
    });
  });

  test("renders the layout's columns, formatted values and row colours", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, LIST);

    // The header proves the column defs survived the adapter.
    await headerCell(page, t(TITLE_KEY));
    await headerCell(page, t(DATE_KEY));
    await headerCell(page, t(RECEIVER_KEY));

    const rows = await requireRows(page);

    // A cell shows its value as text, not "[object Object]" or JSON — that is what the replaced
    // hand-written table did.
    const titleColumn = await columnIndex(page, t(TITLE_KEY));
    const title = rows.first().locator(`td:nth-child(${titleColumn})`);
    await expect(title).not.toHaveText(/object Object|\{/i);

    // The DATE formatter renders through Intl with the layout's dd.MM.yyyy, so every date is a German
    // date and never an ISO string — or empty, an entry may have none.
    // Located by its header rather than by a fixed position: PostausgangPagesRest.createListLayout may gain a
    // column now and then, and a stale index reads another column's text.
    const dateColumn = await columnIndex(page, t(DATE_KEY));
    const dates = listRows(page).locator(`td:nth-child(${dateColumn})`);
    for (const text of await dates.allInnerTexts()) {
      expect(text.trim()).toMatch(/^(\d{2}\.\d{2}\.\d{4})?$/);
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
      // `row-highlighted` marks the entry edited last (globals.css), not a rule of the layout.
      expect(className).toMatch(
        /^row-(deleted|red|green|blue|grey|highlighted)$/
      );
    }
  });

  test("row click opens the entry", async ({ loggedInPage: page }) => {
    await goto(page, LIST);
    const rows = await requireRows(page);

    await rows.first().click();
    // `rowClickRedirectUrl` is "/react/outgoingMail/edit/id": outgoingMail is not in NextMigration.MIGRATED,
    // so the row leads into the legacy frontend — with the row's id substituted for the trailing
    // "/id" segment (applyRowId).
    await expect(page).toHaveURL(/\/react\/outgoingMail\/edit\/\d+/, {
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
    await (await headerCell(page, t(TITLE_KEY))).click();
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
    const checkbox = page.locator("#col-empfaenger");
    await expect(checkbox).toHaveAttribute("data-state", "checked");
    const stored = columnStateStored(page, '"empfaenger":false');
    await checkbox.click();
    await expect(checkbox).toHaveAttribute("data-state", "unchecked");
    await page.keyboard.press("Escape");
    await expect(
      page.locator("th").filter({ hasText: t(RECEIVER_KEY) })
    ).toHaveCount(0);
    await stored;

    // The reload is the actual assertion: the state has to come back through the *layout* response,
    // which the backend rewrites in restoreColumnsFromUserPref — nothing is stored in the client.
    await page.reload({ waitUntil: "domcontentloaded" });
    await headerCell(page, t(TITLE_KEY));
    await expect(
      page.locator("th").filter({ hasText: t(RECEIVER_KEY) })
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
    await expect(page.locator("#col-empfaenger")).toHaveAttribute(
      "data-state",
      "checked"
    );
    await page.keyboard.press("Escape");
    await headerCell(page, t(RECEIVER_KEY));
  });
});
