import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { filterElements, openPill } from "./fixtures/filter-pill";
import { ORDER_PAGE } from "../components/features/order/order.page";
import type {
  FilterListValue,
  MagicFilter,
  MagicFilterEntry,
} from "../lib/rs/types";
import type { Page, Response } from "@playwright/test";

type Translate = UserFormat["t"];

/**
 * The customer and the project filter of the order book: Excel-like checklists of the customers and
 * projects that occur in orders (`OrderCustomerFilter`, `OrderProjectFilter`), loaded on demand from their
 * `valuesUrl` and narrowed by each other. Read-only — they pick from what the account sees and check that
 * the list request returns nothing but orders of the picks.
 */

const HEADERS = { "X-PF-Frontend": "next" };

/** A checklist filter, and the name of a list row's value it filters by. */
const FILTERS = [
  {
    field: "customers",
    url: "order/customerFilterValues",
    cell: (row: Row) => row.customer?.displayName,
  },
  {
    field: "projects",
    url: "order/projectFilterValues",
    cell: (row: Row) => row.project?.displayName,
  },
] as const;

type Row = {
  customer?: { displayName?: string } | null;
  project?: { displayName?: string } | null;
};

test.describe.configure({ timeout: 120_000 });

test.describe("order book checklist filters", { tag: "@lane-order" }, () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The filter is stored per user: start from none, so no other criterion narrows what is checked.
    await page.request
      .get("/rs/order/filter/reset", { headers: HEADERS })
      .catch(() => undefined);
  });

  for (const filter of FILTERS) {
    test(`filters the list to the ticked ${filter.field}`, async ({
      loggedInPage: page,
    }) => {
      const { t } = await userFormat(page);
      const offered = await filterValues(page, filter.url);
      test.skip(offered.length < 2, `Fewer than two ${filter.field}.`);
      const [first, second] = offered;

      const label = await openFilter(page, t, filter.field);
      // Listened for before ticking: the pill applies live, so the request may leave at once.
      const filtered = filteredListPage(page, filter.field, [
        first.id,
        second.id,
      ]);
      for (const pick of [first, second]) await tick(page, t, pick);
      const response = await filtered;
      await page.keyboard.press("Escape");

      const names = [first.displayName, second.displayName];
      const rows = (await response.json()).resultSet as Row[];
      expect(rows.length).toBeGreaterThan(0);
      for (const row of rows) expect(names).toContain(filter.cell(row));

      // The closed pill names both picks.
      const pill = page.getByRole("button", {
        name: t("filter.editEntry", { arg0: label }),
      });
      await expect(pill).toContainText(first.displayName);
      await expect(pill).toContainText(second.displayName);
    });
  }

  test("a ticked customer narrows the projects offered", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    const allProjects = await filterValues(page, FILTERS[1].url);
    // A customer whose orders do not cover every project, so the narrowing shows.
    let customer: FilterListValue | undefined;
    let expected: FilterListValue[] = [];
    for (const candidate of await filterValues(page, FILTERS[0].url)) {
      if (candidate.freeText) continue;
      expected = await filterValues(page, FILTERS[1].url, [
        { field: "customers", value: { values: [candidate.id] } },
      ]);
      if (expected.length > 0 && expected.length < allProjects.length) {
        customer = candidate;
        break;
      }
    }
    test.skip(!customer, "No customer with orders of only some projects.");

    await openFilter(page, t, "customers");
    await tick(page, t, customer!);
    await page.keyboard.press("Escape");

    // The project checklist asks with the customer as the other criterion, and offers that answer.
    const narrowed = page.waitForResponse(
      (response) =>
        response.url().endsWith(`/rs/${FILTERS[1].url}`) &&
        (
          response.request().postDataJSON() as { entries?: MagicFilterEntry[] }
        ).entries?.some((it) => it.field === "customers") === true
    );
    const label = await openFilter(page, t, "projects", false);
    const offered = (await (await narrowed).json()) as FilterListValue[];
    expect(offered.map((it) => it.id)).toEqual(expected.map((it) => it.id));
    await expect(
      page
        .getByRole("group", { name: label })
        .getByRole("checkbox", { name: expected[0].displayName, exact: true })
    ).toBeVisible();
  });
});

/** The values a checklist offers with [entries] as the other criteria, named and not free text first. */
async function filterValues(
  page: Page,
  url: string,
  entries: MagicFilterEntry[] = []
): Promise<FilterListValue[]> {
  const status = await page.request.get("/rs/userStatus", { headers: HEADERS });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  const response = await page.request.post(`/rs/${url}`, {
    headers: {
      ...HEADERS,
      "X-PF-CSRF-Token": csrfToken,
      "Content-Type": "application/json",
    },
    data: { entries },
  });
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as FilterListValue[]).filter(
    (it) => !it.freeText && it.displayName.trim()
  );
}

/** Opens the pill of [field] (on the list page first, if [navigate]); answers its label. */
async function openFilter(
  page: Page,
  t: Translate,
  field: string,
  navigate = true
): Promise<string> {
  if (navigate) {
    await goto(page, "/order");
    await expect(
      page.getByRole("heading", { name: t(ORDER_PAGE.titleKey) })
    ).toBeVisible({ timeout: 60_000 });
  }
  // The label is the backend's, as listMeta declares it — together with the on-demand values.
  const element = (await filterElements(page, "order")).find(
    (it) => it.id === field
  );
  expect(element?.valuesUrl).toBeTruthy();
  const label = element!.label!;
  await openPill(page, t, label);
  return label;
}

async function tick(page: Page, t: Translate, pick: FilterListValue) {
  await page
    .getByRole("searchbox", { name: t("select.search") })
    .fill(pick.displayName);
  await page
    .getByRole("checkbox", { name: pick.displayName, exact: true })
    .check();
}

/** The list request carrying all of [keys] as the criterion of [field]. */
function filteredListPage(
  page: Page,
  field: string,
  keys: string[]
): Promise<Response> {
  return page.waitForResponse((response) => {
    if (!response.url().endsWith("/rs/order/listPage")) return false;
    const body = response.request().postDataJSON() as
      | { filter?: MagicFilter }
      | undefined;
    const entry = body?.filter?.entries?.find((it) => it.field === field);
    const values = entry?.value?.values ?? [];
    return keys.every((key) => values.includes(key));
  });
}
