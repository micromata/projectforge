import type { Locator, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import { INVOICE_PAGE } from "../components/features/invoice/invoice.page";
import { MULTI_SELECTION_PARAM } from "../lib/rs/multi-select";

/**
 * The selection mode of the invoice list against the live backend: what it makes appear, whether the
 * keyboard works without a click first, and whether the ticks survive a reload and a change of the
 * filter — plus the collapsible that finally says *which* entries are picked.
 *
 * Read-only as far as the invoices go: the mass update itself is deliberately never run, since this is
 * a real database and the run has no undo beyond its Excel protocol. What *is* written is session state
 * (the registered and the ticked ids), which every case drops again by leaving the mode.
 *
 * The list of this database is a production ledger of thousands of invoices, so nothing here names one:
 * every assertion is about the first rows of whatever the list shows, identified by their number cell.
 */
test.describe("invoice selection mode", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // A criterion another run left behind would decide which rows the first page shows, and two cases
    // below compare the list before and after a filter change.
    await page.request
      .get(`/rs/${INVOICE_PAGE.entity}/filter/reset`, {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
    // And a selection another run left behind would restore itself into this one, mode and all (see
    // the selection store's `restore` and `listMeta.selectedIds`).
    await page.request
      .get(`/rs/${MASS_UPDATE.endpoint}/cancel`, {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the checkboxes only inside the mode", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openList(page);

    // Outside the mode there is nothing to tick, and the toggle says which mode the list is in.
    const toggle = modeToggle(page, format);
    await expect(toggle).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(0);

    await toggle.click();
    await expect(toggle).toHaveAttribute("aria-pressed", "true");
    // The header's checkbox, which covers the whole result set, and the bar that counts nothing yet.
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(1);
    await expect(selectionCount(page, format, 0)).toBeVisible();
    // Nothing to look at while nothing is ticked.
    await expect(panelTrigger(page, format)).toHaveCount(0);

    await toggle.click();
    await expect(toggle).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(0);
  });

  test("takes the arrow keys without a click first", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openList(page);
    await modeToggle(page, format).click();

    // Entering the mode focuses the table body and puts the keyboard on the first row, so Space ticks
    // it with no click having happened — the mode used to need one before the arrows did anything.
    await expect(page.locator("tbody tr.row-focused")).toHaveCount(1);
    await page.keyboard.press(" ");
    await expect(selectionCount(page, format, 1)).toBeVisible();
    // And the arrows move from there, Shift extending the range.
    await page.keyboard.press("Shift+ArrowDown");
    await page.keyboard.press("Shift+ArrowDown");
    await expect(selectionCount(page, format, 3)).toBeVisible();

    await modeToggle(page, format).click();
  });

  test("keeps the ticks across a reload and a change of the filter", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openList(page);
    await modeToggle(page, format).click();
    const picked = await tickFirstRows(page, 2);
    await expect(selectionCount(page, format, 2)).toBeVisible();

    // Restored from the session, which is what `listMeta.selectedIds` is served for: the store that
    // held them is gone after a reload.
    await page.reload();
    await expect(selectionCount(page, format, 2)).toBeVisible();
    for (const number of picked) {
      await expect(tickedRow(page, number), `row ${number}`).toHaveCount(1);
    }

    // A changed filter re-registers what may be picked, and that *replaces* the session context — so
    // the ticks have to be restated with it (see useListSelection, and `registerEntityIdsForSelection`
    // with its "Clear session"). Searching for something no invoice matches is the sharp case: the
    // ticked rows leave the result set, the selection must not.
    await search(page, format).fill(noMatch());
    await expect(numberCells(page)).toHaveCount(0);
    await expect(selectionCount(page, format, 2)).toBeVisible();
    await page.reload();
    await expect(selectionCount(page, format, 2)).toBeVisible();

    await modeToggle(page, format).click();
    await page.reload();
    // Nothing restored: leaving told the backend to forget the selection.
    await expect(modeToggle(page, format)).toHaveAttribute(
      "aria-pressed",
      "false"
    );
  });

  test("shows the picked entries in the collapsible of the list", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openList(page);
    await modeToggle(page, format).click();
    const picked = await tickFirstRows(page, 2);

    // Closed until asked, because its rows are a request of their own (`{page}/selectedList`).
    const trigger = panelTrigger(page, format);
    await expect(trigger).toBeVisible();
    // Radix keeps the content mounted and hidden rather than unmounting it, so "closed" is a statement
    // about visibility — but the fetch is what actually matters, and `enabled: open` holds it back.
    await expect(panelContent(page)).toBeHidden();
    await trigger.click();
    for (const number of picked) {
      await expect(panelCell(page, number), `row ${number}`).toHaveCount(1);
    }

    // And they are the *server's* answer rather than the list's own rows, which is the whole reason
    // this panel exists: under a filter that matches none of them, it still shows both.
    await search(page, format).fill(noMatch());
    await expect(numberCells(page)).toHaveCount(0);
    await expect(selectionCount(page, format, 2)).toBeVisible();
    for (const number of picked) {
      await expect(panelCell(page, number), `row ${number}`).toHaveCount(1);
    }

    await modeToggle(page, format).click();
  });

  test("opens in the mode when the url asks for it", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // How the backend links to a list that is opened *for* a mass update (`PagesResolver
    // .getMultiSelectionPageUrl`) — the mode must not have to be switched on by hand then.
    await openList(page, `?${MULTI_SELECTION_PARAM}=true`);
    await expect(modeToggle(page, format)).toHaveAttribute(
      "aria-pressed",
      "true"
    );
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(1);

    // And leaving is final, although the parameter is still in the url.
    await modeToggle(page, format).click();
    await expect(modeToggle(page, format)).toHaveAttribute(
      "aria-pressed",
      "false"
    );
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(0);
  });

  test("carries the selection to the mass update page", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await openList(page);
    await modeToggle(page, format).click();
    const picked = await tickFirstRows(page, 2);

    await page
      .getByRole("button", { name: format.t("massUpdate.button") })
      .click();
    await expect(page).toHaveURL(new RegExp(`${MASS_UPDATE.route}$`));
    await expect(selectionCount(page, format, 2)).toBeVisible();

    // The same panel as in the list, and here too the ids come from the session — so a reload of this
    // page, which holds no selection of its own at all, still knows them.
    await page.reload();
    await panelTrigger(page, format).click();
    for (const number of picked) {
      await expect(panelCell(page, number), `row ${number}`).toHaveCount(1);
    }

    // Out through the page's own cancel, which drops the selection and returns to the list.
    await page
      .getByRole("button", { name: format.t("cancel"), exact: true })
      .click();
    await expect(page).toHaveURL(new RegExp(`${INVOICE_PAGE.route}$`));
  });
});

