import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import {
  fetchNextFreeProjectNumber,
  fetchNumberRanges,
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
  type SeededProject,
} from "./fixtures/seed";
import {
  formatCustomerNumber,
  formatProjectNumber,
  hasCustomers,
  maxLocalNumber,
  projectNumber,
  rangeForCustomer,
} from "../lib/number-ranges";
import type {
  NumberRange,
  NumberRangeMode,
  NumberRangesInfo,
} from "../lib/rs/number-ranges";

/**
 * Customer and project numbers in the mode of the number ranges the server runs with (see
 * fixtures/number-ranges.ts), one block per mode; the blocks of the other modes skip:
 *
 * - **FREE** (a fresh database, e.g. a slot of `bin/pfDev.sh`): free numbers — also for customers 0-999 and
 *   with cost units, which don't bring back the former ranges.
 * - **LEGACY** (a copy of an installation that used the former ranges 4 and 5, e.g. 8080): customer
 *   projects of range 5, internal ones of range 4.
 * - **CONFIGURED** (`projectforge.fibu.number-ranges.*`): only on a slot configured that way, e.g. with the
 *   ranges of `application-test.properties` (4 internal, 5 customers 0-999, 1 customers 10000-19999) in
 *   `~/ProjectForge-<n>/projectforge.properties`; then `bin/pfDev.sh run <n>` and
 *   `bin/pfDev.sh e2e <n> number-ranges`.
 *
 * No tag: switching cost units changes every form and list of the instance, so this runs isolated (see
 * playwright.config.ts), and the former value is restored at the end.
 *
 * Customers and projects created here are marked deleted at the end; deleted customers of the tests'
 * pools are recycled by the next run (see fixtures/seed.ts).
 */
