import type { Page } from "@playwright/test";
import { test, expect, goto } from "./fixtures/auth";
import { typeNumber } from "./fixtures/form";
import { label, userFormat, type UserFormat } from "./fixtures/format";
import {
  createInvoice,
  fetchFormDefaults,
  findProjectsWithKost2,
  removeInvoice,
} from "./fixtures/invoice";
import { listRows } from "./fixtures/list-table";
import { MARKER } from "./fixtures/seed";
import {
  formatCurrency,
  formatPercentageDecimal,
  type FormatContext,
} from "../lib/format";
import { formatNumberInput } from "../lib/number-parse";
import type { FilterElement } from "../lib/rs/types";

/**
 * Spelled out rather than imported from `invoice.page.tsx`: importing a page definition pulls the
 * whole component tree into Playwright's loader, and `@hugeicons/core-free-icons` is ESM-only, which
 * that loader cannot read (the same import breaks `audit-columns.spec.ts` today). Keys, not texts —
 * the wording still comes from the catalogs through `userFormat`.
 */
const ENTITY = "outgoingInvoice";
const ROUTE = "/invoice";
const TITLE_KEY = "fibu.rechnung.title.list";
/** Label and id of the difference column — see `Rechnung.kostZuweisungenFehlbetrag`. */
const LABEL_KEY = "fibu.rechnung.kostZuweisungFehlbetrag";
const COLUMN_ID = "kostZuweisungenFehlbetrag";
/**
 * Id of the synthetic completeness filter — `INCOMPLETE_FILTER` of `IncompleteInvoiceFilter`. Its label
 * is not spelled out as a key: it is not named anywhere in the next sources, so `NextI18nKeyScanner`
 * doesn't export it and the catalogs this test reads don't know it. The backend translates it and sends
 * it with the element, which is what the pill shows — so that is what the test compares against.
 */
const FILTER_ID = "incomplete";

/**
 * The cost assignment status of the invoice list against the live backend — what Wicket's
 * `showKostZuweisungStatus` checkbox was, split into the column and the completeness filter that grew
 * out of it.
 *
 * Only a live run can settle either half. The column shows a value the lean row has to carry
 * (`Rechnung.copyFrom4ListRow`, which fills it only where cost accounting is configured), and the
 * filter field exists only because `OutgoingInvoiceEntityRest.addMagicFilterElements` sends it — both
 * are contracts between two modules that no typecheck spans.
 *
 * Read-only: the column is switched on and off again and the filter is opened but never applied, so
 * the account's stored list state is left as it was found.
 */
test.describe("invoice cost assignment", () => {
  // The visibility under test is the declared one, so whatever a previous run stored is dropped first
  // (AbstractEntityRest.resetListFilter drops the grid state along with the filter).
  test.beforeEach(async ({ loggedInPage: page }) => {
    await page.request
      .get(`/rs/${ENTITY}/filter/reset`, {
        headers: { "X-PF-Frontend": "next" },
      })
      .catch(() => undefined);
  });

  test("offers the difference column hidden, and filled once switched on", async ({
    loggedInPage: page,
  }) => {
    const { t, context } = await userFormat(page);
    await goto(page, ROUTE);
    await expect(page.getByRole("heading", { name: t(TITLE_KEY) })).toBeVisible(
      { timeout: 60_000 }
    );

    const label = t(LABEL_KEY);
    const header = page.getByRole("columnheader", {
      name: new RegExp(`^${escape(label)}(\\s|$)`),
    });
    // Declared but hidden: the amount is read by whoever books the costs and is noise to everyone else.
    await expect(header).toHaveCount(0);

    // Matched on the whole label: "Spalten" is a prefix of the panel's own "Spalten zurücksetzen".
    const panel = page.getByRole("button", {
      name: t("columns._"),
      exact: true,
    });
    await panel.click();
    const checkbox = page.locator(`#col-${COLUMN_ID}`);
    await expect(checkbox).toHaveAttribute("data-state", "unchecked");
    await checkbox.click();
    await page.keyboard.press("Escape");
    await expect(header).toHaveCount(1);

    // And it arrives filled rather than empty — the value has to be on the lean row. Matched by the
    // shape the account's own formatter produces, never by a spelled-out "0,00 €".
    const column = await columnIndex(page, label);
    const cell = listRows(page).first().locator("td").nth(column);
    await expect(cell).toHaveText(amountPattern(context));

    // Left as found: the reset returns to the declared visibility, which is this column hidden again.
    await panel.click();
    await page.getByRole("button", { name: t("columns.reset") }).click();
    await page.keyboard.press("Escape");
    await expect(header).toHaveCount(0);
  });

  test("offers the completeness filter as a permanent checkbox", async ({
    loggedInPage: page,
  }) => {
    const { t } = await userFormat(page);

    // The field is the backend's, so the test asks it first: an installation that neither keeps cost
    // accounting nor expects an account sends none, and there is nothing to look for in the UI then.
    const elements = await filterElements(page, ENTITY);
    const element = elements.find((e) => e.id === FILTER_ID);
    test.skip(
      !element,
      "Neither cost accounting nor a required account is configured, so the filter is not offered."
    );
    expect(element?.filterType).toBe("BOOLEAN");
    expect(element?.defaultFilter).toBe(true);
    const label = element?.label ?? "";
    expect(label).not.toBe("");

    await goto(page, ROUTE);
    await expect(page.getByRole("heading", { name: t(TITLE_KEY) })).toBeVisible(
      { timeout: 60_000 }
    );

    // A pill of its own rather than a field behind the picker: `defaultFilter` keeps it in the filter
    // bar, since whether anything is still missing is a standing question of whoever books the invoices.
    const pill = page.getByRole("button", {
      name: t("filter.editEntry", { arg0: label }),
    });
    await expect(pill).toBeVisible();
    await pill.click();
    await expect(page.locator(`#filter-${FILTER_ID}`)).toBeVisible();
    // Not applied — the stored filter of the account stays untouched.
    await page.keyboard.press("Escape");
  });
});

