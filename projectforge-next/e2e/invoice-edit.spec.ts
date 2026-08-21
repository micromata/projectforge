import { test, expect, goto } from "./fixtures/auth";
import { typeNumber } from "./fixtures/form";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import {
  formatCurrency,
  formatNumber,
  formatPercentageDecimal,
} from "../lib/format";
import { formatDateInput } from "../lib/date-parse";
import { formatNumberInput } from "../lib/number-parse";
import { INVOICE_PAGE } from "../components/features/invoice/invoice.page";
import { MARKER, uniqueSuffix } from "./fixtures/seed";
import {
  createInvoice as seedInvoice,
  fetchInvoice,
  removeInvoice,
  type InvoiceOptions,
  type PostedPosition,
} from "./fixtures/invoice";
import type { Page } from "@playwright/test";

/**
 * The edit form of an outgoing invoice against the live backend — the deepest form of the migration:
 * positions, and inside each of them the cost assignments its net sum is split across (see
 * MIGRATION.md).
 *
 * Every case works on an invoice it created itself and marks as deleted afterwards, whatever happened
 * in between. Two reasons it cannot read one of the database instead: the list here is a production
 * ledger, so no spec may name an invoice of it (see fixtures/seed.ts), and the cases below need a
 * *known* net sum and a *known* cost assignment state to say anything about the Fehlbetrag.
 *
 * The invoices are created as GEPLANT on purpose: `RechnungDao` assigns an invoice number on the
 * transition out of it, and a number, once handed out, is spent — the ledger would gain a gap for every
 * run. A planned invoice has none, which is also what makes it removable without a trace in the
 * numbering.
 */

/** In the subject of every invoice these tests create, so a leftover is recognisable in the list. */
const SUBJECT = `${MARKER} invoice (delete me)`;

// More than the default: each case fills a form of dozens of fields against a live backend, and the
// first navigation to a route additionally waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

