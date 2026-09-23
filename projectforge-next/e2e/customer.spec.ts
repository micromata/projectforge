import type { APIRequestContext, Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { waitForRow } from "./fixtures/list-table";
import { markAsDeleted, type SeededCustomer } from "./fixtures/seed";
import { CUSTOMER_PAGE } from "../components/features/customer/customer.page";
import { KUNDE_METADATA } from "../lib/metadata/kunde.generated";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";

/**
 * The customer (Kunde) page of projectforge-next — list and form, both rendered from CUSTOMER_PAGE.
 *
 * Everything it asserts on is the customer `seededCustomer` created (see fixtures/seed.ts): the list
 * of this database is a production copy, so no customer of it may be named in the source, and on a
 * fresh database only the seeded one exists. The customer's number is the entity's user-assigned id
 * (`KundeDO.nummer`, 0..999); the seed probes the high end of the range for a free one.
 *
 * `KundeDO` is historizable, so the seeded customer cannot be removed cleanly — it is marked deleted
 * at the end (afterAll), which keeps the row but takes it out of every default list.
 */
test.describe("customer page", () => {
  let customer: SeededCustomer;

  test.beforeEach(async ({ loggedInPage: page, seededCustomer }) => {
    customer = seededCustomer;
    // The list filter is stored per user and per entity: a term or status left behind by another run
    // would decide whether the seeded customer is in the list at all.
    await page.request
      .get("/rs/customer/filter/reset", {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of KundeDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/customer");

    await expect(
      page.getByRole("heading", { name: format.t("fibu.kunde.title.list") })
    ).toBeVisible({ timeout: 30_000 });

    // Against the metadata, never against literals: every column label is the `i18nKey` of the field
    // in KundeDO (or the column's own `labelKey` for `konto`, a foreign DO the metadata cannot carry),
    // which is exactly what the declaration does not repeat.
    for (const column of CUSTOMER_PAGE.columns) {
      const key = columnHeaderKeyOf(column, KUNDE_METADATA);
      await expect(
        page.getByRole("columnheader", { name: label(format, key) }),
        `column ${columnIdOf(column)}`
      ).toHaveCount(1);
    }
  });

  test("narrows the list to the searched customer and opens it by its row", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/customer");
    // The run's own suffix, not the whole name: "ZZ e2e customer" is the name of every customer an
    // earlier run left, and the backend matches a row on any word of the term.
    await page
      .getByPlaceholder(format.t("filter.searchList"))
      .fill(customer.suffix);
    const row = await waitForRow(page, customer.name, 30_000);

    await row.click();

    await expect(page).toHaveURL(new RegExp(`/customer/${customer.id}$`));
    await expect(nameField(page, format)).toHaveValue(customer.name);
  });

  test("prefills the form with the stored customer", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const stored = await storedCustomer(page.request, customer.id);
    // A deep link rather than a click from the list: it is what proves the SPA shell map covers
    // `customer/[id]` — under `output: 'export'` an unknown route lands on Next's 404.
    await goto(page, `/customer/${customer.id}`);

    await expect(nameField(page, format)).toHaveValue(stored.name ?? "", {
      timeout: 30_000,
    });
    await expect(
      page.getByLabel(label(format, "description"), { exact: true })
    ).toHaveValue(stored.description ?? "");

    // The number is the entity's user-assigned id and must not change once assigned, so the form shows
    // it read-only on an existing customer (see CustomerNumberField). Both halves matter: it carries
    // the stored value, and it cannot be edited.
    const number = page.getByLabel(format.t("fibu.kunde.nummer"), {
      exact: true,
    });
    await expect(number).toHaveValue(String(customer.nummer));
    await expect(number).toBeDisabled();
  });

  test("saves a change and returns to the list", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const changed = `${customer.name} [pf-e2e]`;
    await goto(page, `/customer/${customer.id}`);
    const name = nameField(page, format);
    await expect(name).toHaveValue(customer.name, { timeout: 30_000 });

    await name.fill(changed);
    await page
      .getByRole("button", { name: format.t("save"), exact: true })
      .click();

    await expect(
      page.getByText(format.t("message.successfullChanged"))
    ).toBeVisible();
    await expect(page).toHaveURL(/\/customer$/);
    // Read back through the API: the assertion is on what was stored, not on what the list cached. A
    // hand built save posts the form's values *as* the DTO, so a value the form doesn't carry would be
    // cleared — the number (the id) must survive the round trip untouched.
    const after = await storedCustomer(page.request, customer.id);
    expect(after.name).toBe(changed);
    expect(after.nummer).toBe(customer.nummer);
  });

  test("filters the list by the status of a customer", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/customer");
    await page
      .getByPlaceholder(format.t("filter.searchList"))
      .fill(customer.suffix);
    await waitForRow(page, customer.name, 30_000);

    // `status` is a `defaultFilter`, so its pill is on the row without being added first — labelled
    // with the backend's own word for it (`CustomerPagesRest.addMagicFilterElements`).
    await page
      .getByRole("button", {
        name: format.t("filter.editEntry", { arg0: format.t("status") }),
      })
      .click();
    // The pill popover applies live and has no save button (see filter-pill-shell): picking a value
    // fires the list request by itself, so the wait is armed before the click and matched on the enum
    // it carries. The seeded customer is ACTIVE, so it survives its own filter.
    const request = page.waitForRequest(
      (candidate) =>
        candidate.url().includes("/rs/customer/list") &&
        candidate.method() === "POST" &&
        (candidate.postData() ?? "").includes("ACTIVE")
    );
    await page
      .locator('[data-slot="popover-content"]')
      .getByRole("option", {
        name: format.t("fibu.kunde.status.active"),
        exact: true,
      })
      .click();

    // `status` is a real enum property, so the standard magic filter matches on its name — a value in
    // the wrong shape would be dropped silently and the list would look right and simply not filter.
    const body = JSON.parse((await request).postData() ?? "{}") as {
      entries: { field: string; value: { values?: string[] } }[];
    };
    expect(
      body.entries.find((entry) => entry.field === "status")?.value.values
    ).toContain("ACTIVE");
    await waitForRow(page, customer.name, 30_000);
  });

  // The seeded customer occupies its number for good; mark it deleted so it leaves every default list,
  // and so a following run's search finds only its own row.
  test.afterAll(async ({ seedRequest, seededCustomer }) => {
    await markAsDeleted(seedRequest, "customer", seededCustomer.id).catch(
      () => undefined
    );
  });
});

/** The customer as its page's DTO — what a write has to be given in full. */
async function storedCustomer(
  request: APIRequestContext,
  id: number
): Promise<{ name?: string; nummer?: number; description?: string }> {
  const response = await request.get(`/rs/customer/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) {
    throw new Error(`Could not read customer ${id}: HTTP ${response.status()}`);
  }
  return await response.json();
}

/** The name field, by the label KundeDO gives it. */
function nameField(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "fibu.kunde.name"), { exact: true });
}
