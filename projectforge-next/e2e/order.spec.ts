import { test, expect, goto } from "./fixtures/auth";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { formatCurrency, formatNumber } from "../lib/format";
import { AUFTRAG_METADATA } from "../lib/metadata/auftrag.generated";
import { AUFTRAGS_POSITION_METADATA } from "../lib/metadata/auftrags-position.generated";
import { ORDER_PAGE } from "../components/features/order/order.page";
import { columnHeaderKeyOf, columnIdOf } from "../lib/page-def/define-page";
import { findProjectWithCustomer, ownUserSearchTerm } from "./fixtures/seed";
import type { Page } from "@playwright/test";

/**
 * The order book against the live backend — the hard case of the migration (see MIGRATION.md): two
 * nested collections of unbounded length, sums the server computes from unsaved form state, and a
 * project that fills other fields in.
 *
 * Three of the cases write. Each of them creates its own order and marks it as deleted afterwards,
 * whatever happened in between: the database is real, and an order cannot be removed physically
 * (`AuftragDO` is historizable, so `forceDelete` is refused).
 *
 * The one behaviour worth naming here, because it is a data-loss risk rather than a display detail:
 * `AuftragDO.positionen` has `autoUpdateCollectionEntries` but no `@SoftDeleteCollection`, so
 * `CollectionHandler` **physically** removes a position a posted collection leaves out — history and
 * all. A removed row therefore has to be posted with `deleted = true`, which is what "keeps a removed
 * position as soft-deleted" verifies.
 */

/** Title of every order these tests create, so a leftover is recognisable in the list. */
const TITLE = "ZZ e2e order (delete me)";

// More than the default 30 s: every case here fills a form of dozens of fields against a live backend,
// and the first navigation to a route additionally waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