test.describe("number ranges of customers and projects", () => {
  let costBefore: boolean;
  let mode: NumberRangeMode;
  const created: { entity: "customer" | "project"; id: number }[] = [];

  test.beforeAll(async ({ seedRequest }) => {
    costBefore = await isCostConfigured(seedRequest);
    mode = (await fetchNumberRanges(seedRequest)).mode;
  });

  test.afterAll(async ({ seedRequest }) => {
    // Projects first: a customer is the parent of its projects.
    for (const { entity, id } of [...created].reverse()) {
      await markAsDeleted(seedRequest, entity, id).catch(() => undefined);
    }
    await setCostConfigured(seedRequest, costBefore);
  });

  /** Skips the tests of a block unless the server runs in its mode. */
  const onlyIn = (expected: NumberRangeMode) =>
    test.beforeEach(() => {
      test.skip(
        mode !== expected,
        `The server's number ranges are ${mode}, not ${expected} (see the comment of this spec).`
      );
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

  /**
   * A new project for a customer of [pool] that has a project already: the form offers the next free number
   * of the customer's range, which the link takes.
   */
  const nextFreeProjectNumberIsOffered = async (
    page: Page,
    request: APIRequestContext,
    info: NumberRangesInfo,
    pool: CustomerPool | undefined,
    maxLocal: number
  ) => {
    const format = await userFormat(page);
    const project = await seedProject(request, pool, maxLocal);
    const next = await fetchNextFreeProjectNumber(request, project.customer.id);
    test.skip(next == null, "The customer's range is exhausted.");
    expect(next!.nummer).not.toBe(project.nummer);

    await goto(page, "/project/new");
    const local = localNumberField(page, format);
    await expect(local).toBeVisible({ timeout: 30_000 });
    await page
      .getByRole("combobox", {
        name: new RegExp(`^${escapeRegExp(label(format, "fibu.kunde._"))}`),
      })
      .first()
      .click();
    const popover = page
      .locator('[data-slot="popover-content"]:visible')
      .last();
    // A recycled customer is renamed: the search finds it once it is indexed under the new name.
    const option = popover.getByRole("option", {
      name: new RegExp(escapeRegExp(project.customer.suffix)),
    });
    await expect(async () => {
      await popover.locator('[data-slot="command-input"]').fill("");
      await popover
        .locator('[data-slot="command-input"]')
        .fill(project.customer.suffix);
      await expect(option.first()).toBeVisible({ timeout: 2_000 });
    }).toPass({ timeout: 30_000 });
    await option.first().click();
    await expect(
      page.getByText(
        `${formatCustomerNumber(info, project.customer.nummer)}.`,
        {
          exact: true,
        }
      )
    ).toBeVisible();
    const range = rangeForCustomer(info, project.customer.nummer)!;
    // The taken number of the seeded project: the link offers the free one instead (none while the
    // field holds the free one already).
    await local.fill(pad(project.nummer, range.projectDigits));
    await page
      .getByRole("button", {
        name: format.t("fibu.projekt.nextFreeNumber", {
          arg0: formatProjectNumber(info, next!.projektnummer),
        }),
        exact: true,
      })
      .click();
    await expect(local).toHaveValue(pad(next!.nummer, range.projectDigits));
    // Taken: the link is gone.
    await expect(
      page.getByRole("button", {
        name: messagePattern(format, "fibu.projekt.nextFreeNumber"),
      })
    ).toHaveCount(0);
  };

  test.describe("free numbers", () => {
    onlyIn("FREE");

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

    test("with cost units, a customer of 0-999 has free project numbers and no cost 2 units", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      // The former range 5 doesn't apply to an installation that never used it.
      await setCostConfigured(seedRequest, true);
      const info = await fetchNumberRanges(seedRequest);
      expect(info.mode).toBe("FREE");
      expect(info.ranges).toEqual([]);
      const format = await userFormat(page);
      const project = await seedProject(seedRequest);
      expect(project.customer.nummer).toBeLessThanOrEqual(999);
      const stored = await storedProject(seedRequest, project.id);
      // A free project has no local number of its own (ProjektDao.assignProjektnummer).
      expect(stored.nummer).toBe(stored.projektnummer);

      await goto(page, `/project/${project.id}`);
      await expect(freeNumberField(page, format)).toHaveValue(
        String(stored.projektnummer),
        { timeout: 30_000 }
      );
      await expect(localNumberField(page, format)).toHaveCount(0);
      // Cost 2 types are listed, but none can be created: the number isn't made of Kost2.
      await expect(
        page.getByText(format.t("fibu.kost2.error.projektWithoutNumberRange"))
      ).toBeVisible();
    });

    test("a project gets the next free project number, a taken one is rejected", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      const format = await userFormat(page);
      const project = await seedProject(seedRequest);
      const stored = await storedProject(seedRequest, project.id);
      expect(stored.projektnummer).toBeGreaterThan(0);

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
  });

  test.describe("former ranges 4 and 5", () => {
    onlyIn("LEGACY");

    test("they apply with and without cost units", async ({ seedRequest }) => {
      for (const on of [false, true]) {
        await setCostConfigured(seedRequest, on);
        const info = await fetchNumberRanges(seedRequest);
        expect(info.mode, `cost units ${on}`).toBe("LEGACY");
        expect(info.ranges.map((range) => range.prefix)).toEqual([4, 5]);
      }
    });

    test("a customer of 0-999 numbers its projects in range 5", async ({
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

    test("a new project is offered the next free number of its customer", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      await setCostConfigured(seedRequest, true);
      const info = await fetchNumberRanges(seedRequest);
      await nextFreeProjectNumberIsOffered(
        page,
        seedRequest,
        info,
        undefined,
        99
      );
    });

    test("an internal project is numbered in range 4, until a customer is picked", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      const format = await userFormat(page);
      const customer = await createCustomer(seedRequest);
      created.push({ entity: "customer", id: customer.id });
      // The form only: a saved internal project would take a number of range 4 for good.
      await goto(page, "/project/new");
      const local = localNumberField(page, format);
      await expect(local).toHaveAttribute("maxlength", "5", {
        timeout: 30_000,
      });
      await expect(page.getByText("4.", { exact: true })).toBeVisible();
      await expect(local).toHaveValue("00000");

      // The box of range 5 has two digits: the five of range 4 must not stay ("00000").
      await page
        .getByRole("combobox", {
          name: new RegExp(`^${escapeRegExp(label(format, "fibu.kunde._"))}`),
        })
        .first()
        .click();
      const popover = page
        .locator('[data-slot="popover-content"]:visible')
        .last();
      await popover
        .locator('[data-slot="command-input"]')
        .fill(customer.suffix);
      await popover.getByRole("option").first().click();
      await expect(
        page.getByText(`5.${pad(customer.nummer, 3)}.`, { exact: true })
      ).toBeVisible();
      await expect(local).toHaveAttribute("maxlength", "2");
      await expect(local).toHaveValue("00");
    });

    test("a customer beyond 999 has free project numbers", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
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
  });

  test.describe("configured number ranges", () => {
    onlyIn("CONFIGURED");
    test.beforeEach(async ({ seedRequest }) => {
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

    test("a new project is offered the next free number of its customer", async ({
      loggedInPage: page,
      seedRequest,
    }) => {
      const info = await fetchNumberRanges(seedRequest);
      const range = lastCustomerRange(info.ranges);
      test.skip(range == null, "No range with customers.");
      const pool = {
        high: range!.customerMax!,
        low: Math.max(range!.customerMin!, range!.customerMax! - 99),
      };
      await nextFreeProjectNumberIsOffered(
        page,
        seedRequest,
        info,
        pool,
        maxLocalNumber(range!)
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
