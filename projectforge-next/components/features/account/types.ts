// Mirrors org.projectforge.rest.dto.Konto (projectforge-rest). Keep field names in sync with the
// Spring DTO.

import type { KONTO_METADATA } from "@/lib/metadata/konto.generated";

/** The constants of org.projectforge.business.fibu.KontoStatus, from the metadata. */
export type KontoStatus =
  (typeof KONTO_METADATA.fields.status.enumValues)[number]["value"];

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface KontoDetail {
  /** null for an account that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /** The account number, unique among the accounts (`KontoDao.onInsertOrModify`). */
  nummer?: number | null;
  bezeichnung?: string | null;
  description?: string | null;
  status?: KontoStatus | null;
  // The e-invoice block (Wicket's `KontoEditForm`, heading `fibu.konto.eInvoice`).
  contactPerson?: string | null;
  street?: string | null;
  zipCode?: string | null;
  city?: string | null;
  country?: string | null;
  vatId?: string | null;
  leitwegId?: string | null;
  eInvoiceEmail?: string | null;
  /** Name of one of the seller's configured bank accounts (`EInvoiceSellerConfig.bankAccounts`). */
  sellerBankAccountName?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface KontoListRow extends KontoDetail {
  id: number;
}
