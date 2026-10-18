import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import {
  fetchNumberRanges,
  hasConfiguredRanges,
  isCostConfigured,
  setCostConfigured,
} from "./fixtures/number-ranges";
import {
  createCustomer,
  createProject,
  fetchEntity,
  insert,
  markAsDeleted,
  MARKER,
  uniqueSuffix,
  type CustomerPool,
  type SeededCustomer,
  type SeededProject,
} from "./fixtures/seed";
import {
  formatCustomerNumber,
  hasCustomers,
  maxLocalNumber,
  projectNumber,
  rangeForCustomer,
} from "../lib/number-ranges";
import type { NumberRange } from "../lib/rs/number-ranges";

/**
 * Customer and project numbers in the modes of the number ranges (see fixtures/number-ranges.ts):
 *
 * - **without configured ranges** (the default of an installation): with cost units the former ranges 4
 *   and 5 apply (legacy), without them the numbers are free. Both are tested in one run, switching
 *   `fibu.costConfigured` — and so is a project saved free and edited after cost units were switched on,
 *   whose stored local number is still its project number (`ProjektDao.localNummer`).
 * - **with configured ranges** (`projectforge.fibu.number-ranges.*`, bound at startup): only on a slot
 *   configured that way, e.g. with the ranges of `application-test.properties` (4 internal, 5 customers
 *   0-999, 1 customers 10000-19999) in `~/ProjectForge-<n>/projectforge.properties`; then
 *   `bin/pfDev.sh run <n>` and `bin/pfDev.sh e2e <n> number-ranges`. Elsewhere this block skips.
 *
 * No tag: switching cost units changes every form and list of the instance, so this runs isolated (see
 * playwright.config.ts), and the former value is restored at the end.
 *
 * Customers and projects created here are marked deleted at the end; deleted customers of the tests'
 * pools are recycled by the next run (see fixtures/seed.ts).
 */
