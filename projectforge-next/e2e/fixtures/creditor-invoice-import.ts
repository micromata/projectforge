import type { Page } from "@playwright/test";
import { MARKER, uniqueSuffix } from "./seed";
import {
  fetchCreditorInvoice,
  removeCreditorInvoice,
  CREDITOR_ENTITY,
} from "./creditor-invoice";

/**
 * The throwaway DATEV CSV the import spec uploads, and the cleanup of whatever the committed import job
 * then creates in the ledger.
 *
 * The import commit does not hand back an id — it enqueues a background job that inserts the invoices
 * asynchronously — so the spec cannot remember what to delete the way the edit spec does. Instead every
 * run's CSV carries a *unique* creditor (MARKER + suffix), and the cleanup finds the created invoices by
 * searching the list for exactly that creditor. Marking them deleted leaves nothing behind: a creditor
 * invoice has no number handed out, and the list hides deleted rows by default (see fixtures/seed.ts).
 */

/** The identifying, unique-per-run fields the import CSV is built from and the cleanup searches by. */
export interface ImportSubject {
  kreditor: string;
  referenz: string;
}

/** A fresh subject: the creditor carries the run marker so a leftover is recognisable and searchable. */
export function importSubject(): ImportSubject {
  const suffix = uniqueSuffix();
  return {
    kreditor: `${MARKER} import creditor ${suffix}`,
    referenz: `${MARKER}-import-${suffix}`,
  };
}

/**
 * The invoice date every import row carries — deliberately far in the future.
 *
 * Reconcile syncs a *period*: it loads the ledger between the min and max date of the imported rows and
 * proposes deleting every DB invoice in that window the CSV does not also carry (see
 * `EingangsrechnungImportStorage.doReconcileImportStorage`). A date no real invoice shares means that
 * window holds nothing but this run's row, so reconcile can only ever propose the one NEW invoice and
 * never a deletion. The spec still guards on that (asserts zero proposed deletions before it commits),
 * but the date is the first line of defence — never move it back into a populated year.
 */
const FAR_FUTURE_DATE = "31.12.2099";

/**
 * One DATEV CSV row for [subject], in the German column names and formats the backend's
 * `DATEV_IMPORT_SETTINGS` maps (date `dd.MM.yyyy`, amount `#.##0,0#`).
 *
 * A `Periode` column is present, so the import is *position-based* (`isPositionBasedImport = true`).
 * That matters: a header-only import (no Periode) can only update or delete invoices already in the
 * ledger — it skips NEW rows (`EingangsrechnungImportJob.runHeaderOnlyImport`), so it could never create
 * the invoice this spec then reads back. Position-based turns the single row into one insertable invoice
 * with one position.
 */
export function datevCsv(subject: ImportSubject): string {
  return [
    "Rechnungsdatum;Rechnungs-Nr.;Geschäftspartner-Name;Betreff;Rechnungsbetrag;WKZ;Periode",
    `${FAR_FUTURE_DATE};${subject.referenz};${subject.kreditor};${MARKER} import subject;746,13;EUR;01.12.2099-31.12.2099`,
  ].join("\n");
}

/** The CSV as a Playwright file payload, for `setInputFiles` on the drop area's hidden input. */
export function datevCsvFile(subject: ImportSubject) {
  return {
    name: "invoices.csv",
    mimeType: "text/csv",
    buffer: Buffer.from(datevCsv(subject), "utf-8"),
  };
}

interface ListRow {
  id: number;
  kreditor?: string;
}

/**
 * The ids of the invoices the committed job created for [subject], newest first.
 *
 * Polled, not read once: the commit only enqueues the job, so the rows appear a moment later. Filtered on
 * the creditor in code rather than trusting the full-text search alone, so a partial match of another run
 * cannot leak in. Returns as soon as at least [expected] rows are there, or the last read after the
 * timeout — the caller asserts on the count.
 */
export async function findImportedInvoiceIds(
  page: Page,
  subject: ImportSubject,
  expected = 1,
  timeoutMs = 30_000
): Promise<number[]> {
  const deadline = Date.now() + timeoutMs;
  let ids: number[] = [];
  for (;;) {
    const response = await page.request.post(`/rs/${CREDITOR_ENTITY}/list`, {
      headers: await writeHeaders(page),
      data: { searchString: subject.kreditor },
    });
    if (response.ok()) {
      const body = (await response.json()) as { resultSet?: ListRow[] };
      ids = (body.resultSet ?? [])
        .filter((row) => row.kreditor === subject.kreditor)
        .map((row) => row.id)
        .sort((a, b) => b - a);
    }
    if (ids.length >= expected || Date.now() >= deadline) return ids;
    await page.waitForTimeout(1_000);
  }
}

/** Marks every given invoice deleted, so a run leaves nothing in the ledger. */
export async function removeImportedInvoices(page: Page, ids: number[]) {
  for (const id of ids) {
    await removeCreditorInvoice(page, id);
  }
}

/** Reads one back, for a spec that wants to assert on the created invoice's fields before deleting it. */
export { fetchCreditorInvoice };

/** The headers a state changing call needs — the CSRF token is read per call, not cached. */
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