test.describe("order book", () => {
  test.beforeEach(async ({ loggedInPage: page }) => {
    // The filter and the grid state are stored per user, so a criterion or a hidden column left behind
    // by another run — or by someone working with the account — would otherwise decide what these tests
    // see. One call resets both: `AbstractEntityRest.resetListFilter` drops the grid state as well.
    await page.request
      .get("/rs/order/filter/reset", { headers: { "X-PF-Frontend": "next" } })
      .catch(() => undefined);
  });

  test("shows the declared columns under the labels of AuftragDO", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order");

    await waitForList(page, format.t);

    // Against the metadata, never against literals: a declared column takes its label from the
    // `i18nKey` of the field in AuftragDO, which is exactly what the declaration does not repeat. A
    // computed column names its own key, since `KundeDO`/`ProjektDO` and the transient sums have no
    // metadata entry at all.
    for (const column of ORDER_PAGE.columns) {
      const name = columnIdOf(column);
      const key = columnHeaderKeyOf(column, AUFTRAG_METADATA);
      await expect(
        // Anchored at the start rather than `exact`: the accessible name of a header includes its
        // filter button and its resize handle ("Nummer Filter Spaltenbreite ändern"), while a plain
        // substring match would let one label find another's column ("Kunde" in "Kundenreferenz").
        // Followed by whitespace, not by `\b`: a label may end in punctuation ("Anh."), and there is
        // no word boundary between a dot and the space after it.
        page.getByRole("columnheader", {
          name: new RegExp(`^${escapeRegExp(label(format, key))}(\\s|$)`),
        }),
        `column ${name}`
      ).toHaveCount(1);
    }
  });

  test("offers all four states of the invoiced filter", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);
    await goto(page, "/order");
    await waitForList(page, t);

    // `fakturiert` is one of the four synthetic filters of `OrderEntityRest` — a criterion with no
    // column behind it, whose options the layout carries. The chip is named "Filter <x> bearbeiten".
    await page
      .locator(
        `[aria-label="${t("filter.editEntry", { arg0: t("fibu.fakturiert") })}"]`
      )
      .click();

    // A LIST filter is a combobox ([ValueCombobox]), so its options are one click further in — the
    // point of that being that the field stays one line tall wherever it is shown.
    await page.locator("#filter-fakturiert").click();

    for (const key of [
      "fibu.auftrag.filter.type.all",
      "fibu.auftrag.filter.type.vollstaendigFakturiert",
      "fibu.auftrag.filter.type.zuFakturieren",
      "fibu.auftrag.filter.type.nochNichtVollstaendigFakturiert",
    ]) {
      await expect(
        page.getByRole("option", { name: t(key), exact: true }),
        key
      ).toHaveCount(1);
    }
  });

  test("computes the sums on the server while the form is being typed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    await fillHead(page, format);

    await addPosition(page, format, 0, { net: "1000", days: "3" });
    // The sums are the server's (`/rs/order/recalculate` on the unsaved form), so they are awaited
    // rather than computed here: which statuses count towards which sum is `OrderInfo`'s business, and
    // recomputing it in the test would only pin a second opinion.
    await expect(
      sumValue(page, format, "fibu.auftrag.nettoSumme._")
    ).toHaveText(currency(format, 1000));

    await addPosition(page, format, 1, { net: "2500", days: "5" });
    await expect(
      sumValue(page, format, "fibu.auftrag.nettoSumme._")
    ).toHaveText(currency(format, 3500));
    await expect(
      sumValue(page, format, "projectmanagement.personDays._")
    ).toHaveText(number(format, 8));

    // Removing a position takes it out of the sums at once, before any save — the regression this
    // guards is `OrderInfo.calculatePersonDays`, which used to count a deleted position although every
    // other sum skips it.
    await removePosition(page, format, 1);
    await expect(
      sumValue(page, format, "fibu.auftrag.nettoSumme._")
    ).toHaveText(currency(format, 1000));
    await expect(
      sumValue(page, format, "projectmanagement.personDays._")
    ).toHaveText(number(format, 3));
  });

  test("saves an order with two positions and a payment schedule", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      await goto(page, "/order/new");
      await fillHead(page, format);
      await addPosition(page, format, 0, { net: "1000", days: "3" });
      await addPosition(page, format, 1, { net: "2500", days: "5" });
      // Pointed at the second position, i.e. at one that does not exist in the database yet: the form
      // gives a new position a provisional number so it can be referred to, and the backend renumbers
      // position and instalment together on save.
      await addSchedule(page, format, { amount: "500", positionNumber: 2 });

      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();

      // A successful save leaves for the list, as every hand-built edit page does.
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });
      id = await findOrderId(page);
      expect(id, "the saved order must be findable through the API").not.toBe(
        null
      );

      const stored = await fetchOrder(page, id!);
      // Assigned by the backend on the first save (`AuftragDao.getNextNumber`) — the one field of this
      // form nobody types.
      expect(
        stored.nummer,
        "the order number is the backend's"
      ).toBeGreaterThan(0);
      expect(stored.positionen?.map((pos) => pos.number)).toEqual([1, 2]);
      expect(stored.positionen?.map((pos) => pos.nettoSumme)).toEqual([
        1000, 2500,
      ]);
      expect(stored.paymentSchedules?.map((s) => s.number)).toEqual([1]);
      // The instalment still points at the position it was pointed at — the numbers the backend handed
      // out happen to match the provisional ones here, which is exactly what must not be relied upon
      // silently: it is asserted against the position's stored number.
      expect(stored.paymentSchedules?.map((s) => s.positionNumber)).toEqual([
        stored.positionen?.[1].number,
      ]);
      expect(stored.nettoSumme).toBe(3500);
      expect(stored.personDays).toBe(8);
    } finally {
      await removeOrder(page, id);
    }
  });

  test("keeps a removed position as soft-deleted", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      await goto(page, "/order/new");
      await fillHead(page, format);
      await addPosition(page, format, 0, { net: "1000", days: "3" });
      await addPosition(page, format, 1, { net: "2500", days: "5" });
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });
      id = await findOrderId(page);
      expect(id).not.toBe(null);

      // Reopen and remove the second position, then save again.
      await goto(page, `/order/${id}`);
      await expect(positionRows(page, format)).toHaveCount(2);
      await removePosition(page, format, 1);
      await expect(positionRows(page, format)).toHaveCount(1);
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });

      const stored = await fetchOrder(page, id!);
      // Both rows are still there, the second one flagged — not removed from the table. Their numbers
      // are untouched too: `AuftragsPositionDO.equals` matches on (number, auftrag), so renumbering
      // would read to the collection handler as "removed and added".
      expect(
        stored.positionen?.map((pos) => [pos.number, pos.deleted === true])
      ).toEqual([
        [1, false],
        [2, true],
      ]);
    } finally {
      await removeOrder(page, id);
    }
  });

  test("takes a deleted instalment back with its own number", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      await goto(page, "/order/new");
      await fillHead(page, format);
      await addPosition(page, format, 0, { net: "1000", days: "3" });
      await addSchedule(page, format, { amount: "400" });
      await addSchedule(page, format, { amount: "600" });
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });
      id = await findOrderId(page);
      expect(id).not.toBe(null);

      // Delete the *first* instalment and save — the case a user hit: the row is gone, and a new one
      // would be #3, so #1 was unreachable for good.
      await goto(page, `/order/${id}`);
      await expect(scheduleRows(page, format)).toHaveCount(2);
      await removeSchedule(page, format, 1);
      await expect(scheduleRows(page, format)).toHaveCount(1);
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });

      // Reopen: only the live row is listed, and the toggle says how many are hidden.
      await goto(page, `/order/${id}`);
      await expect(scheduleRows(page, format)).toHaveCount(1);
      const reveal = page.getByRole("button", {
        name: `${format.t("deleted")} (1)`,
      });
      await expect(
        reveal,
        "a deleted row has to be reachable, or its number is spent"
      ).toBeVisible();
      await reveal.click();
      await page
        .getByRole("button", {
          name: `${format.t("undelete")}: ${format.t("fibu.auftrag.paymentschedule._")} 1`,
          exact: true,
        })
        .click();
      // Restored rows are live rows again, so the row count is what proves it took effect.
      await expect(scheduleRows(page, format)).toHaveCount(2);
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/order$/, { timeout: 30_000 });

      const stored = await fetchOrder(page, id!);
      // The point of the whole exercise: #1 is back — the same row, with the number and therefore the
      // history (`payment#1`) it always had. This is also what proves the backend writes `deleted = false`
      // back for a kept row; nothing in `CollectionHandler` treats the flag specially on that path.
      expect(
        stored.paymentSchedules?.map((s) => [s.number, s.deleted === true])
      ).toEqual([
        [1, false],
        [2, false],
      ]);
      expect(stored.paymentSchedules?.map((s) => s.amount)).toEqual([400, 600]);
    } finally {
      await removeOrder(page, id);
    }
  });

  test("fills customer and managers in from the project, without overwriting", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    await goto(page, "/order/new");
    // The form has to be there before a popover of it can be opened — the first navigation to this
    // route compiles it.
    await expect(
      page.getByLabel(label(format, "fibu.auftrag.title"), { exact: true })
    ).toBeVisible({ timeout: 60_000 });

    // A manager chosen by hand first — the autofill must leave it alone. An order may deliberately
    // name a stand-in (`fibu.auftrag.hint.kannVonProjektKundenAbweichen`).
    // The logged-in account's own name, so the lookup is certain to match without naming a person in
    // the source (see fixtures/seed.ts).
    const manager = await pickFirst(
      page,
      label(format, "fibu.projectManager"),
      await ownUserSearchTerm(page)
    );
    test.skip(
      manager === null,
      "no user matched the lookup, so there is nothing to overwrite"
    );
    // A whole project name rather than a syllable: `ProjektDao`'s lookup searches identifier, name
    // and customer, and a short term matches enough rows for the list to still be changing when it is
    // clicked. Read off the database at runtime — a project with a customer cannot be created here
    // (`KundeDO` has no generated id, see fixtures/seed.ts), and naming one would put a customer of
    // the production copy into the source.
    const withCustomer = await findProjectWithCustomer(page.request);
    test.skip(
      withCustomer === null,
      "the account sees no project with a customer (PM_PROJECT right, or an empty database)"
    );
    const project = await pickFirst(
      page,
      label(format, "fibu.projekt._"),
      withCustomer!.searchTerm
    );
    test.skip(project === null, "no project matched its own name");

    await expect(
      trigger(page, label(format, "fibu.projectManager")),
      "a manager chosen by hand is kept"
    ).toHaveText(manager!);
    // The empty ones are filled from what the project knows.
    await expect(trigger(page, label(format, "fibu.kunde._"))).not.toHaveText(
      format.t("filter.chooseEntity")
    );
  });

  test("refuses a position title longer than the column, naming the row", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // Through the API, not the form: the input's `maxLength` comes from the metadata
    // (`AuftragsPositionDO.titel` is 255), so the value under test cannot be typed at all. What is
    // verified is the server side rule and where it lands — `ValidationUtils` descends into nested
    // collections and prefixes the field with the row that caused it, for every entity alike.
    const maxLength = AUFTRAGS_POSITION_METADATA.fields.titel.maxLength;
    const response = await put(page, "/rs/order/saveorupdate", {
      data: {
        titel: TITLE,
        status: "IN_ERSTELLUNG",
        positionen: [
          {
            number: 1,
            status: "IN_ERSTELLUNG",
            titel: "x".repeat(maxLength + 1),
          },
        ],
      },
    });

    expect(response.status(), "a too long value is a validation error").toBe(
      406
    );
    const body = (await response.json()) as {
      validationErrors?: { fieldId?: string; message?: string }[];
    };
    expect(body.validationErrors?.map((e) => e.fieldId)).toContain(
      "positionen[0].titel"
    );
    expect(body.validationErrors?.[0]?.message).toBe(
      format.t("validation.error.maxLength", {
        arg0: label(format, "fibu.auftrag.title"),
        arg1: maxLength,
      })
    );
  });
});

