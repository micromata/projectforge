// Mirrors org.projectforge.rest.dto.Rechnung, RechnungsPosition and KostZuweisung — the lean row of the
// list ([InvoiceListRow], what `copyFrom4ListRow` fills) and the whole DTO of the edit form
// ([InvoiceDetail], what `copyFromWithCollections` fills).
//
// Every property is optional: Spring's mapper uses `JsonInclude.Include.NON_NULL`
// (JacksonConfiguration), so an empty field is absent from the JSON rather than null.

import type { RowWithAttachments } from "@/components/shared/attachments/attachments-column";
import type { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import type { RECHNUNGS_POSITION_METADATA } from "@/lib/metadata/rechnungs-position.generated";
import type { Attachment } from "@/lib/rs/attachments";
import type { ListRow } from "@/hooks/use-entity-list-page";

type EnumOf<F extends { enumValues?: readonly { value: string }[] }> =
  NonNullable<F["enumValues"]>[number]["value"];

/** `RechnungStatus` — from planned to paid, seven values. */
export type RechnungStatus = EnumOf<typeof RECHNUNG_METADATA.fields.status>;

/** `RechnungTyp` — a proper invoice, or a credit note in one of its two directions. */
export type RechnungTyp = EnumOf<typeof RECHNUNG_METADATA.fields.typ>;

/** `PeriodOfPerformanceType` of a position: its own dates, or the invoice's. */
export type PeriodOfPerformanceType = EnumOf<
  typeof RECHNUNGS_POSITION_METADATA.fields.periodOfPerformanceType
>;

/** A referenced entity as the lean row carries it: the name to show, nothing else. */
export interface DisplayRef {
  displayName?: string;
}

/**
 * A referenced entity as the edit form carries it: the id it is written back by, and the name it shows.
 *
 * A type alias rather than an interface, so it satisfies the index signature of the schema's
 * `looseObject` (see invoice-schema.ts): TypeScript infers an implicit index signature for an alias,
 * never for an interface.
 */
export type EntityRefDto = {
  id: number;
  displayName?: string;
};

/**
 * The order position an invoice position bills, as `RechnungsPosition.OrderPositionRef` sends it.
 *
 * Read-only in the form for now: there is no picker for it, and it exists so an inherited reference is
 * not lost on save and can be shown as a link to the order.
 *
 * An alias rather than an interface for the same reason as [EntityRefDto]: it has to satisfy the index
 * signature of the schema's `looseObject`.
 */
export type OrderPositionRef = {
  id?: number | null;
  /** Id of the order the position belongs to — the link's target. */
  auftragId?: number | null;
  auftragNummer?: number | null;
  number?: number | null;
};

/** One cost assignment of a position — `KostZuweisung`, the third nesting level. */
export interface KostZuweisungDto {
  id?: number | null;
  deleted?: boolean;
  /** Position within its own position's list, **0-based** (`KostZuweisungDO.addKostZuweisung`). */
  index?: number | null;
  netto?: number | null;
  kost1?: EntityRefDto | null;
  kost2?: EntityRefDto | null;
  comment?: string | null;
}

/** One position of the invoice — `RechnungsPosition`. */
export interface InvoicePositionDto {
  id?: number | null;
  deleted?: boolean;
  /** Position number inside the invoice, 1-based. Assigned by the backend for a new row. */
  number?: number | null;
  text?: string | null;
  menge?: number | null;
  einzelNetto?: number | null;
  /** The VAT rate as a factor, not a percentage: 0.19 for 19 % (`RechnungsPositionDO.vat`). */
  vat?: number | null;
  auftragsPosition?: OrderPositionRef | null;
  periodOfPerformanceType?: PeriodOfPerformanceType | null;
  periodOfPerformanceBegin?: string | null;
  periodOfPerformanceEnd?: string | null;
  kostZuweisungen?: KostZuweisungDto[] | null;
  /**
   * The sums the backend computed for this position. Read-only, and only present on a loaded invoice —
   * while the form is being edited they come from `POST recalculate` (see use-invoice-sums.ts).
   */
  netSum?: number | null;
  vatAmountSum?: number | null;
  grossSum?: number | null;
  kostZuweisungNetSum?: number | null;
  /** Negated, as `RechnungPosInfo` computes it — see `InvoicePositionSums` in lib/rs/invoice.ts. */
  kostZuweisungNetFehlbetrag?: number | null;
}

/** The whole invoice as the edit form reads and writes it — `Rechnung.copyFromWithCollections`. */
export interface InvoiceDetail {
  /** null for an invoice that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /**
   * The invoice number. Assigned by `RechnungDao` on the transition out of GEPLANT, and a credit note
   * announced by the customer never gets one — so the form only ever shows it.
   */
  nummer?: number | null;
  // `customer`/`project` as the DTO names them, not `kunde`/`projekt` as the entity does — the JSON
  // carries the DTO's names, and `Rechnung.copyTo` is what maps them back.
  customer?: EntityRefDto | null;
  /** Free text customer for one not in the list. Nulled by the backend as soon as [customer] is set. */
  kundeText?: string | null;
  project?: EntityRefDto | null;
  konto?: EntityRefDto | null;
  status?: RechnungStatus | null;
  typ?: RechnungTyp | null;
  datum?: string | null;
  betreff?: string | null;
  customerref1?: string | null;
  attachment?: string | null;
  // The address block of the e-invoice, prefilled from the customer's account where empty.
  customerContactPerson?: string | null;
  customerAddress?: string | null;
  customerZipCode?: string | null;
  customerCity?: string | null;
  customerCountry?: string | null;
  customerVatId?: string | null;
  customerLeitwegId?: string | null;
  customerEInvoiceEmail?: string | null;
  /** IBAN of the seller's bank account, one of `EInvoiceSellerConfig.bankAccounts`. */
  sellerBankAccount?: string | null;
  periodOfPerformanceBegin?: string | null;
  periodOfPerformanceEnd?: string | null;
  faelligkeit?: string | null;
  /** Days from [datum] to [faelligkeit]; offered only while the latter is empty, as Wicket does. */
  zahlungsZielInTagen?: number | null;
  discountZahlungsZielInTagen?: number | null;
  discountPercent?: number | null;
  discountMaturity?: string | null;
  bezahlDatum?: string | null;
  zahlBetrag?: number | null;
  currency?: string | null;
  bemerkung?: string | null;
  besonderheiten?: string | null;
  positionen?: InvoicePositionDto[] | null;
  /** Access flags, filled by `transformFromDB` — `GET /rs/outgoingInvoice/{id}` sends no `userAccess`. */
  writeAccess?: boolean;
  deleteAccess?: boolean;
  /**
   * Whether cost accounting is configured at all. False hides the cost assignments of every position, as
   * `AbstractRechnungEditForm` hides the whole table then.
   */
  costConfigured?: boolean;
  attachmentsCounter?: number | null;
  attachmentsSize?: number | null;
  attachments?: Attachment[] | null;
  created?: string | null;
  lastUpdate?: string | null;
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
  /**
   * How much of the net sum is not assigned to a cost unit yet
   * (`RechnungInfo.kostZuweisungenFehlbetrag`). Absent where cost accounting is not configured at all,
   * so the column is empty rather than a row of "0,00 €" (see `Rechnung.copyFrom4ListRow`).
   */
  kostZuweisungenFehlbetrag?: number;
  /** The cost 1 units the invoice is assigned to, as numbers ("5.100.01, 5.100.02"). */
  kost1List?: string;
  /** The same with names and amounts, for the cell's tooltip. */
  kost1Info?: string;
  kost2List?: string;
  kost2Info?: string;
  created?: string;
  lastUpdate?: string;
}