test.describe("outgoing invoice edit", () => {
  test("is what the list leads to, by a row click and by add", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // Its own subject, unlike the other cases: this one has to find *its* row in a ledger of thousands,
    // and the shared subject would match what an earlier run left behind as well.
    const subject = `${SUBJECT} ${uniqueSuffix()}`;
    let id: number | null = null;
    try {
      id = await seedInvoice(
        page,
        [{ number: 1, text: `${subject} 1`, menge: 1, einzelNetto: 100 }],
        { subject }
      );
      // A criterion another run left behind decides which rows the list shows, and the row of a planned
      // invoice has to be among them (see invoice-selection.spec.ts, which resets it for the same reason).
      await page.request
        .get(`/rs/${INVOICE_PAGE.entity}/filter/reset`, {
          headers: { "X-PF-Frontend": "next" },
        })
        .catch(() => undefined);
      await goto(page, "/invoice");

      // What the release switch decides: without it the backend answered `wa/outgoingInvoiceEdit?id=:id`
      // as the edit page and `useEditTargets` opened Wicket with a full page load. Both targets are one
      // decision — a list that opens the legacy form has to add there too — so both are asserted, and on
      // the `/next` prefix rather than on the route alone, since that is the half that used to be `/wa`.
      await expect(
        page.getByRole("link", { name: format.t("menu.addNewEntry") })
      ).toHaveAttribute("href", "/next/invoice/new");

      await page.getByPlaceholder(format.t("filter.searchList")).fill(subject);
      await page.getByRole("cell", { name: subject, exact: true }).click();

      await expect(page).toHaveURL(new RegExp(`/next/invoice/${id}$`));
      await expect(
        page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
      ).toHaveValue(subject, { timeout: 60_000 });
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("shows the head, the positions and the sums the server computed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 2, einzelNetto: 1000 },
        { number: 2, text: `${SUBJECT} 2`, menge: 1, einzelNetto: 500 },
      ]);
      const stored = await fetchInvoice(page, id);
      await goto(page, `/invoice/${id}`);

      await expect(
        page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
      ).toHaveValue(SUBJECT, { timeout: 60_000 });
      await expect(positionRows(page, format)).toHaveCount(2);

      // Against what the API says rather than against a sum computed here: how a position is rounded
      // before it enters a sum is `RechnungCalculator`'s rule and German law, and a second
      // multiplication in the test would only pin a second opinion. What is under test is that the
      // banner shows the *server's* numbers for the form as it stands.
      await expect(sumValue(page, format, "fibu.common.netto")).toHaveText(
        currency(format, stored.netSum)
      );
      await expect(sumValue(page, format, "fibu.common.brutto")).toHaveText(
        currency(format, stored.grossSum)
      );
      // Nothing is assigned to a cost unit yet, so the whole net sum is missing — the one number of
      // this line that means something is wrong. Not negated, unlike a position's (see `InvoiceSums`).
      await expect(
        sumValue(page, format, "fibu.rechnung.kostZuweisungFehlbetrag")
      ).toHaveText(currency(format, stored.netSum));
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("follows the Fehlbetrag while a cost assignment is added and removed", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      // One position of 2.000 € net, with nothing assigned — so the Fehlbetrag starts at the full sum
      // and the form has something to close.
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 2, einzelNetto: 1000 },
      ]);
      await goto(page, `/invoice/${id}`);

      const fehlbetrag = sumValue(
        page,
        format,
        "fibu.rechnung.kostZuweisungFehlbetrag"
      );
      await expect(fehlbetrag).toHaveText(currency(format, 2000), {
        timeout: 60_000,
      });

      // A stored position opens folded (see PositionRow), so the row has to be unfolded before its
      // cost assignments are reachable at all.
      const row = positionRows(page, format).first();
      await row.locator('[data-slot="collapsible-trigger"]').click();
      await row
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addKostZuweisung"),
        })
        .click();

      // Only the amount: cost 1 and cost 2 are not mandatory (`KostZuweisungDO`), and what is under
      // test is the arithmetic between the position's net sum and its assignments — picking cost units
      // would add two autocompletes and nothing to the assertion.
      const netto = row.getByLabel(label(format, "fibu.common.netto"), {
        exact: true,
      });

      // Prefilled with what the position still has unassigned, which on the first row is all of it —
      // so the difference is closed by the click alone and the row reads as the whole position.
      await expect(netto).toHaveValue(amountInput(format, 2000));
      await expect(fehlbetrag).toHaveCount(0);
      await expect(row).toContainText(percent(format, 1));

      await typeNumber(netto, "1500");
      await netto.blur();
      await expect(row).toContainText(percent(format, 0.75));

      // Live, without a save: the sums come from `POST /rs/outgoingInvoice/recalculate` over the form's
      // current state, which is the whole point of the endpoint.
      await expect(fehlbetrag).toHaveText(currency(format, 500));

      await typeNumber(netto, "2000");
      await netto.blur();
      // Gone once everything adds up — a permanent "0,00 €" would read as a complaint (see
      // InvoiceSumsLine), so its absence *is* the assertion that the difference closed.
      await expect(fehlbetrag).toHaveCount(0);

      // And back to the full net sum when the row is taken out again.
      await row
        .getByRole("button", {
          name: `${format.t("delete")}: ${format.t("fibu.rechnung.showKostZuweisungen")} 1`,
          exact: true,
        })
        .click();
      await expect(fehlbetrag).toHaveText(currency(format, 2000));
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("says everything a position holds while its row is folded", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 2, einzelNetto: 1000 },
      ]);
      const stored = await fetchInvoice(page, id);
      await goto(page, `/invoice/${id}`);

      // A stored row opens folded (see PositionRow) — so this is the state the assertions below are
      // about, and nothing is clicked open anywhere in this case.
      const row = positionRows(page, format).first();
      await expect(row).toBeVisible({ timeout: 60_000 });
      const header = row.locator('[data-slot="collapsible-trigger"]');

      // Quantity × unit price, the VAT rate *and* the two amounts it produces: a folded row that shows
      // only the net sum leaves the reader to multiply, which is what this guards against.
      await expect(header).toContainText(
        `${formatNumber(2, format.context, 2)} × ${currency(format, 1000)}`
      );
      await expect(header).toContainText(
        `${format.t("fibu.common.vatAmount")} ${currency(format, stored.positionen?.[0]?.vatAmountSum)}`
      );
      await expect(header).toContainText(
        `${format.t("fibu.common.brutto")} ${currency(format, stored.positionen?.[0]?.grossSum)}`
      );
      // "See above" rather than nothing: that the invoice's period applies here is a fact about the
      // position, not a default worth hiding.
      await expect(header).toContainText(
        format.t("fibu.periodOfPerformance.type.seeabove")
      );

      // And the cost split, which is a list of its own — added through the form, so the case also
      // covers that the folded row picks up a change made while it was open.
      await header.click();
      await row
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addKostZuweisung"),
        })
        .click();
      const netto = row.getByLabel(label(format, "fibu.common.netto"), {
        exact: true,
      });
      await typeNumber(netto, "1500");
      const comment = row.getByLabel(label(format, "comment"), { exact: true });
      await comment.fill(`${MARKER} split`);
      await comment.blur();
      await header.click();

      // The share is part of the folded reading too, in the order the open row has it: amount, share,
      // why (see CostAssignmentsSummary).
      await expect(header).toContainText(
        `${currency(format, 1500)} · ${percent(format, 0.75)} · ${MARKER} split`
      );
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("reads the payment target of a stored invoice off its dates, and lets nobody type it", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      // Due 30 days after the invoice's fixed `datum` of 2026-03-02, and nothing else: the day count
      // is `@Transient`, so this is all an invoice can store of its term (see fixtures/invoice.ts).
      id = await createInvoice(
        page,
        [{ number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 }],
        { faelligkeit: "2026-04-01" }
      );
      await goto(page, `/invoice/${id}`);

      const due = page.getByLabel(label(format, "fibu.rechnung.faelligkeit"), {
        exact: true,
      });
      await expect(due).toHaveValue(dateInput(format, "2026-04-01"), {
        timeout: 60_000,
      });

      // One box per label, the discount's day count carrying its own („Skonto Zahlungsziel",
      // `AbstractRechnungDO.discountZahlungsZielInTagen`): the two used to share the key of the due date's,
      // which put the same label twice into one grid.
      const term = paymentTerm(page, format);
      const discountTerm = discountPaymentTerm(page, format);
      await expect(term).toHaveCount(1);
      await expect(discountTerm).toHaveCount(1);

      // Derived from the two dates, although the payload carries no day count at all — that it stayed
      // empty here was the defect behind `toFormValues` deriving it (see PaymentTermsFields).
      await expect(term).toHaveValue(dayCount(format, 30));
      // And nothing to type into: the dates are what a stored invoice is judged by, the days the formula
      // they came out of. Wicket shows a read-only text here for the same reason.
      await expect(term).toBeDisabled();
      await expect(discountTerm).toBeDisabled();
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("keeps the payment target of a new invoice and the date it leads to on each other", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    // Nothing is saved here, so nothing has to be removed: the two directions are the form's own doing.
    await goto(page, "/invoice/new");
    const date = page.getByLabel(label(format, "fibu.rechnung.datum"), {
      exact: true,
    });
    await expect(date).toBeVisible({ timeout: 60_000 });
    // The day counts are counted from this one, so it comes first — a new invoice starts without it.
    await date.fill(dateInput(format, "2026-03-02"));
    await date.press("Enter");

    const due = page.getByLabel(label(format, "fibu.rechnung.faelligkeit"), {
      exact: true,
    });
    const term = paymentTerm(page, format);

    // A term typed in days moves the date the invoice will be judged by. The backend derives it only
    // while it is empty (`AuftragAndRechnungDaoHelper.onSaveOrModify`), so a term changed later would
    // otherwise leave the old date standing.
    await typeNumber(term, "10");
    await expect(due).toHaveValue(dateInput(format, "2026-03-12"));

    // The date rewrites the days, so what the two boxes say stays one term. Committed with Enter,
    // which also closes the calendar the field opens on focus (see DateInput).
    await due.fill(dateInput(format, "2026-03-04"));
    await due.press("Enter");
    await expect(term).toHaveValue(dayCount(format, 2));

    // An emptied box leaves the date standing: deleting the number is how one is retyped, and the
    // due date of an invoice must not fall over a keystroke.
    await typeNumber(term, "");
    await term.blur();
    await expect(due).toHaveValue(dateInput(format, "2026-03-04"));

    // The discount's pair is wired the same way, and starts empty because no discount date is set.
    const discountTerm = discountPaymentTerm(page, format);
    await expect(discountTerm).toHaveValue("");
    await typeNumber(discountTerm, "5");
    await expect(
      page.getByLabel(label(format, "fibu.rechnung.discountMaturity"), {
        exact: true,
      })
    ).toHaveValue(dateInput(format, "2026-03-07"));
  });

  test("marks a paid amount far from the gross sum, and saves it anyway", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 1000 },
      ]);
      // The server's gross sum, not one multiplied here: the VAT rate is the seed's and the rounding is
      // `RechnungCalculator`'s (see the case on the sums line above).
      const { grossSum } = await fetchInvoice(page, id);
      if (grossSum == null) {
        throw new Error("The seeded invoice came back without a gross sum");
      }
      await goto(page, `/invoice/${id}`);

      const paid = page.getByLabel(label(format, "fibu.rechnung.zahlBetrag"), {
        exact: true,
      });
      await expect(paid).toBeVisible({ timeout: 60_000 });
      const warning = page.locator('[data-tone="warning"]');

      // The date belongs to the amount, and the backend insists on it:
      // `AuftragAndRechnungDaoHelper.validateBezahlDatumAndZahlBetrag` refuses one without the other,
      // which would make the save below fail for a reason that has nothing to do with the warning.
      const paidDate = page.getByLabel(
        label(format, "fibu.rechnung.bezahlDatum"),
        { exact: true }
      );
      await paidDate.fill(dateInput(format, "2026-03-10"));
      await paidDate.press("Enter");

      // What the invoice asks for passes without a word.
      await typeNumber(paid, formatNumberInput(grossSum, format.context, 2));
      await paid.blur();
      await expect(warning).toHaveCount(0);

      // A digit too many — the keying error this exists for. The sums are debounced and fetched, so the
      // message arrives a moment later; naming the gross sum is what makes it checkable.
      await typeNumber(
        paid,
        formatNumberInput(grossSum * 10, format.context, 2)
      );
      await paid.blur();
      await expect(warning).toHaveText(
        format.t("fibu.rechnung.zahlBetrag.warning.deviation", {
          arg0: currency(format, grossSum),
        })
      );

      // And it is a hint, not a gate: the amount may well be right, so the save goes through and the
      // form leaves for the list.
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      await expect(page).toHaveURL(/\/invoice$/);
      expect((await fetchInvoice(page, id)).zahlBetrag).toBeCloseTo(
        grossSum * 10,
        2
      );

      // And it is there again when the invoice is opened later, without anybody typing into the field:
      // an amount that was saved wrong stays wrong, so what the form says about it cannot depend on the
      // box having been touched in this session.
      await goto(page, `/invoice/${id}`);
      await expect(warning).toHaveText(
        format.t("fibu.rechnung.zahlBetrag.warning.deviation", {
          arg0: currency(format, grossSum),
        }),
        { timeout: 60_000 }
      );
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("opens a clone as a new invoice, positions and unsaved edits included", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 2, einzelNetto: 1000 },
        { number: 2, text: `${SUBJECT} 2`, menge: 1, einzelNetto: 500 },
      ]);
      await goto(page, `/invoice/${id}`);

      const subject = page.getByLabel(label(format, "fibu.rechnung.betreff"), {
        exact: true,
      });
      await expect(subject).toHaveValue(SUBJECT, { timeout: 60_000 });
      await expect(positionRows(page, format)).toHaveCount(2);
      // Not saved: what the clone posts are the form's current values, so this edit has to travel —
      // the whole reason Wicket sets `ignoreErrorOnClone` (see lib/rs/entity.ts).
      await subject.fill(`${SUBJECT} edited`);
      await subject.blur();

      // The add page loads the backend's preset for a new invoice like any other add does, and that
      // answer is the moment the form could be reset onto it. Awaited before anything is asserted,
      // because every edit page of this entity is the *same* route (`invoice/[id]`, "new" being one of
      // its ids): until the preset has arrived the fields still hold the old form's values, and an
      // assertion made before it would pass without saying anything about the clone.
      const preset = page.waitForResponse(
        (response) =>
          response.url().includes(`/rs/${INVOICE_PAGE.entity}/newEntry`) &&
          response.ok()
      );
      await page
        .getByRole("button", { name: format.t("clone"), exact: true })
        .click();
      // `?clone=1` is what the add page reads the clone by (see usePendingClone), so it is part of
      // what a working clone looks like and not incidental to the navigation.
      await expect(page).toHaveURL(/\/invoice\/new\?clone=1$/);
      await preset;
      // The clone button needs a stored entry, so its disappearance says the page really has become
      // the add page — and, coming after the preset, that the form has been rebuilt for it.
      await expect(
        page.getByRole("button", { name: format.t("clone"), exact: true })
      ).toHaveCount(0);

      // What the clone is for: the preset did *not* win, the clone did.
      await expect(subject).toHaveValue(`${SUBJECT} edited`);
      await expect(positionRows(page, format)).toHaveCount(2);
      // Nothing is stored yet, so there is no number — `RechnungDao` hands one out on the save.
      await expect(
        page.getByLabel(label(format, "fibu.rechnung.nummer"), { exact: true })
      ).toHaveValue("");

      // And the *next* plain add starts from the backend's preset again, not from the clone: the
      // handover is read only under `?clone=1`, which this navigation does not carry.
      await goto(page, "/invoice");
      await goto(page, "/invoice/new");
      await expect(subject).toHaveValue("", { timeout: 60_000 });
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("reports an invoice left without positions instead of swallowing it", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 },
      ]);
      await goto(page, `/invoice/${id}`);
      await expect(positionRows(page, format)).toHaveCount(1, {
        timeout: 60_000,
      });

      // The row is soft-deleted rather than dropped (it is stored), so it still travels with the save —
      // but `RechnungDao.validate` counts only the live ones and refuses the invoice.
      await page
        .getByRole("button", {
          name: `${format.t("delete")}: ${SUBJECT} 1`,
          exact: true,
        })
        .click();
      await page
        .getByRole("alertdialog")
        .getByRole("button", { name: format.t("delete"), exact: true })
        .click();
      await expect(positionRows(page, format)).toHaveCount(0);

      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();

      // The regression this guards is what `arrayFieldNames` exists for: the backend names `positionen`
      // as the field, no `<form.Field>` is mounted for a collection itself, and without the array
      // names the message would be written into a slot nobody renders — a save that silently does
      // nothing (see applyServerValidationErrors).
      await expect(
        page.getByText(
          format.t("fibu.rechnung.error.rechnungHatKeinePositionen")
        )
      ).toBeVisible();
      // And the page stays, so the invoice can be repaired rather than lost.
      await expect(page).toHaveURL(new RegExp(`/invoice/${id}$`));
    } finally {
      await removeInvoice(page, id);
    }
  });
});

