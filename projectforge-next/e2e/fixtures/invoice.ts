import type { Page } from "@playwright/test";
import { MARKER, uniqueSuffix } from "./seed";

/**
 * The throwaway outgoing invoices the invoice specs work on, and the cost units they book onto.
 *
 * Every case creates its own invoice and marks it deleted afterwards, whatever happened in between.
 * Two reasons it cannot read one of the database instead: the list is a production ledger, so no spec
 * may name an invoice of it (see seed.ts), and the cases need a *known* net sum and a *known* cost
 * assignment state to say anything about the Fehlbetrag or about a cost unit warning.
 *
 * The invoices are created as GEPLANT on purpose: `RechnungDao` assigns an invoice number on the
 * transition out of it, and a number, once handed out, is spent — the ledger would gain a gap for every
 * run. A planned invoice has none, which is also what makes it removable without a trace in the
 * numbering.
 */

/** The entity of the outgoing invoice pages — spelled out rather than imported from `INVOICE_PAGE`. */
const ENTITY = "outgoingInvoice";

/** One position as these specs post it — the fields the form then shows and sums. */
export interface PostedPosition {
  number: number;
  text: string;
  menge: number;
  einzelNetto: number;
}

/** What the API answers for one invoice; only the parts the specs assert on. */
export interface StoredInvoice {
  id: number;
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

/** What [createInvoice] may add beyond the positions — a project, for the cost unit cases. */
export interface InvoiceOptions {
  /** Subject of the invoice; carries [MARKER] so a leftover is recognizable in the list. */
  subject: string;
  /** The project the invoice is written for, by id — see [findProjectsWithKost2]. */
  projectId?: number;
}

/**
 * Creates a planned invoice with the given positions and answers with its id.
 *
 * Through the API rather than through the form: what the cases are about is the *editing* of an
 * invoice that exists, and building one through the UI first would make every one of them fail for the
 * reasons of the save path instead.
 *
 * 19 % VAT on every position, so the gross sums are not all equal to the net ones — the banner shows
 * both, and two identical columns would not tell them apart. `datum` and the period are fixed rather
 * than derived from today: nothing here depends on when the run happens.
 */
export async function createInvoice(
  page: Page,
  positions: PostedPosition[],
  options: InvoiceOptions
): Promise<number> {
  const response = await page.request.put(`/rs/${ENTITY}/saveorupdate`, {
    headers: await writeHeaders(page),
    data: {
      data: {
        // GEPLANT, so no invoice number is handed out — see this file's KDoc.
        status: "GEPLANT",
        typ: "RECHNUNG",
        datum: "2026-03-02",
        betreff: options.subject,
        // Required, not decoration: a position of type SEEABOVE - the default - refers to the invoice's
        // period, which makes the invoice's begin date mandatory (`PeriodOfPerformanceValidator`, and
        // Wicket's `setRequiredSupplier` on the same field).
        periodOfPerformanceBegin: "2026-03-01",
        periodOfPerformanceEnd: "2026-03-31",
        // A project makes the invoice's cost units answerable at all (`kost2Check`, `activeKost2`).
        ...(options.projectId == null
          ? {}
          : { project: { id: options.projectId } }),
        // A free-text customer, because a real one cannot be created here (`KundeDO` has no generated
        // id, see seed.ts) and naming one would put a customer of the database into the source.
        kundeText: `${MARKER} customer ${uniqueSuffix()}`,
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
    throw new Error(`Could not create the invoice for the test: ${reason}`);
  }
  return body.variables.id;
}

export async function fetchInvoice(
  page: Page,
  id: number
): Promise<StoredInvoice> {
  const response = await page.request.get(`/rs/${ENTITY}/${id}?editMode=true`, {
    headers: { "X-PF-Frontend": "next" },
  });
  if (!response.ok()) {
    throw new Error(`Could not read invoice ${id}: HTTP ${response.status()}`);
  }
  return (await response.json()) as StoredInvoice;
}

/**
 * Marks an invoice as deleted, so a run leaves nothing behind that the list shows.
 *
 * Not `forceDelete`: `RechnungDO` is historizable, so the physical delete is refused — marking it is
 * as far as this goes, and `RechnungFilter` hides deleted invoices by default. The whole DTO is posted
 * back because that is what the endpoint takes (see lib/rs/entity.ts).
 */
export async function removeInvoice(page: Page, id: number | null) {
  if (id == null) return;
  const invoice = await fetchInvoice(page, id);
  await page.request.delete(`/rs/${ENTITY}/markAsDeleted`, {
    headers: await writeHeaders(page),
    data: { data: invoice },
  });
}

/** A cost unit as `outgoingInvoice/activeKost2` answers it — the form's own source for them. */
export interface ActiveKost2 {
  id: number;
  displayName: string;
}

/** Two projects that have active cost units, with the cost units of each. */
export interface ProjectsWithKost2 {
  first: { id: number; kost2: ActiveKost2[] };
  second: { id: number; kost2: ActiveKost2[] };
}

/**
 * Two projects of the database that have active cost units — one for the invoice, one to take a
 * *foreign* cost unit from (see Kost2Warning).
 *
 * Read rather than created: a cost unit's number is part of someone's chart of accounts and is
 * assigned by hand (`Kost2Dao.onInsertOrModify` ties it to its project's number range and area), so
 * inserting one means inventing an account number that can never be released again. Nothing of the
 * rows enters the source — the ids and the search terms are taken at runtime, which is what the
 * confidentiality rule is about.
 *
 * Null where the account sees fewer than two such projects (an empty database, or no `PM_PROJECT`
 * right): the caller then skips, rather than failing over missing data.
 */
export async function findProjectsWithKost2(
  page: Page
): Promise<ProjectsWithKost2 | null> {
  const response = await page.request.post("/rs/project/list", {
    headers: await writeHeaders(page),
    data: {},
  });
  if (!response.ok()) return null;
  const { resultSet = [] } = (await response.json()) as {
    resultSet?: { id?: number }[];
  };

  const found: { id: number; kost2: ActiveKost2[] }[] = [];
  // Bounded: most projects of a chart of accounts have cost units, so the two are found in the first
  // handful — and asking for every project of a production database would be a request per row.
  for (const project of resultSet.slice(0, 40)) {
    if (project.id == null) continue;
    const kost2 = await fetchActiveKost2(page, project.id);
    if (kost2.length > 0) found.push({ id: project.id, kost2 });
    if (found.length === 2) return { first: found[0], second: found[1] };
  }
  return null;
}

/** The active cost units of one project, as the form reads them. */
export async function fetchActiveKost2(
  page: Page,
  projectId: number
): Promise<ActiveKost2[]> {
  const response = await page.request.get(
    `/rs/${ENTITY}/activeKost2?projektId=${projectId}`,
    { headers: { "X-PF-Frontend": "next" } }
  );
  if (!response.ok()) return [];
  const list = (await response.json()) as
    | { id?: number; displayName?: string }[]
    | null;
  return (list ?? [])
    .filter((k): k is ActiveKost2 => k.id != null && !!k.displayName)
    .map((k) => ({ id: k.id, displayName: k.displayName }));
}

/** The default VAT and the other form defaults, as the form itself reads them. */
export async function fetchFormDefaults(
  page: Page
): Promise<{ defaultVat?: number | null }> {
  const response = await page.request.get(`/rs/${ENTITY}/formDefaults`, {
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
