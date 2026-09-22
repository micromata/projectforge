import type { Locator, Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat, type UserFormat } from "./fixtures/format";
import { listRows } from "./fixtures/list-table";
import { MULTI_SELECTION_PARAM } from "../lib/rs/multi-select";

/**
 * The selection mode of the incoming invoice list against the live backend — the incoming sibling of
 * `invoice-selection.spec.ts`: what it makes appear, whether the keyboard works without a click first,
 * whether the ticks survive a reload and a change of the filter, and the collapsible that finally says
 * *which* entries are picked.
 *
 * Read-only as far as the invoices go: the mass update itself is deliberately never run, since this is
 * a real database and the run has no undo beyond its Excel protocol. What *is* written is session state
 * (the registered and the ticked ids), which every case drops again by leaving the mode.
 *
 * The list of this database is a production ledger, so nothing here names one: rows are identified by
 * their `data-row-id` (the entity id `DataTableRow` sets), read at runtime, never spelled out — and a
 * creditor invoice has no invoice number to go by in the first place. Run as `finance-user`; the file
 * skips where the instance has no such account.
 */

/** The REST category, route and mass-update endpoint, spelled out — see invoice-cost-assignment.spec.ts. */
const ENTITY = "incomingInvoice";
const ROUTE = "/creditor-invoice";
const MASS_UPDATE = {
  endpoint: "incomingInvoiceSelected",
  route: `${ROUTE}/mass-update`,
};

const ROLE = "finance-user";

test.describe.configure({ timeout: 120_000 });

test.describe("creditor invoice selection mode", () => {
  test.skip(
    !hasRole(ROLE),
    `No ${ROLE} account on this instance — see e2e/fixtures/credentials.ts.`
  );

  test.beforeEach(async ({ page }) => {
    await login(page, "/next/", ROLE);
    // A criterion another run left behind would decide which rows the first page shows, and two cases
    // below compare the list before and after a filter change.
    await page.request
      .get(`/rs/${ENTITY}/filter/reset`, {
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

  test("shows the checkboxes only inside the mode", async ({ page }) => {
    const format = await userFormat(page);
    await openList(page);

    const toggle = modeToggle(page, format);
    await expect(toggle).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(0);

    await toggle.click();
    await expect(toggle).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(1);
    await expect(selectionCount(page, format, 0)).toBeVisible();
    // Nothing to look at while nothing is ticked.
    await expect(panelTrigger(page, format)).toHaveCount(0);

    await toggle.click();
    await expect(toggle).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("thead").getByRole("checkbox")).toHaveCount(0);
  });

  test("takes the arrow keys without a click first", async ({ page }) => {
    const format = await userFormat(page);
    await openList(page);
    await modeToggle(page, format).click();

    // Entering the mode focuses the table body and puts the keyboard on the first row, so Space ticks
    // it with no click having happened.
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
    page,
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
    for (const id of picked) {
      await expect(tickedRow(page, id), `row ${id}`).toHaveCount(1);
    }

    // A changed filter re-registers what may be picked, and that *replaces* the session context — so
    // the ticks have to be restated with it. Searching for something no invoice matches is the sharp
    // case: the ticked rows leave the result set, the selection must not.
    await search(page, format).fill(noMatch());
    await expect(listRows(listTable(page))).toHaveCount(0);
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
    page,
  }) => {
    const format = await userFormat(page);
    await openList(page);
    await modeToggle(page, format).click();
    const picked = await tickFirstRows(page, 2);

    // Closed until asked, because its rows are a request of their own (`{page}/selectedList`).
    const trigger = panelTrigger(page, format);
    await expect(trigger).toBeVisible();
    await expect(panelContent(page)).toBeHidden();
    await trigger.click();
    for (const id of picked) {
      await expect(panelRow(page, id), `row ${id}`).toHaveCount(1);
    }

    // And they are the *server's* answer rather than the list's own rows, which is the whole reason
    // this panel exists: under a filter that matches none of them, it still shows both.
    await search(page, format).fill(noMatch());
    await expect(listRows(listTable(page))).toHaveCount(0);
    await expect(selectionCount(page, format, 2)).toBeVisible();
    for (const id of picked) {
      await expect(panelRow(page, id), `row ${id}`).toHaveCount(1);
    }

    await modeToggle(page, format).click();
  });

  test("opens in the mode when the url asks for it", async ({ page }) => {
    const format = await userFormat(page);
    // How the backend links to a list opened *for* a mass update (`PagesResolver
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

  test("carries the selection to the mass update page", async ({ page }) => {
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
    for (const id of picked) {
      await expect(panelRow(page, id), `row ${id}`).toHaveCount(1);
    }

    // Out through the page's own cancel, which drops the selection and returns to the list.
    await page
      .getByRole("button", { name: format.t("cancel"), exact: true })
      .click();
    await expect(page).toHaveURL(new RegExp(`${ROUTE}$`));
  });
});

async function openList(page: Page, query = ""): Promise<void> {
  await goto(page, `${ROUTE}${query}`);
  // Rows that are *there*, not the loading skeleton: the mode focuses the first row as it is switched
  // on, and there would be none over an empty table.
  await expect(listRows(listTable(page)).first()).toBeVisible({
    timeout: 60_000,
  });
}

/**
 * The list's own table — the one *outside* the collapsible, which sits above it and would otherwise be
 * the first `table` on the page once the panel is open.
 */
function listTable(page: Page): Locator {
  return page.locator('table:not([data-slot="collapsible-content"] table)');
}

/**
 * The toggle in the toolbar. Matched by its `aria-pressed`, because the bar's help icon carries the
 * same name.
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
 * A search string no invoice can match, so the ticked rows leave the result set. One word without
 * punctuation: the backend's search splits on it and matches a row on *any* token.
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

/** The panel's row for a given entity id — the panel table carries `data-row-id` like the list does. */
function panelRow(page: Page, id: string): Locator {
  return panelContent(page).locator(`tr[data-row-id="${id}"]`);
}

/**
 * Ticks the first `count` rows and answers their entity ids, which is what identifies them again inside
 * the panel — the list of this database is not fixed, so nothing may be spelled out.
 */
async function tickFirstRows(page: Page, count: number): Promise<string[]> {
  // The ticks are posted debounced, and a reload kills the page before any unmount flush could run — so
  // the write has to be seen landing (by the *number* of ids, since a `select` also goes out with the
  // registration that entering the mode triggered, carrying whatever was ticked before, i.e. nothing).
  const posted = page.waitForResponse((response) => {
    if (!response.url().endsWith(`/rs/${MASS_UPDATE.endpoint}/select`)) {
      return false;
    }
    const body = response.request().postDataJSON() as { selectedIds?: [] };
    return response.ok() && body?.selectedIds?.length === count;
  });
  const ids: string[] = [];
  for (let index = 0; index < count; index++) {
    const row = listRows(listTable(page)).nth(index);
    const id = await row.getAttribute("data-row-id");
    if (id) ids.push(id);
    await row.getByRole("checkbox").check();
  }
  await posted;
  return ids;
}

function tickedRow(page: Page, id: string): Locator {
  return listTable(page)
    .locator(`tbody tr[data-row-id="${id}"]`)
    .getByRole("checkbox", { checked: true });
}