/**
 * Waits until the list has arrived, generously: the dev server compiles a route on the first
 * navigation to it, which has nothing to do with what is under test.
 */
async function waitForList(page: Page, t: UserFormat["t"]) {
  await expect(
    page.getByRole("heading", { name: t(ORDER_PAGE.titleKey) })
  ).toBeVisible({ timeout: 60_000 });
}

/** Escapes what a label may contain that a regular expression would read as syntax ("Anh."). */
function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** An amount as the page writes it — through the app's own helper, so neither side is spelled out. */
function currency(format: UserFormat, value: number): string {
  return formatCurrency(value, format.context);
}

/** Person days: two digits, as OrderSumsLine renders them. */
function number(format: UserFormat, value: number): string {
  return formatNumber(value, format.context, 2);
}

/** One entry of the sums line, addressed by the term above it (see OrderSumsLine). */
function sumValue(page: Page, format: UserFormat, key: string) {
  return page
    .locator("dl div", { has: page.getByText(format.t(key), { exact: true }) })
    .locator("dd")
    .first();
}

/** The trigger of an autocomplete, whose accessible name is its `aria-label`. */
function trigger(page: Page, name: string) {
  return page.locator(`[aria-label="${name}"]`).first();
}

/** The position rows currently shown — the soft-deleted ones are not rendered. */
function positionRows(page: Page, format: UserFormat) {
  return rowsOf(page, format, "fibu.auftrag.positions");
}

