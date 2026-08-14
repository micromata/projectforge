// Mirrors org.projectforge.rest.dto.Auftrag, .AuftragsPosition and .PaymentSchedule
// (projectforge-rest). Keep the field names in sync with those DTOs — they are what `copyFrom`/`copyTo`
// read and write, not the DO's.
//
// Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
// (JacksonConfiguration), so an empty field is absent from the JSON rather than null. `toFormValues`
// normalises that away (see order-values.ts).

import type { AUFTRAG_METADATA } from "@/lib/metadata/auftrag.generated";
import type { AUFTRAGS_POSITION_METADATA } from "@/lib/metadata/auftrags-position.generated";

type EnumOf<F extends { enumValues?: readonly { value: string }[] }> =
  NonNullable<F["enumValues"]>[number]["value"];

export type AuftragsStatus = EnumOf<typeof AUFTRAG_METADATA.fields.status>;
export type AuftragsPositionsStatus = EnumOf<
  typeof AUFTRAGS_POSITION_METADATA.fields.status
>;
export type AuftragsPositionsArt = EnumOf<
  typeof AUFTRAGS_POSITION_METADATA.fields.art
>;
export type PeriodOfPerformanceType = EnumOf<
  typeof AUFTRAGS_POSITION_METADATA.fields.periodOfPerformanceType
>;
export type PaymentType = EnumOf<
  typeof AUFTRAGS_POSITION_METADATA.fields.paymentType
>;
export type ModeOfPaymentType = EnumOf<
  typeof AUFTRAGS_POSITION_METADATA.fields.modeOfPaymentType
>;
/** The same enum on the order and on each of its positions (`AuftragForecastType`). */
export type ForecastType = EnumOf<typeof AUFTRAG_METADATA.fields.forecastType>;

/**
 * A referenced entity as every DTO carries it: the id to write back, the name to show.
 *
 * `displayName` is optional but not nullable, matching what the schema accepts (`entityField` in
 * from-metadata.ts) — the DTO's own `displayName` is a computed getter that is either there or absent
 * from the JSON, never null.
 *
 * A type alias rather than an interface, so it satisfies the index signature of the schema's
 * `looseObject` (which keeps whatever else the backend sends): TypeScript infers an implicit index
 * signature for an alias, never for an interface.
 */
export type EntityRefDto = {
  id: number;
  displayName?: string;
};

/** One invoice a position was billed with — read-only, from `RechnungCache`. */
export interface InvoiceRef {
  id?: number | null;
  nummer?: number | null;
  date?: string | null;
  netSum?: number | null;
}

/**
 * The read-only part of a position: what it was invoiced with. Kept out of the form's values on purpose
 * — see [PositionInvoices] — and looked up per position id from the loaded order.
 */
export interface PositionInvoiceInfo {
  invoicedSum?: number | null;
  notInvoicedSum?: number | null;
  invoices?: InvoiceRef[] | null;
  /** True when an invoice references this position — then it must not be deleted. */
  invoicedElsewhere?: boolean;
}

export interface OrderPositionDto extends PositionInvoiceInfo {
  id?: number | null;
  deleted?: boolean;
  /** Position number within the order, 1-based. Assigned by the backend for a new row. */
  number?: number | null;
  titel?: string | null;
  art?: AuftragsPositionsArt | null;
  paymentType?: PaymentType | null;
  forecastType?: ForecastType | null;
  status?: AuftragsPositionsStatus | null;
  nettoSumme?: number | null;
  personDays?: number | null;
  bemerkung?: string | null;
  vollstaendigFakturiert?: boolean | null;
  periodOfPerformanceType?: PeriodOfPerformanceType | null;
  periodOfPerformanceBegin?: string | null;
  periodOfPerformanceEnd?: string | null;
  modeOfPaymentType?: ModeOfPaymentType | null;
  task?: EntityRefDto | null;
}

export interface PaymentScheduleDto {
  id?: number | null;
  deleted?: boolean;
  number?: number | null;
  /** Number of the position this instalment belongs to, not its id — see `PaymentScheduleDO`. */
  positionNumber?: number | null;
  scheduleDate?: string | null;
  amount?: number | null;
  comment?: string | null;
  reached?: boolean | null;
  vollstaendigFakturiert?: boolean | null;
}