/** An amount as the page writes it — through the app's own helper, so neither side is spelled out. */
function currency(format: UserFormat, value: unknown): string {
  return formatCurrency(value, format.context);
}

/**
 * An amount as it stands in an input box at rest: grouped like a rendered one, but without the currency
 * beside it — that is the box's suffix, not part of its value (see formatNumberInput).
 */
function amountInput(format: UserFormat, value: number): string {
  return formatNumberInput(value, format.context, 2, true);
}

/** A share as a row states it: whole percent in the user's layout (see CostAssignmentShare). */
function percent(format: UserFormat, value: number): string {
  return formatPercentageDecimal(value, format.context, 0);
}

/** One entry of a sums line, addressed by the term above it (see InvoiceSumsLine). */
function sumValue(page: Page, format: UserFormat, key: string) {
  return page
    .locator("dl div", { has: page.getByText(format.t(key), { exact: true }) })
    .locator("dd")
    .first();
}

/**
 * The position rows currently shown — the soft-deleted ones are not rendered.
 *
 * A deleted row is no `Collapsible` at all ([RepeatableRow] renders it as a header only), which is
 * what makes this the count of the live rows. The cost assignments inside a row are not collapsibles
 * either ([CostAssignmentRow] is a flat line), so they cannot be counted by mistake.
 */
