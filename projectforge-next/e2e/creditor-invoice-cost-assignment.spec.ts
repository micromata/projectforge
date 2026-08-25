import type { Page } from "@playwright/test";
import { test, expect, goto, login } from "./fixtures/auth";
import { hasRole } from "./fixtures/credentials";
import { typeNumber } from "./fixtures/form";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import {
  createCreditorInvoice,
  fetchFormDefaults,
  removeCreditorInvoice,
} from "./fixtures/creditor-invoice";
import { MARKER, uniqueSuffix } from "./fixtures/seed";
import { formatCurrency, formatPercentageDecimal } from "../lib/format";
import { formatNumberInput } from "../lib/number-parse";

/**
 * The cost split of an incoming invoice position against the live backend — the incoming sibling of the
 * form half of `invoice-cost-assignment.spec.ts`.
 *
 * The cost assignment is the *shared* component ([CostAssignmentsSection]), so its arithmetic is the
 * same on both invoices and asserted the same way: what a split adds up to against the position's net
 * sum, and the Fehlbetrag that is left over. What the incoming invoice does **not** have is a project,
 * so there is no cost-2-of-the-project preselection and no "cost unit not of this project" warning to
 * assert — those live on the outgoing side alone (see Kost2Warning). Nor is there a cost-assignment
 * difference column on the incoming list.
 *
 * Cost 1 and cost 2 are deliberately left unpicked: they are not mandatory (`KostZuweisungDO`), and what
 * is under test is the arithmetic between a position's net sum and its assignments — picking cost units
 * would add two autocompletes over the account's real chart of accounts and nothing to the assertion.
 *
 * Nothing is saved. The invoice is created through the API and marked deleted afterwards, and the
 * proposals under test are the form's, before any submit. Run as `finance-user`; the file skips where
 * the instance has no such account.
 */

const ROUTE = "/creditor-invoice";
const ROLE = "finance-user";

/** In the subject of every invoice these tests create, so a leftover is recognisable in the list. */
const SUBJECT = `${MARKER} creditor cost assignment (delete me)`;

/** One position of 1.000 € net — enough for a cost assignment to be proposed an amount. */
const POSITION = { number: 1, menge: 1, einzelNetto: 1000 };