test.describe("number ranges of customers and projects", () => {
  let costBefore: boolean;
  let configured: boolean;
  const created: { entity: "customer" | "project"; id: number }[] = [];

  test.beforeAll(async ({ seedRequest }) => {
    costBefore = await isCostConfigured(seedRequest);
    configured = await hasConfiguredRanges(seedRequest);
  });

  test.afterAll(async ({ seedRequest }) => {
    // Projects first: a customer is the parent of its projects.
    for (const { entity, id } of [...created].reverse()) {
      await markAsDeleted(seedRequest, entity, id).catch(() => undefined);
    }
    await setCostConfigured(seedRequest, costBefore);
  });

  /** A customer of the pool with a project of a free local number (see [customerWithProject]). */
  const seedProject = async (
    request: APIRequestContext,
    pool?: CustomerPool,
    maxLocal?: number
  ) => {
    const project = await customerWithProject(request, pool, maxLocal);
    created.push(
      { entity: "customer", id: project.customer.id },
      { entity: "project", id: project.id }
    );
    return project;
  };

  test.describe("without configured number ranges", () => {
    test.beforeEach(() => {
      test.skip(
        configured,
        "Number ranges are configured (projectforge.fibu.number-ranges.*): no legacy and no free mode."
      );
    });

    test("without cost units, the forms offer free numbers and no cost 2 types", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      await setCostConfigured(seedRequest, false);
      const format = await userFormat(page);

      await goto(page, "/customer/new");
      await expect(
        page.getByLabel(format.t("fibu.kunde.nummer"), { exact: true })
      ).toBeEditable({ timeout: 30_000 });
      // No range, so no next free number of one.
      await expect(
        page.getByRole("button", {
          name: messagePattern(format, "fibu.kunde.nextFreeNumber"),
        })
      ).toHaveCount(0);

      await goto(page, "/project/new");
      await expect(freeNumberField(page, format)).toBeEditable({
        timeout: 30_000,
      });
      await expect(localNumberField(page, format)).toHaveCount(0);
      await expect(page.locator("[data-kost2-art]")).toHaveCount(0);
    });

    test("without cost units, a project gets the next free project number, a taken one is rejected", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      await setCostConfigured(seedRequest, false);
      const format = await userFormat(page);
      const project = await seedProject(seedRequest);
      const stored = await storedProject(seedRequest, project.id);
      expect(stored.projektnummer).toBeGreaterThan(0);
      // A free project has no local number of its own (ProjektDao.assignProjektnummer).
      expect(stored.nummer).toBe(stored.projektnummer);

      await goto(page, `/project/${project.id}`);
      await expect(freeNumberField(page, format)).toHaveValue(
        String(stored.projektnummer),
        { timeout: 30_000 }
      );

      await expect(
        insert(seedRequest, "project", {
          name: `${project.name} duplicate`,
          customer: { id: project.customer.id },
          projektnummer: stored.projektnummer,
          nummer: stored.projektnummer,
        })
      ).rejects.toThrow(
        messagePattern(format, "fibu.projekt.validation.projektnummerExists")
      );
    });

    test("with cost units, a customer of 0-999 numbers its projects in range 5", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      await setCostConfigured(seedRequest, true);
      const format = await userFormat(page);
      const project = await seedProject(seedRequest);
      const stored = await storedProject(seedRequest, project.id);
      expect(stored.nummer).toBe(project.nummer);
      expect(stored.projektnummer).toBe(
        500_000 + project.customer.nummer * 100 + project.nummer
      );

      await goto(page, `/project/${project.id}`);
      const local = localNumberField(page, format);
      await expect(local).toHaveValue(pad(project.nummer, 2), {
        timeout: 30_000,
      });
      await expect(local).toHaveAttribute("maxlength", "2");
      await expect(
        page.getByText(`5.${pad(project.customer.nummer, 3)}.`, {
          exact: true,
        })
      ).toBeVisible();
      await expect(page.getByText(".##", { exact: true })).toBeVisible();

      // Only 100 projects per customer.
      await expect(
        insert(seedRequest, "project", {
          name: `${project.name} out of range`,
          customer: { id: project.customer.id },
          nummer: 100,
        })
      ).rejects.toThrow(
        messagePattern(format, "fibu.projekt.validation.localNumberOutOfRange")
      );
    });

    test("with cost units, an internal project is numbered in range 4", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      await setCostConfigured(seedRequest, true);
      const format = await userFormat(page);
      // The form only: a saved internal project would take a number of range 4 for good.
      await goto(page, "/project/new");
      await expect(localNumberField(page, format)).toHaveAttribute(
        "maxlength",
        "5",
        { timeout: 30_000 }
      );
      await expect(page.getByText("4.", { exact: true })).toBeVisible();
    });

    test("with cost units, a customer beyond 999 has free project numbers", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      await setCostConfigured(seedRequest, true);
      const info = await fetchNumberRanges(seedRequest);
      test.skip(
        !info.allowFreeNumbers,
        "Free numbers are not allowed (projectforge.fibu.number-ranges.allow-free-numbers)."
      );
      const format = await userFormat(page);
      const project = await seedProject(seedRequest, FREE_CUSTOMER_POOL);
      const stored = await storedProject(seedRequest, project.id);
      expect(stored.nummer).toBe(stored.projektnummer);

      await goto(page, `/project/${project.id}`);
      await expect(freeNumberField(page, format)).toHaveValue(
        String(stored.projektnummer),
        { timeout: 30_000 }
      );
      await expect(localNumberField(page, format)).toHaveCount(0);
    });

    test("a project saved free shows and saves its local number once cost units are switched on", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      // Saved free: the project number is one of range 5 (no range applies yet), and so is the local number.
      await setCostConfigured(seedRequest, false);
      const format = await userFormat(page);
      const customer = await createCustomer(seedRequest);
      created.push({ entity: "customer", id: customer.id });
      const { id, name, local, projektnummer } = await insertFreeInRange5(
        seedRequest,
        customer
      );
      created.push({ entity: "project", id });

      // With cost units range 5 applies: the form shows the local number, not the project number.
      await setCostConfigured(seedRequest, true);
      await goto(page, `/project/${id}`);
      await expect(localNumberField(page, format)).toHaveValue(pad(local, 2), {
        timeout: 30_000,
      });
      // Save is enabled by a change only; the number itself is right already.
      await page
        .getByLabel(label(format, "fibu.projekt.name"), { exact: true })
        .fill(`${name} saved`);
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(
        page.getByText(format.t("message.successfullChanged"))
      ).toBeVisible();

      const stored = await storedProject(seedRequest, id);
      expect(stored.nummer).toBe(local);
      expect(stored.projektnummer).toBe(projektnummer);
    });
  });

  test.describe("with configured number ranges", () => {
    test.beforeEach(async ({ seedRequest }) => {
      test.skip(
        !configured,
        "No number ranges configured (projectforge.fibu.number-ranges.*), see the comment of this spec."
      );
      // The Kost2 part of the numbers is shown with cost units only.
      await setCostConfigured(seedRequest, true);
    });

    test("a new customer is offered the next free number of each range", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      const format = await userFormat(page);
      const ranges = (await fetchNumberRanges(seedRequest)).ranges.filter(
        (range) => hasCustomers(range) && range.nextFreeCustomerNumber != null
      );
      test.skip(ranges.length === 0, "No range has a free customer number.");

      await goto(page, "/customer/new");
      const number = page.getByLabel(format.t("fibu.kunde.nummer"), {
        exact: true,
      });
      await expect(number).toBeEditable({ timeout: 30_000 });
      for (const range of ranges) {
        const button = page.getByRole("button", {
          name: format.t("fibu.kunde.nextFreeNumber", {
            arg0: range.name ?? String(range.prefix),
            arg1: String(range.nextFreeCustomerNumber),
          }),
          exact: true,
        });
        await button.click();
        await expect(number).toHaveValue(String(range.nextFreeCustomerNumber));
      }
    });

    test("a customer of a range numbers its projects by the range", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      const format = await userFormat(page);
      const info = await fetchNumberRanges(seedRequest);
      const range = lastCustomerRange(info.ranges);
      test.skip(range == null, "No range with customers.");
      const pool = {
        high: range!.customerMax!,
        low: Math.max(range!.customerMin!, range!.customerMax! - 99),
      };
      const maxLocal = maxLocalNumber(range!);
      const project = await seedProject(seedRequest, pool, maxLocal);
      expect(rangeForCustomer(info, project.customer.nummer)).toEqual(range);
      const stored = await storedProject(seedRequest, project.id);
      expect(stored.nummer).toBe(project.nummer);
      expect(stored.projektnummer).toBe(
        projectNumber(range!, project.customer.nummer, project.nummer)
      );

      await goto(page, `/project/${project.id}`);
      const local = localNumberField(page, format);
      await expect(local).toHaveValue(
        pad(project.nummer, range!.projectDigits),
        {
          timeout: 30_000,
        }
      );
      await expect(local).toHaveAttribute(
        "maxlength",
        String(range!.projectDigits)
      );
      await expect(
        page.getByText(
          `${formatCustomerNumber(info, project.customer.nummer)}.`,
          { exact: true }
        )
      ).toBeVisible();

      // One more than the range has digits for.
      await expect(
        insert(seedRequest, "project", {
          name: `${project.name} out of range`,
          customer: { id: project.customer.id },
          nummer: maxLocal + 1,
        })
      ).rejects.toThrow(
        messagePattern(format, "fibu.projekt.validation.localNumberOutOfRange")
      );
    });

    test("a free project number must not lie in a range", async ({
      seedRequest,
    }) => {
      const range = lastCustomerRange(
        (await fetchNumberRanges(seedRequest)).ranges
      );
      test.skip(range == null, "No range with customers.");
      // Without customer: the project numbers of a range with customers are its customers' only.
      await expect(
        insert(seedRequest, "project", {
          name: `${MARKER} project ${uniqueSuffix()} in range`,
          projektnummer: projectNumber(range!, range!.customerMin!, 0),
        })
      ).rejects.toThrow(
        // Not in the frontend's catalog (fibu.numberRange.error.freeNumberInRange).
        /lies in a number range|liegt in einem Nummernkreis/
      );
    });

    test("a customer number outside the ranges is rejected unless free numbers are allowed", async ({
      seedRequest,
    }) => {
      const info = await fetchNumberRanges(seedRequest);
      test.skip(
        info.allowFreeNumbers,
        "Free numbers are allowed (projectforge.fibu.number-ranges.allow-free-numbers)."
      );
      const outside = FREE_CUSTOMER_POOL.high;
      test.skip(
        rangeForCustomer(info, outside) != null,
        `Customer ${outside} lies in a configured range.`
      );
      await expect(
        insert(seedRequest, "customer", {
          nummer: outside,
          name: `${MARKER} customer ${uniqueSuffix()}`,
        })
      ).rejects.toThrow(
        // Not in the frontend's catalog (fibu.numberRange.error.customerNotInRange).
        /belongs to no configured number range|gehört zu keinem konfigurierten Nummernkreis/
      );
    });
  });
});

