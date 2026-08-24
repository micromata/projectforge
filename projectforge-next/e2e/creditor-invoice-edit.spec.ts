import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { typeNumber } from "./fixtures/form";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import { formatCurrency } from "../lib/format";
import { formatDateInput } from "../lib/date-parse";
import { MARKER, uniqueSuffix } from "./fixtures/seed";
import {
  createCreditorInvoice as seedInvoice,
  fetchCreditorInvoice,
  removeCreditorInvoice,
  type CreditorInvoiceOptions,
  type PostedPosition,
} from "./fixtures/creditor-invoice";
import type { Page } from "@playwright/test";

/**
 * The edit form of an incoming (creditor) invoice against the live backend — the incoming sibling of
 * `invoice-edit.spec.ts`, and a leaner one throughout. A creditor invoice has no number, status, type,
 * customer, project or period of performance ProjectForge assigns, and none of the outgoing form's
 * document machinery (e-invoice, Word template, invoice PDF, clone). What identifies it is its
 * *creditor* and *reference*, free text, beside the DATEV account.
 *
 * Every case works on an invoice it created itself and marks as deleted afterwards, whatever happened
 * in between. Two reasons it cannot read one of the database instead: the list here is a production
 * ledger, so no spec may name an invoice of it (see fixtures/seed.ts), and the cases below need a
 * *known* net sum to say anything about the sums line. Unlike the outgoing invoice there is no invoice
 * number to spend, so nothing has to be created "GEPLANT": a creditor invoice never gets one handed
 * out, and marking it deleted leaves nothing behind.
 *
 * Run as `finance-user`: the FIBU rights without the admin group are what an incoming invoice asks
 * for. The instance may have no such account (an older one, or the role pointed elsewhere) — then the
 * whole file skips rather than fails, per CLAUDE.md.
 */

/** The REST category and route, spelled out rather than imported — see invoice-cost-assignment.spec.ts. */
const ENTITY = "incomingInvoice";
const ROUTE = "/creditor-invoice";

/** In the subject of every invoice these tests create, so a leftover is recognisable in the list. */
const SUBJECT = `${MARKER} creditor invoice (delete me)`;

const ROLE = "finance-user";

// More than the default: each case fills a form of many fields against a live backend, and the first
// navigation to a route additionally waits for the dev server to compile it.
test.describe.configure({ timeout: 120_000 });

