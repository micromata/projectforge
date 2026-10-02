import { test, expect, goto } from "./fixtures/auth";
import { userFormat, type UserFormat } from "./fixtures/format";
import { filterElements, reopenPill } from "./fixtures/filter-pill";
import { ORDER_PAGE } from "../components/features/order/order.page";
import {
  INVOICE_ENTITY,
  INVOICE_PAGE,
} from "../components/features/invoice/invoice.page";
import { PROJECT_PAGE } from "../components/features/project/project.page";
import { COST2_PAGE } from "../components/features/cost2/cost2.page";
import {
  TIMESHEET_ENTITY,
  TIMESHEET_PAGE,
} from "../components/features/timesheet/timesheet.page";
import type {
  FilterListValue,
  MagicFilter,
  MagicFilterEntry,
} from "../lib/rs/types";
import type { Page, Response } from "@playwright/test";

type Translate = UserFormat["t"];

/**
 * The customer and the project checklists of the lists that name either (`CustomerChecklistFilter`,
 * `ProjectChecklistFilter`): Excel-like lists of the customers and projects that occur in the rows,
 * loaded on demand from their `valuesUrl` and narrowed by each other. Read-only — they pick from what the
 * account sees and check that the list request returns nothing but rows of the picks.
 */

const HEADERS = { "X-PF-Frontend": "next" };

type Row = Record<string, unknown> & {
  customer?: { id?: number; displayName?: string } | null;
  project?: {
    id?: number;
    displayName?: string;
    customer?: { id?: number } | null;
  } | null;
  kost2?: { id?: number } | null;
};

/**
 * A checklist, and what of a list row it filters by: the pick's display name where the row names the
 * customer or project (order, invoice), its key where the row carries the id instead.
 */
type Checklist = {
  field: "customers" | "projects";
  by: "displayName" | "id";
  cell: (
    row: Row,
    page: Page
  ) => Promise<string | undefined> | string | undefined;
};

const customerKey = (id?: number | null) =>
  id == null ? undefined : `k:${id}`;
const projectKey = (id?: number | null) => (id == null ? undefined : `${id}`);

const BY_NAME: Checklist[] = [
  {
    field: "customers",
    by: "displayName",
    cell: (row) => row.customer?.displayName,
  },
  {
    field: "projects",
    by: "displayName",
    cell: (row) => row.project?.displayName,
  },
];

const LISTS = [
  {
    entity: "order",
    route: "/order",
    titleKey: ORDER_PAGE.titleKey,
    tag: "@lane-order",
    checklists: BY_NAME,
  },
  {
    entity: INVOICE_ENTITY,
    route: "/invoice",
    titleKey: INVOICE_PAGE.titleKey,
    tag: "@lane-invoice",
    checklists: BY_NAME,
  },
  {
    entity: "project",
    route: "/project",
    titleKey: PROJECT_PAGE.titleKey,
    tag: "@lane-customer",
    checklists: [
      {
        field: "customers",
        by: "id",
        cell: (row) => customerKey(row.customer?.id),
      },
    ],
  },
  {
    entity: "cost2",
    route: "/cost2",
    titleKey: COST2_PAGE.titleKey,
    tag: "@lane-cost2",
    checklists: [
      {
        field: "customers",
        by: "id",
        cell: (row) => customerKey(row.project?.customer?.id),
      },
      {
        field: "projects",
        by: "id",
        cell: (row) => projectKey(row.project?.id),
      },
    ],
  },
  {
    entity: TIMESHEET_ENTITY,
    route: "/timesheet",
    titleKey: TIMESHEET_PAGE.titleKey,
    tag: "@lane-timesheet",
    checklists: [
      // A sheet's row carries only its cost 2; the project is the cost 2's.
      {
        field: "projects",
        by: "id",
        cell: async (row, page) =>
          projectKey(await projectOfKost2(page, row.kost2?.id)),
      },
    ],
  },
] satisfies {
  entity: string;
  route: string;
  titleKey: string;
  tag: string;
  checklists: Checklist[];
}[];

type List = (typeof LISTS)[number];

test.describe.configure({ timeout: 120_000 });

