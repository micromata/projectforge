// Mirrors org.projectforge.rest.dto.Rechnung — specifically what `copyFrom4ListRow` fills, which is the
// lean row of this list and not the whole DTO (the edit form is still Wicket's).
//
// Every property is optional: Spring's mapper uses `JsonInclude.Include.NON_NULL`
// (JacksonConfiguration), so an empty field is absent from the JSON rather than null.

import type { RowWithAttachments } from "@/components/shared/attachments/attachments-column";
import type { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import type { ListRow } from "@/hooks/use-entity-list-page";

type EnumOf<F extends { enumValues?: readonly { value: string }[] }> =
  NonNullable<F["enumValues"]>[number]["value"];

/** `RechnungStatus` — from planned to paid, seven values. */
export type RechnungStatus = EnumOf<typeof RECHNUNG_METADATA.fields.status>;

/** A referenced entity as the lean row carries it: the name to show, nothing else. */
export interface DisplayRef {
  displayName?: string;
}

/** One row of the invoice list. */
export interface InvoiceListRow extends ListRow, RowWithAttachments {
  nummer?: number;
  /** The customer, or the free text of an invoice naming none (`KundeFormatter`). */
  customer?: DisplayRef;
  project?: DisplayRef;
  /** The account of the invoice itself, as "11400 - Debitoren". */
  konto?: DisplayRef;
  betreff?: string;
  bemerkung?: string;
  status?: RechnungStatus;
  /** The status translated by the backend — what the column shows and sorts by. */
  statusAsString?: string;
  datum?: string;
  faelligkeit?: string;
  bezahlDatum?: string;
  zahlBetrag?: number;
  periodOfPerformanceBegin?: string;
  periodOfPerformanceEnd?: string;
  netSum?: number;
  /** Gross sum minus a discount that was taken — the amount the invoice actually came to. */
  grossSumWithDiscount?: number;
  /**
   * Whether the invoice is past its due date and unpaid (`RechnungInfo.isUeberfaellig`). Not a column but
   * the row's colour, so it travels as data rather than being derived from the dates here — whether a
   * partial payment counts as paid is `RechnungCalculator`'s rule.
   */
  ueberfaellig?: boolean;
  /** The cost 1 units the invoice is assigned to, as numbers ("5.100.01, 5.100.02"). */
  kost1List?: string;
  /** The same with names and amounts, for the cell's tooltip. */
  kost1Info?: string;
  kost2List?: string;
  kost2Info?: string;
  created?: string;
  lastUpdate?: string;
}