/** The instalments currently shown, the soft-deleted ones again excluded. */
function scheduleRows(page: Page, format: UserFormat) {
  return rowsOf(page, format, "fibu.auftrag.paymentschedule._");
}

/**
 * The rows of the collection in the section with that title.
 *
 * By title rather than by the n-th section holding rows: a section whose rows are all deleted holds
 * none, and every index after it would then point at the wrong collection. A deleted row is not a
 * `Collapsible` at all — [RepeatableRow] renders it as a header only, so it has no field to tab into —
 * which is what makes this the count of the live rows.
 */
function rowsOf(page: Page, format: UserFormat, titleKey: string) {
  return page
    .locator("section", {
      has: page.getByText(format.t(titleKey), { exact: true }),
    })
    .locator('[data-slot="collapsible"]');
}

/** The head fields every saved order needs, plus the period the positions inherit. */
async function fillHead(page: Page, format: UserFormat) {
  const titel = page.getByLabel(label(format, "fibu.auftrag.title"), {
    exact: true,
  });
  await expect(
    titel,
    "the form has to be hydrated before it is filled"
  ).toBeVisible();
  await titel.fill(TITLE);
  // Mandatory as soon as a position inherits the period ("see above", the default) —
  // `PeriodOfPerformanceValidator`, which the metadata cannot express.
  const today = new Date(2026, 2, 1);
  await page
    .getByLabel(label(format, "fibu.periodOfPerformance.from"), { exact: true })
    .fill(format.date(today));
  await page
    .getByLabel(label(format, "fibu.periodOfPerformance.to"), { exact: true })
    .fill(format.date(new Date(2026, 5, 30)));
}