for (const list of LISTS) {
  test.describe(`${list.entity} checklist filters`, { tag: list.tag }, () => {
    test.beforeEach(async ({ loggedInPage: page }) => {
      // The filter is stored per user: start from the default, so no picked criterion narrows the check.
      await page.request
        .get(`/rs/${list.entity}/filter/reset`, { headers: HEADERS })
        .catch(() => undefined);
    });

    for (const checklist of list.checklists) {
      test(`filters the list to the ticked ${checklist.field}`, async ({
        loggedInPage: page,
      }) => {
        const { t } = await userFormat(page);
        const offered = await filterValues(
          page,
          valuesUrl(list, checklist.field)
        );
        test.skip(offered.length < 2, `Fewer than two ${checklist.field}.`);
        const [first, second] = offered;

        const label = await openFilter(page, t, list, checklist.field);
        // Listened for before ticking: the pill applies live, so the request may leave at once.
        const filtered = filteredListPage(page, list.entity, checklist.field, [
          first.id,
          second.id,
        ]);
        for (const pick of [first, second]) await tick(page, t, pick);
        const response = await filtered;
        await page.keyboard.press("Escape");

        const expected = [first, second].map((pick) => pick[checklist.by]);
        const rows = (await response.json()).resultSet as Row[];
        expect(rows.length).toBeGreaterThan(0);
        for (const row of rows)
          expect(expected).toContain(await checklist.cell(row, page));

        // The closed pill names both picks.
        const pill = page.getByRole("button", {
          name: t("filter.editEntry", { arg0: label }),
        });
        await expect(pill).toContainText(first.displayName);
        await expect(pill).toContainText(second.displayName);
      });
    }

    if (list.checklists.length === 2) {
      test("a ticked customer narrows the projects offered", async ({
        loggedInPage: page,
      }) => {
        const { t } = await userFormat(page);
        const projectsUrl = valuesUrl(list, "projects");
        const allProjects = await filterValues(page, projectsUrl);
        // A customer whose rows do not cover every project, so the narrowing shows.
        let customer: FilterListValue | undefined;
        let expected: FilterListValue[] = [];
        for (const candidate of await filterValues(
          page,
          valuesUrl(list, "customers")
        )) {
          expected = await filterValues(page, projectsUrl, [
            { field: "customers", value: { values: [candidate.id] } },
          ]);
          if (expected.length > 0 && expected.length < allProjects.length) {
            customer = candidate;
            break;
          }
        }
        test.skip(!customer, "No customer with rows of only some projects.");

        await openFilter(page, t, list, "customers");
        // Applied once the pill's edit settles; closing a new pill before that would drop it.
        const applied = filteredListPage(page, list.entity, "customers", [
          customer!.id,
        ]);
        await tick(page, t, customer!);
        await applied;
        await page.keyboard.press("Escape");

        // The project checklist asks with the customer as the other criterion, and offers that answer.
        const narrowed = page.waitForResponse(
          (response) =>
            response.url().endsWith(`/rs/${projectsUrl}`) &&
            (
              response.request().postDataJSON() as {
                entries?: MagicFilterEntry[];
              }
            ).entries?.some((it) => it.field === "customers") === true
        );
        const label = await openFilter(page, t, list, "projects", false);
        const offered = (await (await narrowed).json()) as FilterListValue[];
        expect(offered.map((it) => it.id)).toEqual(expected.map((it) => it.id));
        await expect(
          page.getByRole("group", { name: label }).getByRole("checkbox", {
            name: expected[0].displayName,
            exact: true,
          })
        ).toBeVisible();
      });
    }
  });
}

function valuesUrl(list: List, field: Checklist["field"]): string {
  return `${list.entity}/${field === "customers" ? "customer" : "project"}FilterValues`;
}

/**
 * The values a checklist offers with [entries] as the other criteria (the list's default filter is not
 * among them, so they may be more than the list shows); named and not free text only.
 */
async function filterValues(
  page: Page,
  url: string,
  entries: MagicFilterEntry[] = []
): Promise<FilterListValue[]> {
  const response = await page.request.post(`/rs/${url}`, {
    headers: await postHeaders(page),
    data: { entries },
  });
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as FilterListValue[]).filter(
    (it) => !it.freeText && it.displayName.trim()
  );
}

async function postHeaders(page: Page) {
  const status = await page.request.get("/rs/userStatus", { headers: HEADERS });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    ...HEADERS,
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}

const kost2Projects = new Map<number, Promise<number | undefined>>();

/** The project of a cost 2, as its own endpoint answers it. */
function projectOfKost2(
  page: Page,
  kost2Id?: number
): Promise<number | undefined> {
  if (kost2Id == null) return Promise.resolve(undefined);
  let project = kost2Projects.get(kost2Id);
  if (!project) {
    project = page.request
      .get(`/rs/cost2/${kost2Id}`, { headers: HEADERS })
      .then(async (response) => {
        expect(response.ok()).toBeTruthy();
        return ((await response.json()) as Row).project?.id;
      });
    kost2Projects.set(kost2Id, project);
  }
  return project;
}

/** Opens the pill of [field] (on the list page first, if [navigate]); answers its label. */
async function openFilter(
  page: Page,
  t: Translate,
  list: List,
  field: string,
  navigate = true
): Promise<string> {
  if (navigate) {
    await goto(page, list.route);
    await expect(
      page.getByRole("heading", { name: t(list.titleKey) })
    ).toBeVisible({
      timeout: 60_000,
    });
  }
  // The label is the backend's, as listMeta declares it — together with the on-demand values.
  const element = (await filterElements(page, list.entity)).find(
    (it) => it.id === field
  );
  expect(element?.valuesUrl).toBeTruthy();
  expect(element?.defaultFilter).toBe(true);
  const label = element!.label!;
  // Pinned, so the row shows the pill before anything is ticked.
  await reopenPill(page, t, label);
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
  entity: string,
  field: string,
  keys: string[]
): Promise<Response> {
  return page.waitForResponse((response) => {
    if (!response.url().endsWith(`/rs/${entity}/listPage`)) return false;
    const body = response.request().postDataJSON() as
      | { filter?: MagicFilter }
      | undefined;
    const entry = body?.filter?.entries?.find((it) => it.field === field);
    const values = entry?.value?.values ?? [];
    return keys.every((key) => values.includes(key));
  });
}