/** In the subject of every invoice the form cases create, so a leftover is recognisable in the list. */
const SUBJECT = `${MARKER} cost assignment (delete me)`;

/**
 * What a *new* cost assignment starts with, and what it says when it books outside the invoice's
 * project — the two halves of `RechnungCostEditTablePanel.newKostZuweisung` and of
 * `RechnungEditForm.onRenderCostRow`.
 *
 * Both need a project with cost units, which only the live database has (a cost unit's number is part
 * of a chart of accounts and cannot be invented, see fixtures/invoice.ts). Where the account sees none,
 * the cases skip rather than fail: the code they cover is then unreachable in this installation too.
 *
 * Nothing is saved. The invoice is created through the API and marked deleted afterwards, and the
 * proposals under test are the form's, before any submit.
 */
test.describe("invoice cost assignment form", () => {
  // Each case fills a form of dozens of fields against a live backend, and the first navigation to a
  // route additionally waits for the dev server to compile it.
  test.describe.configure({ timeout: 120_000 });

  test("prefills the VAT of a new position from the configuration", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const { defaultVat } = await fetchFormDefaults(page);
    test.skip(
      defaultVat == null,
      "`fibu.defaultVAT` is not configured, so there is nothing to prefill."
    );
    let id: number | null = null;
    try {
      id = await createInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }], {
        subject: SUBJECT,
      });
      await goto(page, `/invoice/${id}`);
      await expect(
        page.getByLabel(label(format, "fibu.rechnung.betreff"), { exact: true })
      ).toHaveValue(SUBJECT, { timeout: 60_000 });

      // The *added* position, not the stored one: the stored one carries the VAT it was posted with,
      // and only a fresh row shows what the form proposes.
      await page
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addPosition"),
        })
        .click();
      const rows = page.getByLabel(
        label(format, "fibu.rechnung.mehrwertSteuerSatz"),
        { exact: true }
      );
      // Entered as a percentage although the field holds a factor (see NumberField.percent) — so the
      // expectation is the backend's factor turned into one, never a spelled-out "19".
      await expect(rows.last()).toHaveValue(
        percentInput(format, defaultVat as number)
      );
    } finally {
      await removeInvoice(page, id);
    }
  });

  test("takes a percentage in the net amount and books that share of the position", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    let id: number | null = null;
    try {
      id = await createInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }], {
        subject: SUBJECT,
      });
      await goto(page, `/invoice/${id}`);
      const row = await openStoredPosition(page, format);
      // The net sum first: a share is a share *of* it, and it is the server's answer (see
      // useInvoiceSums), debounced. A user reads it before splitting the position; a test that types
      // faster than the first answer arrives would be measuring the debounce.
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
      // sum as its total, and the reason this case exists at all (it was the one thing the migrated
      // form could not do).
      const netto = row.getByLabel(label(format, "fibu.common.netto"), {
        exact: true,
      });
      // Typed over the proposed amount rather than `fill`ed — see [typeNumber], which is why.
      await typeNumber(netto, "50%");
      // On blur the box writes the amount it stands for, as the converter does after Wicket's ajax:
      // half of the 1.000 € the position is worth, in the account's own layout.
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
      await removeInvoice(page, id);
    }
  });

  test("proposes a cost unit of the invoice's project, and warns about a foreign one", async ({
    loggedInPage: page,
  }) => {
    const format = await userFormat(page);
    const projects = await findProjectsWithKost2(page);
    test.skip(
      projects === null,
      "The account sees fewer than two projects with active cost units."
    );
    const { first, second } = projects!;
    let id: number | null = null;
    try {
      id = await createInvoice(page, [{ ...POSITION, text: `${SUBJECT} 1` }], {
        subject: SUBJECT,
        projectId: first.id,
      });
      await goto(page, `/invoice/${id}`);

      const row = await openStoredPosition(page, format);
      await row
        .getByRole("button", {
          name: format.t("fibu.rechnung.tooltip.addKostZuweisung"),
        })
        .click();

      // Prefilled with the first active cost unit of the invoice's project — the expectation comes
      // from the backend's own answer, so no cost unit number enters the source.
      const kost2 = row.getByRole("combobox", {
        name: label(format, "fibu.kost2"),
      });
      await expect(kost2).toHaveText(first.kost2[0].displayName);
      // And nothing to complain about, since that is by definition a cost unit of the project.
      const warning = row.getByRole("button", {
        name: format.t("fibu.kost.error.kost2NotOfProject"),
      });
      await expect(warning).toHaveCount(0);

      // Picked from another project: allowed (Wicket saves it too) but marked, and the mark is what
      // this case is about. Searched by the cost unit's own name, which the backend just supplied.
      await kost2.click();
      const popover = page.locator('[data-slot="popover-content"]');
      await popover
        .getByPlaceholder(format.t("filter.search"))
        .fill(searchTerm(second.kost2[0].displayName));
      const suggestion = page.getByRole("listbox").getByRole("option").first();
      await expect(suggestion).toBeVisible();
      await suggestion.click();

      await expect(warning).toHaveCount(1);
    } finally {
      await removeInvoice(page, id);
    }
  });
});

