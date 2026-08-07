import { test, expect, goto } from "./fixtures/auth";
import type { Page } from "@playwright/test";

/**
 * The server-laid-out list page (`components/dynamic/components/grid/`) against the live backend.
 *
 * `vacation` is the check page for it: its layout carries every part the adapter has to translate —
 * `valueGetter` (`data?.employee?.displayName`), two DATE formatter columns, a
 * SHOW_LIST_OF_DISPLAYNAMES one, explicit widths, a `getRowClass` with two branches and a
 * `rowClickRedirectUrl` ending in `/id`. The list is read-only, so no test data is created; the
 * persistence test does write the account's column state and resets it again at the end.
 */
const LIST = "/vacation";

/** Column labels come from the layout as plain text (`headerName`), not as i18n keys. */
const EMPLOYEE = "Mitarbeiter";
const COMMENT = "Bemerkung";

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
    await goto(page, LIST);

    // The header proves the column defs survived the adapter, including the one whose value comes
    // from a valueGetter rather than from its own field.
    await headerCell(page, EMPLOYEE);
    await headerCell(page, "Startdatum");
    await headerCell(page, COMMENT);

    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible();

    // valueGetter: the employee cell must show the nested displayName, not "[object Object]" or
    // JSON — that is what the replaced hand-written table did.
    const firstEmployee = rows.first().locator("td").first();
    await expect(firstEmployee).not.toHaveText(/object|\{/i);
    await expect(firstEmployee).toHaveText(/\p{L}/u);

    // The DATE formatter renders through Intl with the layout's dd.MM.yyyy, so every start date is
    // a German date and never an ISO string.
    const startDates = page.locator("tbody tr td:nth-child(2)");
    for (const text of await startDates.allInnerTexts()) {
      expect(text.trim()).toMatch(/^\d{2}\.\d{2}\.\d{4}$/);
    }

    // getRowClass, translated by row-class.ts: conflicting entries are red. There are a handful in
    // the demo data, but none of them on the first page of 50 — so widen the page first.
    await page.getByRole("combobox").last().selectOption("100");
    await expect(page.locator("tbody tr.row-red").first()).toBeVisible();
  });

  test("row click opens the entry", async ({ loggedInPage: page }) => {
    await goto(page, LIST);
    const rows = page.locator("tbody tr");
    await expect(rows.first()).toBeVisible({ timeout: 20_000 });

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
    await goto(page, LIST);

    // Sorting: a click on the header cell sorts (DataTable sorts on the whole cell), which posts to
    // onColumnStatesChangedUrl.
    await (await headerCell(page, EMPLOYEE)).click();
    const sortedFirst = await page
      .locator("tbody tr")
      .first()
      .locator("td")
      .first()
      .innerText();

    // Hiding: through the column panel, the same path the book list uses. The write is debounced, so
    // the test has to see it land before reloading — otherwise it races the client and the reload
    // shows the state from before the click.
    const panel = page.getByRole("button", { name: /Spalten/ });
    await panel.click();
    const checkbox = page.locator("#col-comment");
    await expect(checkbox).toHaveAttribute("data-state", "checked");
    const stored = columnStateStored(page, '"comment":false');
    await checkbox.click();
    await expect(checkbox).toHaveAttribute("data-state", "unchecked");
    await page.keyboard.press("Escape");
    await expect(page.locator("th").filter({ hasText: COMMENT })).toHaveCount(
      0
    );
    await stored;

    // The reload is the actual assertion: the state has to come back through the *layout* response,
    // which the backend rewrites in restoreColumnsFromUserPref — nothing is stored in the client.
    await page.reload({ waitUntil: "domcontentloaded" });
    await headerCell(page, EMPLOYEE);
    await expect(page.locator("th").filter({ hasText: COMMENT })).toHaveCount(
      0
    );
    await expect(
      page.locator("tbody tr").first().locator("td").first()
    ).toHaveText(sortedFirst);

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
      page.getByRole("button", { name: /zurücksetzen/i }).click(),
    ]);
    await expect(page.locator("#col-comment")).toHaveAttribute(
      "data-state",
      "checked"
    );
    await page.keyboard.press("Escape");
    await headerCell(page, COMMENT);
  });
});