test.describe("creditor invoice cost assignment form", () => {
  test.skip(
    !hasRole(ROLE),
    `No ${ROLE} account on this instance — see e2e/fixtures/credentials.ts.`
  );

  // Each case fills a form of many fields against a live backend, and the first navigation to a route
  // additionally waits for the dev server to compile it.
  test.describe.configure({ timeout: 120_000 });

  test.beforeEach(async ({ page }) => {
    await login(page, "/next/", ROLE);
  });

  test("follows the position shortfall while a cost assignment is added and removed", async ({
    page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await seedInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }]);
      await goto(page, `${ROUTE}/${id}`);

      // Nothing is assigned yet, so the whole net sum is missing — the banner's Fehlbetrag is the full
      // 1.000 € (shown only while non-zero; not negated, unlike a position's — see InvoiceSums).
      const fehlbetrag = sumValue(
        page,
        format,
        "fibu.rechnung.kostZuweisungFehlbetrag"
      );
      await expect(fehlbetrag).toHaveText(currency(format, 1000), {
        timeout: 60_000,
      });

      const row = await openStoredPosition(page, format);
      await row
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addKostZuweisung"),
        })
        .click();

      const netto = row.getByLabel(label(format, "fibu.common.netto"), {
        exact: true,
      });
      // Prefilled with what the position still has unassigned, which on the first row is all of it — so
      // the difference is closed by the click alone and the row reads as the whole position.
      await expect(netto).toHaveValue(amountInput(format, 1000));
      await expect(fehlbetrag).toHaveCount(0);
      await expect(row).toContainText(percent(format, 1));

      // Live, without a save: the sums come from `POST /rs/incomingInvoice/recalculate` over the form's
      // current state.
      await typeNumber(netto, "750");
      await netto.blur();
      await expect(row).toContainText(percent(format, 0.75));
      await expect(fehlbetrag).toHaveText(currency(format, 250));

      await typeNumber(netto, "1000");
      await netto.blur();
      // Gone once everything adds up — a permanent "0,00 €" would read as a complaint, so its absence
      // *is* the assertion that the difference closed.
      await expect(fehlbetrag).toHaveCount(0);

      // And back to the full net sum when the row is taken out again.
      await row
        .getByRole("button", {
          name: `${format.t("delete")}: ${format.t("fibu.rechnung.showKostZuweisungen")} 1`,
          exact: true,
        })
        .click();
      await expect(fehlbetrag).toHaveText(currency(format, 1000));
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });

  test("takes a percentage in the net amount and books that share of the position", async ({
    page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await seedInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }]);
      await goto(page, `${ROUTE}/${id}`);
      const row = await openStoredPosition(page, format);
      // The position's net sum first: a share is a share *of* it, and it is the server's answer (see
      // useInvoiceSums), debounced — a test that types faster than the first answer arrives would be
      // measuring the debounce.
      await expect(
        row.locator("dd").filter({
          hasText: formatCurrency(POSITION.einzelNetto, format.context),
        })
      ).toBeVisible();
      await row
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addKostZuweisung"),
        })
        .click();

      // The share entered instead of the amount — Wicket's `CurrencyConverter` with the position's net
      // sum as its total. Typed over the proposed amount rather than `fill`ed — see [typeNumber].
      const netto = row.getByLabel(label(format, "fibu.common.netto"), {
        exact: true,
      });
      await typeNumber(netto, "50%");
      // On blur the box writes the amount it stands for: half of the 1.000 € the position is worth, in
      // the account's own layout.
      await netto.blur();
      await expect(netto).toHaveValue(
        formatNumberInput(POSITION.einzelNetto / 2, format.context, 2, true)
      );
      // And the share beside it says the same thing, which is what was typed.
      await expect(
        row.getByText(formatPercentageDecimal(0.5, format.context, 0), {
          exact: true,
        })
      ).toBeVisible();
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });

  test("shows the cost split in the folded position header", async ({
    page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await seedInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }]);
      await goto(page, `${ROUTE}/${id}`);
      const row = positionRows(page, format).first();
      await expect(row).toBeVisible({ timeout: 60_000 });
      const header = row.locator('[data-slot="collapsible-trigger"]');

      // Add a cost assignment while the row is open, then fold it — the folded row has to pick up a
      // change made while it was open (see CostAssignmentsSummary).
      await header.click();
      await row
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addKostZuweisung"),
        })
        .click();
      const netto = row.getByLabel(label(format, "fibu.common.netto"), {
        exact: true,
      });
      await typeNumber(netto, "750");
      const comment = row.getByLabel(label(format, "comment"), { exact: true });
      await comment.fill(`${MARKER} split`);
      await comment.blur();
      await header.click();

      // The share is part of the folded reading too, in the order the open row has it: amount, share,
      // why (see CostAssignmentsSummary).
      await expect(header).toContainText(
        `${currency(format, 750)} · ${percent(format, 0.75)} · ${MARKER} split`
      );
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });

  test("prefills the VAT of a new position from the configuration", async ({
    page,
  }) => {
    const format = await userFormat(page);
    const { defaultVat } = await fetchFormDefaults(page);
    test.skip(
      defaultVat == null,
      "`fibu.defaultVAT` is not configured, so there is nothing to prefill."
    );
    let id: number | null = null;
    try {
      id = await seedInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }]);
      await goto(page, `${ROUTE}/${id}`);
      await expect(positionRows(page, format).first()).toBeVisible({
        timeout: 60_000,
      });

      // The *added* position, not the stored one: the stored one carries the VAT it was posted with,
      // and only a fresh row shows what the form proposes (from the row above, then `fibu.defaultVAT`).
      await page
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addPosition"),
        })
        .click();
      const rows = page.getByLabel(
        label(format, "fibu.rechnung.mehrwertSteuerSatz"),
        { exact: true }
      );
      // The predecessor's rate carries over (every position is taxed the same in all but the rare
      // case), and the seed posted 19 %. Entered as a percentage although the field holds a factor.
      await expect(rows.last()).toHaveValue(percentInput(format, 0.19));
    } finally {
      await removeCreditorInvoice(page, id);
    }
  });
});

/** The seed with this file's subject and a unique creditor filled in. */
function seedInvoice(
  page: Page,
  positions: {
    number: number;
    text: string;
    menge: number;
    einzelNetto: number;
  }[]
) {
  return createCreditorInvoice(page, positions, {
    betreff: SUBJECT,
    kreditor: `${MARKER} creditor ${uniqueSuffix()}`,
  });
}

/** An amount as the page writes it — through the app's own helper, so neither side is spelled out. */
function currency(format: UserFormat, value: unknown): string {
  return formatCurrency(value, format.context);
}

/** An amount as it stands in an input box at rest: grouped, but without the currency beside it. */
function amountInput(format: UserFormat, value: number): string {
  return formatNumberInput(value, format.context, 2, true);
}

/** A share as a row states it: whole percent in the user's layout (see CostAssignmentShare). */
function percent(format: UserFormat, value: number): string {
  return formatPercentageDecimal(value, format.context, 0);
}

/** A VAT rate as it stands in its box, from the factor the backend holds: 0.19 → "19,00". */
function percentInput(format: UserFormat, factor: number): string {
  return formatNumberInput(factor * 100, format.context, 2, true);
}

/** One entry of a sums line, addressed by the term above it (see InvoiceSumsLine). */
function sumValue(page: Page, format: UserFormat, key: string) {
  return page
    .locator("dl div", { has: page.getByText(format.t(key), { exact: true }) })
    .locator("dd")
    .first();
}

function positionRows(page: Page, format: UserFormat) {
  return page
    .locator("section", {
      has: page.getByText(format.t("fibu.rechnung.positions"), { exact: true }),
    })
    .locator('[data-slot="collapsible"]');
}

/**
 * The invoice's first position, unfolded: a stored one opens folded (see `PositionRow`), so its cost
 * assignments are not reachable before it is opened.
 */
async function openStoredPosition(page: Page, format: UserFormat) {
  const row = positionRows(page, format).first();
  await expect(row).toBeVisible({ timeout: 60_000 });
  await row.locator('[data-slot="collapsible-trigger"]').click();
  return row;
}