/** One position of 1.000 € net — enough for a cost assignment to be proposed an amount. */
const POSITION = { number: 1, menge: 1, einzelNetto: 1000 };

/**
 * The invoice's first position, unfolded: a stored one opens folded (see `PositionRow`), so its cost
 * assignments are not reachable before it is opened.
 */
async function openStoredPosition(page: Page, format: UserFormat) {
  const row = page
    .locator("section", {
      has: page.getByText(format.t("fibu.rechnung.positions"), { exact: true }),
    })
    .locator('[data-slot="collapsible"]')
    .first();
  await expect(row).toBeVisible({ timeout: 60_000 });
  await row.locator('[data-slot="collapsible-trigger"]').click();
  return row;
}

/**
 * A VAT rate as it stands in its box, from the factor the backend holds: 0.19 → "19,00".
 *
 * Through the app's own two helpers rather than through a percent formatter: the field holds a factor
 * and shows a percentage (`NumberField.percent`), and the box then writes that percentage padded to two
 * digits like every other number input at rest (see formatNumberInput) — with no "%" in the value, since
 * the suffix sits beside the box.
 */
function percentInput(format: UserFormat, factor: number): string {
  return formatNumberInput(factor * 100, format.context, 2, true);
}

/**
 * The part of a cost unit's name that is its number — the term the autocomplete is given.
 *
 * `KostFormatter` writes "4.400.99.00: <project> - <customer>" (FormatType.LONG), and the number alone
 * is both selective, being unique, and free of the business content that follows it. `Kost2PagesRest`
 * searches it as `rawNumberString`, so the dots do no harm.
 */
function searchTerm(displayName: string): string {
  return displayName.split(":")[0].trim();
}

/** The index of a column in the table, so its cell can be read out of a row. */
async function columnIndex(page: Page, label: string): Promise<number> {
  const headers = await page
    .getByRole("columnheader")
    .evaluateAll((nodes) => nodes.map((node) => node.textContent ?? ""));
  const index = headers.findIndex((text) => text.startsWith(label));
  if (index < 0) {
    throw new Error(`No column headed "${label}".`);
  }
  return index;
}

async function filterElements(
  page: Page,
  entity: string
): Promise<FilterElement[]> {
  const res = await page.request.get(`/rs/${entity}/listMeta`, {
    headers: { "X-PF-Frontend": "next" },
  });
  const meta = (await res.json()) as { filterElements?: FilterElement[] };
  return meta.filterElements ?? [];
}

/**
 * An amount as the table writes it, as a pattern: zero goes through the app's own formatter and its
 * digits are replaced by classes, so the assertion tests the shape the account's locale and currency
 * produce rather than any one value (the rows under test are the account's own data).
 *
 * Zero as the sample, and matched as a substring: it is the shortest amount there is, so its shape
 * ("0,00 €" → digit, separator, two digits, currency) is the tail of every larger one as well. A
 * thousands-grouped sample would only match invoices of four digits and up.
 */
function amountPattern(context: FormatContext): RegExp {
  const sample = formatCurrency(0, context);
  return new RegExp(escape(sample).replace(/\d/g, "\\d"));
}

/** A backend label goes into a RegExp, and "Kostzuweisungfehlbetrag" may end in punctuation. */
function escape(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