test.describe("creditor invoice edit", () => {
  test.skip(
    !hasRole(ROLE),
    `No ${ROLE} account on this instance — see e2e/fixtures/credentials.ts.`
  );

  test.beforeEach(async ({ page }) => {
    await login(page, "/next/", ROLE);
  });

  test("saves a creditor invoice built in the form, and reads it back", async ({
    page,
  }) => {
    const format = await userFormat(page);
    const kreditor = `${MARKER} creditor ${uniqueSuffix()}`;
    const referenz = `${MARKER}-ref-${uniqueSuffix()}`;
    let id: number | null = null;
    try {
      await goto(page, `${ROUTE}/new`);
      // The creditor is the form's autofocus field, so it is visible once the form has hydrated.
      const creditorField = page.getByLabel(label(format, "fibu.common.creditor"), {
        exact: true,
      });
      await expect(creditorField).toBeVisible({ timeout: 60_000 });

      await creditorField.fill(kreditor);
      await page
        .getByLabel(label(format, "fibu.common.reference"), { exact: true })
        .fill(referenz);
      await page
        .getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
        .fill(SUBJECT);
      const date = page.getByLabel(label(format, "fibu.rechnung.datum"), {
        exact: true,
      });
      await date.fill(dateInput(format, "2026-03-02"));
      await date.press("Enter");

      // One position of 2 × 1.000 € at 19 % — a known net sum (2.000 €) the reopened form can be held
      // against.
      await page
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addPosition"),
        })
        .click();
      const row = positionRows(page, format).first();
      await expect(row).toBeVisible();
      await row
        .getByLabel(label(format, "fibu.rechnung.text"), { exact: true })
        .fill(`${SUBJECT} 1`);
      await typeNumber(
        row.getByLabel(label(format, "fibu.rechnung.menge"), { exact: true }),
        "2"
      );
      await typeNumber(
        row.getByLabel(label(format, "fibu.rechnung.position.einzelNetto"), {
          exact: true,
        }),
        "1000"
      );
      await typeNumber(
        row.getByLabel(label(format, "fibu.rechnung.mehrwertSteuerSatz"), {
          exact: true,
        }),
        "19"
      );

      // The id is read off the save the endpoint answers, since a creditor invoice has no number to
      // find its row by — and the form leaves for the list once it is stored.
      const saved = page.waitForResponse(
        (response) =>
          response.url().includes(`/rs/${ENTITY}/saveorupdate`) &&
          response.ok()
      );
      await page
        .getByRole("button", { name: format.t("save"), exact: true })
        .click();
      const body = (await (await saved).json()) as {
        variables?: { id?: number };
      };
      id = body.variables?.id ?? null;
      expect(id, "the save returned no id").not.toBeNull();
      await expect(page).toHaveURL(new RegExp(`${ROUTE}$`));

      // Read back: the head fields round-trip, and the sums are the server's own (see below).
      const stored = await fetchCreditorInvoice(page, id!);
      await goto(page, `${ROUTE}/${id}`);
      await expect(
        page.getByLabel(label(format, "fibu.common.creditor"), { exact: true })
      ).toHaveValue(kreditor, { timeout: 60_000 });
      await expect(
        page.getByLabel(label(format, "fibu.common.reference"), { exact: true })
      ).toHaveValue(referenz);
      await expect(
        page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
      ).toHaveValue(SUBJECT);
      await expect(positionRows(page, format)).toHaveCount(1);

      // Against what the API says rather than a sum computed here: how a position is rounded before it
      // enters a sum is `RechnungCalculator`'s rule and German law. What is under test is that the
      // banner shows the *server's* numbers for the form as it stands.
      await expect(sumValue(page, format, "fibu.common.netto")).toHaveText(
        currency(format, stored.netSum)
      );
      await expect(sumValue(page, format, "fibu.common.brutto")).toHaveText(
        currency(format, stored.grossSum)
      );
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });

  test("starts a new invoice focused on its creditor", async ({ page }) => {
    const format = await userFormat(page);
    // Nothing is saved: where the cursor lands is the form's own doing (see the page's `autoFocus`).
    await goto(page, `${ROUTE}/new`);
    const creditor = page.getByLabel(label(format, "fibu.common.creditor"), {
      exact: true,
    });
    await expect(creditor).toBeVisible({ timeout: 60_000 });
    // The first thing a creditor invoice is written by — who it is from.
    await expect(creditor).toBeFocused();
  });

  test("follows the sums live while a position is edited", async ({ page }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      // One position of 1.000 € net at 19 %, so the banner starts at a known sum the change moves.
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 1000 },
      ]);
      await goto(page, `${ROUTE}/${id}`);

      const net = sumValue(page, format, "fibu.common.netto");
      await expect(net).toHaveText(currency(format, 1000), { timeout: 60_000 });

      // A stored position opens folded (see PositionRow), so it has to be unfolded before its fields
      // are reachable.
      const row = positionRows(page, format).first();
      await row.locator('[data-slot="collapsible-trigger"]').click();
      const einzelNetto = row.getByLabel(
        label(format, "fibu.rechnung.position.einzelNetto"),
        { exact: true }
      );
      await typeNumber(einzelNetto, "2000");
      await einzelNetto.blur();

      // Live, without a save: the sums come from `POST /rs/incomingInvoice/recalculate` over the
      // form's current state, which is the whole point of the endpoint. Net doubles to 2.000 €, gross
      // follows to 2.380 €.
      await expect(net).toHaveText(currency(format, 2000));
      await expect(sumValue(page, format, "fibu.common.brutto")).toHaveText(
        currency(format, 2380)
      );
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });

  test("reports an invoice left without positions instead of swallowing it", async ({
    page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [
        { number: 1, text: `${SUBJECT} 1`, menge: 1, einzelNetto: 100 },
      ]);
      await goto(page, `${ROUTE}/${id}`);
      await expect(positionRows(page, format)).toHaveCount(1, {
        timeout: 60_000,
      });

      // The row is soft-deleted rather than dropped (it is stored), so it still travels with the save —
      // but `EingangsrechnungDao.onInsertOrModify` counts only the live ones and refuses the invoice.
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

      // The regression this guards is what `CREDITOR_INVOICE_ARRAY_FIELDS` exists for: the backend
      // names `positionen` as the field, no `<form.Field>` is mounted for the collection itself, and
      // without the array names the message would be written into a slot nobody renders — a save that
      // silently does nothing (see applyServerValidationErrors).
      await expect(
        page.getByText(
          format.t("fibu.rechnung.error.rechnungHatKeinePositionen")
        )
      ).toBeVisible();
      // And the page stays, so the invoice can be repaired rather than lost.
      await expect(page).toHaveURL(new RegExp(`${ROUTE}/${id}$`));
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });
});

/** An amount as the page writes it — through the app's own helper, so neither side is spelled out. */
function currency(format: UserFormat, value: unknown): string {
  return formatCurrency(value, format.context);
}

/** One entry of a sums line, addressed by the term above it (see InvoiceSumsLine). */
function sumValue(page: Page, format: UserFormat, key: string) {
  return page
    .locator("dl div", { has: page.getByText(format.t(key), { exact: true }) })
    .locator("dd")
    .first();
}

/**
 * The position rows currently shown — the soft-deleted ones are not rendered as collapsibles (see the
 * outgoing invoice's `positionRows`), so this counts the live rows.
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
  options?: Omit<CreditorInvoiceOptions, "betreff" | "kreditor">
) {
  return seedInvoice(page, positions, {
    ...options,
    betreff: SUBJECT,
    kreditor: `${MARKER} creditor ${uniqueSuffix()}`,
  });
}

/**
 * A date as it stands in a date box — through the box's own formatter, not `format.date`: the latter
 * renders in the user's time zone, which would move an ISO day across a zone boundary.
 */
function dateInput(format: UserFormat, iso: string): string {
  return formatDateInput(iso, format.context);
}