/** Customers outside the former range 0-999, for free project numbers next to range 5. */
const FREE_CUSTOMER_POOL: CustomerPool = { high: 99_999, low: 99_900 };

interface StoredProject {
  nummer?: number;
  projektnummer?: number;
}

async function storedProject(
  request: APIRequestContext,
  id: number
): Promise<StoredProject> {
  return await fetchEntity<StoredProject>(request, "project", id);
}

/**
 * A customer of the pool with a project. A recycled customer may have used up the few local numbers of
 * its range (10 with one digit; every run takes one): then a fresh customer is taken.
 */
async function customerWithProject(
  request: APIRequestContext,
  pool?: CustomerPool,
  maxLocal?: number
): Promise<SeededProject> {
  const customer = await createCustomer(request, undefined, { pool });
  try {
    return await createProject(request, customer, undefined, maxLocal);
  } catch (cause) {
    if (!/Could not find a free project number/.test(String(cause))) {
      throw cause;
    }
    const fresh = await createCustomer(request, undefined, {
      pool,
      recycle: false,
    });
    return await createProject(request, fresh, undefined, maxLocal);
  }
}

/**
 * Inserts a project of the customer (of range 5's customers 0-999) as a free project, whose project
 * number lies in range 5 — the way it was saved before cost units were switched on. Probes the local
 * numbers from 99 down, as their project numbers stay taken by the projects of earlier runs.
 */
