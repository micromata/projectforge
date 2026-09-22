import type { ImportRead } from "@/components/shared/import/import-types";

/**
 * The parsed row of a creditor-invoice import — `EingangsrechnungPosImportDTO.read` on the backend. Only
 * the fields the preview shows are typed; the flow reads every value by dotted path, so this is a
 * documentation of the shape rather than the access contract (hence the {@link ImportRead} extension).
 */
export interface CreditorInvoiceImportRead extends ImportRead {
  positionNummer?: number;
  referenz?: string;
  kreditor?: string;
  datum?: string;
  grossSum?: number;
  currency?: string;
  betreff?: string;
  konto?: { nummer?: number };
  kost1?: { description?: string };
  kost2?: { description?: string };
  faelligkeit?: string;
  bezahlDatum?: string;
  zahlBetrag?: number;
  paymentTypeAsString?: string;
  taxRate?: number;
  bemerkung?: string;
  customernr?: string;
  discountPercent?: number;
  discountMaturity?: string;
  iban?: string;
  bic?: string;
}

/**
 * The `extraViewMeta` the incoming-invoice import answers with: whether the file is a position-based
 * import (header + positions) or header-only. The preview gates several columns on it.
 */
export interface CreditorInvoiceImportMeta {
  isPositionBasedImport?: boolean;
}