async function addPosition(
  page: Page,
  format: UserFormat,
  index: number,
  values: { net: string; days: string }
) {
  await page
    .getByRole("button", { name: format.t("fibu.auftrag.tooltip.addPosition") })
    .click();
  const row = positionRows(page, format).nth(index);
  await row
    .getByLabel(label(format, "fibu.auftrag.title"), { exact: true })
    .fill(`${TITLE} ${index + 1}`);
  await row
    .getByLabel(label(format, "fibu.auftrag.nettoSumme"), { exact: true })
    .fill(values.net);
  await row
    .getByLabel(label(format, "projectmanagement.personDays"), { exact: true })
    .fill(values.days);
  // Blur, so the number field hands its value to the form before the next step reads the sums.
  await row
    .getByLabel(label(format, "projectmanagement.personDays"), { exact: true })
    .blur();
}

/**
 * Removes a position and confirms the question that asks about it.
 *
 * The confirmation is the point, not an obstacle: a row is dropped without an undo and its button sits
 * beside the one that folds the row open, so [RepeatableRow] asks first. Scoped to the dialog, because
 * "Löschen" is also the name of the button that opened it.
 */
async function removePosition(page: Page, format: UserFormat, index: number) {
  await page
    .getByRole("button", {
      name: `${format.t("delete")}: ${TITLE} ${index + 1}`,
      exact: true,
    })
    .click();
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: format.t("delete"), exact: true })
    .click();
}

/**
 * Adds an instalment to the payment schedule and, when a position number is given, points it at that
 * position.
 *
 * The position is picked by *number*, which is what `PaymentScheduleDO.positionNumber` holds — and a
 * position of an unsaved order has to be pickable already, which is the regression this covers: the
 * select used to be empty on a new order, because a number was only assigned on save.
 */
async function addSchedule(
  page: Page,
  format: UserFormat,
  values: { amount: string; positionNumber?: number }
) {
  await page
    .getByRole("button", {
      name: format.t("fibu.auftrag.tooltip.addPaymentschedule"),
    })
    .click();
  const amount = page
    .getByLabel(label(format, "fibu.common.betrag"), { exact: true })
    .last();
  await amount.fill(values.amount);
  await amount.blur();
  if (values.positionNumber != null) {
    const position = page
      .getByRole("combobox", {
        name: new RegExp(
          `^${escapeRegExp(label(format, "fibu.auftrag.position"))}`
        ),
      })
      .last();
    await position.click();
    await page
      .getByRole("option", {
        name: new RegExp(
          `^${escapeRegExp(`${format.t("label.position.short")} ${values.positionNumber}`)}`
        ),
      })
      .click();
    await expect(position).toContainText(
      `${format.t("label.position.short")} ${values.positionNumber}`
    );
  }
}

/**
 * Removes the instalment with that number and confirms the question, as [removePosition] does.
 *
 * By number, not by index: the number is what a user sees in the row's header and what the row is
 * identified by afterwards — an instalment carries no title to name it with.
 */
async function removeSchedule(page: Page, format: UserFormat, number: number) {
  await page
    .getByRole("button", {
      name: `${format.t("delete")}: ${format.t("fibu.auftrag.paymentschedule._")} ${number}`,
      exact: true,
    })
    .click();
  await page
    .getByRole("alertdialog")
    .getByRole("button", { name: format.t("delete"), exact: true })
    .click();
}