function positionRows(page: Page, format: UserFormat) {
  return page
    .locator("section", {
      has: page.getByText(format.t("fibu.rechnung.positions"), { exact: true }),
    })
    .locator('[data-slot="collapsible"]');
}

/** The seed with this file's subject filled in, so no case repeats it. */
function createInvoice(
  page: Page,
  positions: PostedPosition[],
  options?: Omit<InvoiceOptions, "subject">
) {
  return seedInvoice(page, positions, { ...options, subject: SUBJECT });
}

/**
 * A date as it stands in a date box — through the box's own formatter, not `format.date`: the latter
 * renders in the user's time zone, which would move an ISO day across a zone boundary.
 */
function dateInput(format: UserFormat, iso: string): string {
  return formatDateInput(iso, format.context);
}

/** A day count as its box writes it: a whole number, ungrouped (see formatNumberInput). */
function dayCount(format: UserFormat, days: number): string {
  return formatNumberInput(days, format.context);
}

/** Days from the invoice date to the due date (see PaymentTermsFields). */
function paymentTerm(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "fibu.rechnung.zahlungsZiel"), {
    exact: true,
  });
}

/** Days from the invoice date to the discount date — its own label, unlike [paymentTerm]'s. */
function discountPaymentTerm(page: Page, format: UserFormat) {
  return page.getByLabel(label(format, "fibu.rechnung.discountZahlungsZiel"), {
    exact: true,
  });
}
