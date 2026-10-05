import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { userFormat } from "./fixtures/format";
import { resetFilter } from "./fixtures/filter-pill";
import { ORDER_PAGE } from "../components/features/order/order.page";
import type { MagicFilter } from "../lib/rs/types";
import type { OrderStatisticsMeta } from "../lib/rs/order-statistics";
import type { Page } from "@playwright/test";

/**
 * The order statistics (`/orderStatistics`): forecast and contribution margin over a filter of their own.
 *
 * What has to hold across the modules: the order book's buttons hand its business units, customers and
 * projects over once (`?fromOrderBook=1`, `OrderStatisticsRest.getMeta`), every other criterion stays
 * behind, and nothing done on the statistics page reaches the order book's filter. The favorites are the
 * page's own. A user without the order right gets neither the menu entry nor the page.
 *
 * In this lane because it reads and writes the order book's stored filter.
 */

// The charts run the forecast pipeline on a real database, and the dev server compiles the route on top.
test.describe.configure({ timeout: 180_000 });

const HEADERS = { "X-PF-Frontend": "next" };

test.describe("order statistics", { tag: "@lane-order" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    await resetFilter(page, "order");
  });

  test("takes over the order book's customer once, and keeps its own filter", async ({
    loggedInPage: page,
    seededCustomer,
  }) => {
    const format = await userFormat(page);
    const customerKey = `k:${seededCustomer.nummer}`;
    // The order book's filter: a customer, which is handed over, and a search term, which is not.
    await storeOrderFilter(page, {
      entries: [{ field: "customers", value: { values: [customerKey] } }],
      sortProperties: [],
      searchString: "statistics-e2e",
    });

    await goto(page, "/order");
    await expect(
      page.getByRole("heading", { name: format.t(ORDER_PAGE.titleKey) })
    ).toBeVisible({ timeout: 60_000 });
    await page
      .getByRole("link", {
        name: format.t("fibu.auftrag.statistics.forecast"),
        exact: true,
      })
      .click();

    await expect(
      page.getByRole("heading", { name: format.t("menu.fibu.orderStatistics") })
    ).toBeVisible({ timeout: 60_000 });
    // Taken over once: the parameter is gone, so a reload doesn't take it over again.
    await expect(page).not.toHaveURL(/fromOrderBook/);
    const meta = await statisticsMeta(page);
    expect(meta.filter.entries).toEqual([
      expect.objectContaining({
        field: "customers",
        value: expect.objectContaining({ values: [customerKey] }),
      }),
    ]);
    expect(meta.filter.searchString ?? null).toBeNull();

    // A favorite of the page's own, then gone again: it must not show up among the order book's.
    const name = `e2e statistics ${Date.now()}`;
    await page
      .getByRole("button", { name: format.t("favorites._") })
      .first()
      .click();
    await page.getByPlaceholder(format.t("favorite.addNew")).fill(name);
    await page.keyboard.press("Enter");
    await expect
      .poll(async () =>
        (await statisticsMeta(page)).filterFavorites.map((it) => it.name)
      )
      .toContain(name);
    const favorite = (await statisticsMeta(page)).filterFavorites.find(
      (it) => it.name === name
    )!;
    try {
      const orderBook = await orderBookMeta(page);
      expect(orderBook.filterFavorites?.map((it) => it.name)).not.toContain(
        name
      );
      // The order book's own filter is untouched, search term included.
      expect(orderBook.filter?.searchString).toBe("statistics-e2e");
    } finally {
      await page.request.post(
        `/rs/orderStatistics/filter/delete?id=${favorite.id}`,
        { headers: await writeHeaders(page) }
      );
    }
  });

  test.describe("for a user without the order right", () => {
    test.skip(
      !hasRole("normalo-user"),
      "no normalo-user in this instance's testAccounts.txt"
    );

    test("offers neither the menu entry nor the page", async ({ page }) => {
      await login(page, "normalo-user");
      const menu = await page.request.get("/rs/menu", { headers: HEADERS });
      expect(JSON.stringify(await menu.json())).not.toContain(
        "next/orderStatistics"
      );
      const meta = await page.request.get("/rs/orderStatistics/meta", {
        headers: HEADERS,
      });
      expect(meta.ok()).toBe(false);

      const format = await userFormat(page);
      await goto(page, "/orderStatistics");
      await expect(
        page.getByText(format.t("access.exception.noAccess"))
      ).toBeVisible({ timeout: 60_000 });
    });
  });
});

/** The headers a state changing call needs — the CSRF token is read per call. */
async function writeHeaders(page: Page): Promise<Record<string, string>> {
  const status = await page.request.get("/rs/userStatus", { headers: HEADERS });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    ...HEADERS,
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}

/** Stores `filter` as the order book's current one, as a list request does. */
async function storeOrderFilter(page: Page, filter: MagicFilter) {
  const res = await page.request.post("/rs/order/list", {
    headers: await writeHeaders(page),
    data: filter,
  });
  expect(res.ok()).toBe(true);
}

async function statisticsMeta(page: Page): Promise<OrderStatisticsMeta> {
  const res = await page.request.get("/rs/orderStatistics/meta", {
    headers: HEADERS,
  });
  expect(res.ok()).toBe(true);
  return (await res.json()) as OrderStatisticsMeta;
}

async function orderBookMeta(page: Page) {
  const res = await page.request.get("/rs/order/listMeta", {
    headers: HEADERS,
  });
  expect(res.ok()).toBe(true);
  return (await res.json()) as {
    filter?: MagicFilter;
    filterFavorites?: { id: number; name: string }[];
  };
}