export interface OrderDetail {
  /** null for an order that has not been saved yet (Spring assigns the id). */
  id: number | null;
  nummer?: number | null;
  titel?: string | null;
  referenz?: string | null;
  status?: AuftragsStatus | null;
  // `customer`/`project` as the DTO names them, not `kunde`/`projekt` as the entity does — the JSON
  // carries the DTO's names, and `Auftrag.copyTo` is what maps them back (see its comment there).
  customer?: EntityRefDto | null;
  kundeText?: string | null;
  project?: EntityRefDto | null;
  contactPerson?: EntityRefDto | null;
  projectManager?: EntityRefDto | null;
  headOfBusinessManager?: EntityRefDto | null;
  salesManager?: EntityRefDto | null;
  erfassungsDatum?: string | null;
  angebotsDatum?: string | null;
  entscheidungsDatum?: string | null;
  bindungsFrist?: string | null;
  beauftragungsDatum?: string | null;
  beauftragungsBeschreibung?: string | null;
  periodOfPerformanceBegin?: string | null;
  periodOfPerformanceEnd?: string | null;
  probabilityOfOccurrence?: number | null;
  forecastType?: ForecastType | null;
  bemerkung?: string | null;
  statusBeschreibung?: string | null;
  positionen?: OrderPositionDto[] | null;
  paymentSchedules?: PaymentScheduleDto[] | null;
  /** Whether saving notifies the contact person; preset by the backend, then the user's choice. */
  sendEMailNotification?: boolean | null;
  /** Access flags, filled by `transformFromDB` — `GET /rs/order/{id}` sends no `userAccess`. */
  writeAccess?: boolean;
  deleteAccess?: boolean;
  vollstaendigFakturiertWriteAccess?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/**
 * One row of the list: the same `Auftrag` DTO, with only the fields the columns of `order.page.tsx`
 * show. The backend fills exactly those for a next client (`Auftrag.copyFrom4ListRow`, selected by
 * `AbstractPagesRest.createListRow`) and `JsonInclude.NON_NULL` keeps the rest off the wire — 12.5 MB
 * become 3.2 MB over the 7132 orders of a real installation.
 *
 * So this is deliberately **not** an extension of [OrderDetail], although the wire type is the same DTO:
 * what a list row carries is a subset, and inheriting the full shape is what let the page reach for a
 * field that isn't there (and made the fat payload look intended).
 *
 * `customer`/`project` arrive as an [EntityRefDto] holding nothing but the `displayName` the cell shows —
 * no id, since the row navigates by its own.
 */
export interface OrderListRow {
  id: number;
  deleted?: boolean;
  nummer?: number | null;
  /** Only `displayName` — of the customer, or the free text one of an order without a customer. */
  customer?: Pick<EntityRefDto, "displayName"> | null;
  /** Only `displayName`, see [customer]. */
  project?: Pick<EntityRefDto, "displayName"> | null;
  titel?: string | null;
  /** `#3`, the count of the order's positions (`AuftragDO.pos`, transient). */
  pos?: string | null;
  personDays?: number | null;
  referenz?: string | null;
  /** The four managers in one column (`assignedPersons`, transient). */
  assignedPersons?: string | null;
  erfassungsDatum?: string | null;
  entscheidungsDatum?: string | null;
  nettoSumme?: number | null;
  beauftragtNettoSumme?: number | null;
  fakturiertSum?: number | null;
  zuFakturierenSum?: number | null;
  periodOfPerformanceBegin?: string | null;
  periodOfPerformanceEnd?: string | null;
  probabilityOfOccurrence?: number | null;
  status?: AuftragsStatus | null;
  /** True when at least one position/schedule is due to be invoiced — drives row highlighting. */
  toBeInvoiced?: boolean | null;
  attachmentsCounter?: number | null;
  attachmentsSizeFormatted?: string | null;
  /** Every list offers both as a column, `lastUpdate` shown from the start (see lib/page-def/audit-columns.ts). */
  created?: string | null;
  lastUpdate?: string | null;
}
