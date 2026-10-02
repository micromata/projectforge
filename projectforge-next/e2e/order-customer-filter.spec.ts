import { test, expect, goto } from "./fixtures/auth";
import { userFormat } from "./fixtures/format";
import { filterElements, openPill } from "./fixtures/filter-pill";
import { ORDER_PAGE } from "../components/features/order/order.page";
import type { FilterListValue, MagicFilter } from "../lib/rs/types";
import type { Page, Response } from "@playwright/test";

/**
 * The customer filter of the order book: an Excel-like checklist of the customers that occur in orders
 * (`OrderCustomerFilter`), loaded on demand from its `valuesUrl`. Read-only — it picks from what the
 * account sees and checks that the list request returns nothing but orders of the picked customers.
 */

const FIELD = "customers";

test.describe.configure({ timeout: 120_000 });

test.describe("order book customer filter", { tag: "@lane-order" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The filter is stored per user: start from none, so no other criterion narrows what is checked.
    await page.request
      .get("/rs/order/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("filters the list to the ticked customers", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const picks = await twoCustomers(page);
    test.skip(!picks, "The account sees fewer than two customers with orders.");
    const [first, second] = picks!;

    await goto(page, "/order");
    await expect(
      page.getByRole("heading", { name: t(ORDER_PAGE.titleKey) })
    ).toBeVisible({ timeout: 60_000 });
    // The label is the backend's, as listMeta declares it — together with the on-demand values.
    const field = (await filterElements(page, "order")).find(
      (it) => it.id === FIELD
    );
    expect(field?.valuesUrl).toBeTruthy();
    const label = field!.label!;
    await openPill(page, t, label);

    const search = page.getByRole("searchbox", { name: t("select.search") });
    // Listened for before ticking: the pill applies live, so the request may leave at once.
    const filtered = filteredListPage(page, [first.id, second.id]);
    for (const pick of [first, second]) {
      await search.fill(pick.displayName);
      await page
        .getByRole("checkbox", { name: pick.displayName, exact: true })
        .check();
    }
    const response = await filtered;
    await page.keyboard.press("Escape");

    const names = [first.displayName, second.displayName];
    const rows = (await response.json()).resultSet as {
      customer?: { displayName?: string } | null;
    }[];
    expect(rows.length).toBeGreaterThan(0);
    for (const row of rows) expect(names).toContain(row.customer?.displayName);

    // The closed pill names both picks.
    const pill = page.getByRole("button", {
      name: t("filter.editEntry", { arg0: label }),
    });
    await expect(pill).toContainText(first.displayName);
    await expect(pill).toContainText(second.displayName);
  });
});

/** Two customer entities (not free texts) the account's orders refer to. */
async function twoCustomers(
  page: Page
): Promise<[FilterListValue, FilterListValue] | null> {
  const response = await page.request.get("/rs/order/customerFilterValues", {
    headers: { "X-PF-Frontend": "next" },
  });
  expect(response.ok()).toBeTruthy();
  const values = ((await response.json()) as FilterListValue[]).filter(
    (it) => !it.freeText && it.displayName.trim()
  );
  return values.length >= 2 ? [values[0], values[1]] : null;
}

/** The list request carrying exactly [keys] as the customer criterion. */
function filteredListPage(page: Page, keys: string[]): Promise<Response> {
  return page.waitForResponse((response) => {
    if (!response.url().endsWith("/rs/order/listPage")) return false;
    const body = response.request().postDataJSON() as
      | { filter?: MagicFilter }
      | undefined;
    const entry = body?.filter?.entries?.find((it) => it.field === FIELD);
    const values = entry?.value?.values ?? [];
    return keys.every((key) => values.includes(key));
  });
}