/** Where the selection lives server-side, and where the mass update page is — off the declaration. */
const MASS_UPDATE = INVOICE_PAGE.massUpdate!;

async function openList(page: Page, query = ""): Promise<void> {
  await goto(page, `${INVOICE_PAGE.route}${query}`);
  // Rows that are *there*, not the loading skeleton: it renders a row per placeholder, so waiting for
  // a `tr` would enter the selection mode over an empty table — the mode focuses the first row as it
  // is switched on, and there would be none.
  await expect(numberCells(page).first()).toBeVisible();
}

/**
 * The list's own table — the one *outside* the collapsible, which sits above it and would otherwise be
 * the first `table` on the page.
 */
function listTable(page: Page): Locator {
  // Excluded by its ancestor rather than picked by position: the panel sits *above* the table, so once
  // it is open its own table is the first `table` on the page.
  return page.locator('table:not([data-slot="collapsible-content"] table)');
}

/** The invoice numbers of the rows on screen — digits only, which no skeleton cell is. */
function numberCells(page: Page): Locator {
  return listTable(page)
    .locator("tbody")
    .getByRole("cell")
    .filter({ hasText: /^\d+$/ });
}

/**
 * The toggle in the toolbar. Matched by its `aria-pressed`, because the bar's help icon carries the
 * same name: `multiselection.aggrid.selection.info.title` reads "Mehrfachauswahl" too.
 */
function modeToggle(page: Page, format: UserFormat): Locator {
  return page
    .getByRole("button", { name: format.t("multiselection.button") })
    .and(page.locator("[aria-pressed]"));
}

function search(page: Page, format: UserFormat): Locator {
  return page.getByPlaceholder(format.t("filter.searchList"));
}

/**
 * A search string no invoice can match, so the ticked rows leave the result set.
 *
 * One word without punctuation: the backend's search splits on it and matches a row on *any* token, so
 * "pf-e2e-no-such-invoice" hits every invoice whose subject holds the word "invoice".
 */
function noMatch(): string {
  return `zzqqxpfe2e${process.pid}`;
}

/** "Es sind N Einträge ausgewählt." — the heading of the bar, and of the mass update page. */
function selectionCount(
  page: Page,
  format: UserFormat,
  count: number
): Locator {
  return page.getByText(format.t("massUpdate.entriesFound", { arg0: count }));
}

/** The collapsible's own button; its name carries the count as well, hence the substring match. */
function panelTrigger(page: Page, format: UserFormat): Locator {
  return page.getByRole("button", {
    name: new RegExp(format.t("massUpdate.selectedEntries")),
  });
}

/** Radix renders the content only while the collapsible is open — which is what "closed" asserts on. */
function panelContent(page: Page): Locator {
  return page.locator('[data-slot="collapsible-content"]');
}

function panelCell(page: Page, text: string): Locator {
  return panelContent(page).getByRole("cell", { name: text, exact: true });
}

/**
 * Ticks the first `count` rows and answers their invoice numbers, which is what identifies them again
 * inside the panel — the list of this database is not fixed, so nothing may be spelled out.
 */
async function tickFirstRows(page: Page, count: number): Promise<string[]> {
  // The ticks are posted debounced, and a reload kills the page before any unmount flush could run —
  // so the write has to be seen landing, or a case that reloads would read the session as it was
  // before the last tick (see ListSelection and its DEBOUNCE_MS).
  // By the *number* of ids, because a `select` also goes out with the registration that entering the
  // mode triggered — and that one carries whatever was ticked before, which is nothing.
  const posted = page.waitForResponse((response) => {
    if (!response.url().endsWith(`/rs/${MASS_UPDATE.endpoint}/select`)) {
      return false;
    }
    const body = response.request().postDataJSON() as { selectedIds?: [] };
    return response.ok() && body?.selectedIds?.length === count;
  });
  const numbers: string[] = [];
  for (let index = 0; index < count; index++) {
    const row = listRows(listTable(page)).nth(index);
    // The second cell: the checkbox column leads every row inside the mode.
    numbers.push((await row.getByRole("cell").nth(1).innerText()).trim());
    await row.getByRole("checkbox").check();
  }
  await posted;
  return numbers;
}

function tickedRow(page: Page, number: string): Locator {
  return listRows(listTable(page))
    .filter({ has: page.getByRole("cell", { name: number, exact: true }) })
    .getByRole("checkbox", { checked: true });
}
