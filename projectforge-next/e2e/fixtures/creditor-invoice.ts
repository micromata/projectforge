import type { Page } from "@playwright/test";

/**
 * The throwaway incoming (creditor) invoices the creditor-invoice specs work on, and the cost units
 * they book onto.
 *
 * The incoming sibling of fixtures/invoice.ts, and a leaner one: a creditor invoice has no number,
 * status, type, customer, project or period of performance ProjectForge assigns — what identifies it
 * is its creditor and reference, free text. So there is nothing to create it "GEPLANT" for (the
 * outgoing invoice's trick against spending an invoice number): a creditor invoice never gets a
 * number handed out, and marking it deleted afterwards leaves nothing behind.
 *
 * Every case creates its own invoice through the API and marks it deleted afterwards, whatever
 * happened in between. Two reasons it cannot read one of the database instead: the list is a
 * production ledger, so no spec may name an invoice of it (see seed.ts), and the cases need a *known*
 * net sum and a *known* cost assignment state to say anything about the Fehlbetrag.
 */

/** The REST category of the incoming invoice pages — `IncomingInvoiceEntityRest` maps to this. */
export const CREDITOR_ENTITY = "incomingInvoice";

/** One position as these specs post it — the fields the form then shows and sums. */
export interface PostedPosition {
  number: number;
  text: string;
  menge: number;
  einzelNetto: number;
}

/** What the API answers for one incoming invoice; only the parts the specs assert on. */
export interface StoredCreditorInvoice {
  id: number;
  kreditor?: string;
  referenz?: string;
  betreff?: string;
  netSum?: number;
  grossSum?: number;
  positionen?: {
    number: number;
    netSum?: number;
    vatAmountSum?: number;
    grossSum?: number;
  }[];
}

/** What [createCreditorInvoice] may add beyond the positions and the identifying head fields. */
export interface CreditorInvoiceOptions {
  /** The creditor the invoice is from — free text, the incoming counterpart to a customer. */
  kreditor: string;
  /** The creditor's own invoice number/reference. */
  referenz?: string;
  /** Subject of the invoice; carries the run's MARKER so a leftover is recognizable in the list. */
  betreff: string;
  /** Due date as `yyyy-MM-dd`, for the case that reads the payment target derived from it. */
  faelligkeit?: string;
}

/**
 * Creates an incoming invoice with the given positions and answers with its id.
 *
 * Through the API rather than through the form: what the cases are about is the *editing* of an
 * invoice that exists, and building one through the UI first would make every one of them fail for the
 * reasons of the save path instead.
 *
 * 19 % VAT on every position, so the gross sums are not all equal to the net ones — the banner shows
 * both, and two identical columns would not tell them apart. `datum` is fixed rather than derived from
 * today: nothing here depends on when the run happens. `EingangsrechnungDao` refuses a null date and an
 * invoice without positions, and needs no period of performance (unlike the outgoing invoice).
 */
export async function createCreditorInvoice(
  page: Page,
  positions: PostedPosition[],
  options: CreditorInvoiceOptions
): Promise<number> {
  const response = await page.request.put(`/rs/${CREDITOR_ENTITY}/saveorupdate`, {
    headers: await writeHeaders(page),
    data: {
      data: {
        datum: "2026-03-02",
        ...(options.faelligkeit == null
          ? {}
          : { faelligkeit: options.faelligkeit }),
        kreditor: options.kreditor,
        ...(options.referenz == null ? {} : { referenz: options.referenz }),
        betreff: options.betreff,
        positionen: positions.map((pos) => ({ ...pos, vat: 0.19 })),
      },
    },
  });
  const body = (await response.json()) as {
    variables?: { id?: number };
    validationErrors?: { message?: string }[];
  };
  if (!response.ok() || body.variables?.id == null) {
    const reason =
      body.validationErrors?.map((e) => e.message).join("; ") ??
      `HTTP ${response.status()}`;
    throw new Error(`Could not create the incoming invoice for the test: ${reason}`);
  }
  return body.variables.id;
}

export async function fetchCreditorInvoice(
  page: Page,
  id: number
): Promise<StoredCreditorInvoice> {
  const response = await page.request.get(
    `/rs/${CREDITOR_ENTITY}/${id}?editMode=true`,
    { headers: { "X-PF-Frontend": "next" } }
  );
  if (!response.ok()) {
    throw new Error(`Could not read incoming invoice ${id}: HTTP ${response.status()}`);
  }
  return (await response.json()) as StoredCreditorInvoice;
}

/**
 * Marks an incoming invoice as deleted, so a run leaves nothing behind that the list shows.
 *
 * Not `forceDelete`: `EingangsrechnungDO` is historizable, so the physical delete is refused — marking
 * it is as far as this goes, and the list hides deleted invoices by default. The whole DTO is posted
 * back because that is what the endpoint takes (see lib/rs/entity.ts).
 */
export async function removeCreditorInvoice(page: Page, id: number | null) {
  if (id == null) return;
  const invoice = await fetchCreditorInvoice(page, id);
  await page.request.delete(`/rs/${CREDITOR_ENTITY}/markAsDeleted`, {
    headers: await writeHeaders(page),
    data: { data: invoice },
  });
}

/**
 * The default VAT, as the form itself reads it — the incoming invoice's `formDefaults` carries only
 * that (no seller bank accounts, template variants or e-invoice flags).
 */
export async function fetchFormDefaults(
  page: Page
): Promise<{ defaultVat?: number | null }> {
  const response = await page.request.get(`/rs/${CREDITOR_ENTITY}/formDefaults`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) return {};
  return (await response.json()) as { defaultVat?: number | null };
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
