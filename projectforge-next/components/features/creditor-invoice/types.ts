// Mirrors org.projectforge.rest.dto.Eingangsrechnung and EingangsrechnungsPosition — the lean row of the
// list ([CreditorInvoiceListRow], what `copyFrom4ListRow` fills) and the whole DTO of the edit form
// ([CreditorInvoiceDetail], what `copyFromWithCollections` fills).
//
// Every property is optional: Spring's mapper uses `JsonInclude.Include.NON_NULL` (JacksonConfiguration),
// so an empty field is absent from the JSON rather than null.
//
// The cost assignment (`KostZuweisung`) is identical to the outgoing invoice's and its DTO type lives in
// the shared module ([KostZuweisungDto]).

import type { EntityRefDto } from "@/components/shared/invoice/kost-zuweisung";
import type { KostZuweisungDto } from "@/components/shared/invoice/kost-zuweisung";
import type { EINGANGSRECHNUNG_METADATA } from "@/lib/metadata/eingangsrechnung.generated";
import type { ListRow } from "@/hooks/use-entity-list-page";

type EnumOf<F extends { enumValues?: readonly { value: string }[] }> =
  NonNullable<F["enumValues"]>[number]["value"];

/** `PaymentType` — how the creditor invoice is settled (bank transfer, debit, …). */
export type PaymentType = EnumOf<
  typeof EINGANGSRECHNUNG_METADATA.fields.paymentType
>;

/** A referenced entity as the lean row carries it: the name to show, nothing else. */
export interface DisplayRef {
  displayName?: string;
}

/** One position of the invoice — `EingangsrechnungsPosition`. Leaner than the outgoing one. */
export interface CreditorInvoicePositionDto {
  id?: number | null;
  deleted?: boolean;
  /** Position number inside the invoice, 1-based. Assigned by the backend for a new row. */
  number?: number | null;
  text?: string | null;
  menge?: number | null;
  einzelNetto?: number | null;
  /** The VAT rate as a factor, not a percentage: 0.19 for 19 % (`EingangsrechnungsPositionDO.vat`). */
  vat?: number | null;
  kostZuweisungen?: KostZuweisungDto[] | null;
  /**
   * The sums the backend computed for this position. Read-only, and only present on a loaded invoice —
   * while the form is being edited they come from `POST recalculate` (see use-creditor-invoice-sums, which
   * is the shared use-invoice-sums bound to this category).
   */
  netSum?: number | null;
  vatAmountSum?: number | null;
  grossSum?: number | null;
  kostZuweisungNetSum?: number | null;
  /** Negated, as `RechnungPosInfo` computes it — see `InvoicePositionSums` in lib/rs/invoice-sums.ts. */
  kostZuweisungNetFehlbetrag?: number | null;
}

/** The whole invoice as the edit form reads and writes it — `Eingangsrechnung.copyFromWithCollections`. */
export interface CreditorInvoiceDetail {
  /** null for an invoice that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /** The creditor the invoice is from — free text, the incoming counterpart to a customer. */
  kreditor?: string | null;
  /** The creditor's own invoice number/reference. */
  referenz?: string | null;
  /** The DATEV account, as the DTO names it (`konto`). */
  konto?: EntityRefDto | null;
  datum?: string | null;
  betreff?: string | null;
  customernr?: string | null;
  receiver?: string | null;
  iban?: string | null;
  bic?: string | null;
  paymentType?: PaymentType | null;
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
  positionen?: CreditorInvoicePositionDto[] | null;
  /** Access flags, filled by `transformFromDB` — `GET /rs/incomingInvoice/{id}` sends no `userAccess`. */
  writeAccess?: boolean;
  deleteAccess?: boolean;
  /**
   * Whether cost accounting is configured at all. False hides the cost assignments of every position, as
   * the Wicket form hides the whole table then.
   */
  costConfigured?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** One row of the incoming invoice list. */
export interface CreditorInvoiceListRow extends ListRow {
  kreditor?: string;
  referenz?: string;
  betreff?: string;
  bemerkung?: string;
  datum?: string;
  bezahlDatum?: string;
  currency?: string;
  iban?: string;
  /** IBAN grouped in blocks of four for reading, as the backend formats it (`ibanFormatted`). */
  ibanFormatted?: string;
  /** The account of the invoice itself, as "11400 - Kreditoren". */
  konto?: DisplayRef;
  /** The payment type translated by the backend — what the column shows and sorts by. */
  paymentTypeAsString?: string;
  /** Due date, or the discount date where it comes first (`faelligkeitOrDiscountMaturity`). */
  faelligkeitOrDiscountMaturity?: string;
  netSum?: number;
  /** Gross sum minus a discount that was taken — the amount the invoice actually came to. */
  grossSumWithDiscount?: number;
  /**
   * Whether the invoice is past its due date and unpaid (`RechnungInfo.isUeberfaellig`). Not a column but
   * the row's colour, so it travels as data rather than being derived from the dates here.
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