/**
 * Picks the first hit of an autocomplete and returns what it says, or null when nothing matched.
 *
 * The trigger is a popover button, so its handle is the `aria-label`; the search box inside is a
 * `cmdk` input, which has to be typed into explicitly. Two characters at least — below `minChars` the
 * lookup does not fire (see EntityAutocomplete).
 */
async function pickFirst(
  page: Page,
  name: string,
  term: string
): Promise<string | null> {
  await trigger(page, name).click();
  // Scoped to the popover that is open, not simply the first search box on the page: a popover closed
  // a moment ago is still in the DOM, so on the second autocomplete of a test `.first()` would type
  // into the previous one — where nothing is listening and no lookup fires.
  const popover = page.locator('[data-slot="popover-content"]:visible').last();
  await popover.locator('[data-slot="command-input"]').fill(term);
  const options = popover.getByRole("option");
  const found = await options
    .first()
    .waitFor({ timeout: 10_000 })
    .then(() => true)
    .catch(() => false);
  if (!found) {
    await page.keyboard.press("Escape");
    return null;
  }
  // The list of hits is replaced once more when the debounced query settles, so the first option read
  // here may be detached by the time it is clicked. Addressed by its own text, and retried, so the
  // click lands on whatever list is current.
  let text = "";
  await expect(async () => {
    text = (await options.first().innerText()).trim();
    await popover
      .getByRole("option", { name: text, exact: true })
      .first()
      .click();
  }).toPass({ timeout: 20_000 });
  return text;
}

/** What the API answers for one order — only the parts these tests assert on. */
interface StoredOrder {
  id: number;
  nummer: number;
  titel?: string;
  nettoSumme?: number;
  personDays?: number;
  positionen?: { number: number; nettoSumme?: number; deleted?: boolean }[];
  paymentSchedules?: {
    number: number;
    positionNumber?: number;
    amount?: number;
    deleted?: boolean;
  }[];
}

/**
 * The id of the order these tests created, looked up by its title.
 *
 * Through the API rather than off the URL: a successful save navigates to the list, so the page never
 * shows the new id. The newest match wins — a leftover of an earlier run would otherwise be asserted
 * on and then deleted instead of this run's order.
 */
async function findOrderId(page: Page): Promise<number | null> {
  const response = await post(page, "/rs/order/list", {
    searchString: TITLE,
  });
  const body = (await response.json()) as { resultSet?: StoredOrder[] };
  const ids = (body.resultSet ?? [])
    .filter((order) => order.titel === TITLE)
    .map((order) => order.id);
  return ids.length ? Math.max(...ids) : null;
}

async function fetchOrder(page: Page, id: number): Promise<StoredOrder> {
  const response = await page.request.get(`/rs/order/${id}`, {
    headers: { "X-PF-Frontend": "next" },
  });
  return (await response.json()) as StoredOrder;
}

/**
 * Marks an order as deleted, so a run leaves nothing behind that the list shows.
 *
 * Not `forceDelete`: an order is historizable, so the physical delete is refused — marking it is as
 * far as this goes, and `AuftragFilter` hides deleted orders by default.
 */
async function removeOrder(page: Page, id: number | null) {
  if (id == null) return;
  const order = await fetchOrder(page, id);
  await page.request.delete("/rs/order/markAsDeleted", {
    headers: await writeHeaders(page),
    data: { data: order },
  });
}

async function put(page: Page, url: string, data: unknown) {
  return page.request.put(url, { headers: await writeHeaders(page), data });
}

async function post(page: Page, url: string, data: unknown) {
  return page.request.post(url, { headers: await writeHeaders(page), data });
}

/** The headers every state changing call needs — the CSRF token is read per call, not cached. */
async function writeHeaders(page: Page): Promise<Record<string, string>> {
  const status = await page.request.get("/rs/userStatus", {
    headers: { "X-PF-Frontend": "next" },
  });
  const { csrfToken } = (await status.json()) as { csrfToken: string };
  return {
    "X-PF-Frontend": "next",
    "X-PF-CSRF-Token": csrfToken,
    "Content-Type": "application/json",
  };
}