async function insertFreeInRange5(
  request: APIRequestContext,
  customer: SeededCustomer
): Promise<{
  id: number;
  name: string;
  local: number;
  projektnummer: number;
}> {
  const name = `${MARKER} project ${uniqueSuffix()}`;
  for (let local = 99; local >= 1; local--) {
    const projektnummer = 500_000 + customer.nummer * 100 + local;
    try {
      const id = await insert(request, "project", {
        name,
        customer: { id: customer.id },
        projektnummer,
        nummer: projektnummer,
      });
      return { id, name, local, projektnummer };
    } catch (cause) {
      // Taken by an earlier run's project (projektnummerExists, or the customer's local number).
      if (!/already|bereits|vergeben/i.test(String(cause))) {
        throw cause;
      }
    }
  }
  throw new Error(
    `No free project number of customer ${customer.nummer} in range 5.`
  );
}

/** The range with customers of the highest numbers, e.g. range 1 (10000-19999) of Micromata's. */
function lastCustomerRange(ranges: NumberRange[]): NumberRange | undefined {
  return ranges
    .filter(hasCustomers)
    .sort((a, b) => b.customerMax! - a.customerMax!)[0];
}

/** The free project number of a customer of no range (or of no range at all). */
function freeNumberField(page: Page, format: UserFormat) {
  return page.getByLabel(format.t("fibu.projekt.projektnummer"), {
    exact: true,
  });
}

/** The local number of a project of a range: the one segment of the number field. */
function localNumberField(page: Page, format: UserFormat) {
  const text = format.t("fibu.projekt.nummer");
  return page.getByLabel(`${text}: ${text}`, { exact: true });
}

/**
 * A message of the frontend's catalog with its arguments open: the backend formats a number argument
 * its own way (e.g. "599.901"), so only the text around the arguments is compared.
 */
function messagePattern(format: UserFormat, key: string): RegExp {
  const hole = "\u0001";
  const text = format.t(key, { arg0: hole, arg1: hole });
  return new RegExp(text.split(hole).map(escapeRegExp).join(".*"));
}

function pad(value: number, digits: number): string {
  return String(value).padStart(digits, "0");
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
