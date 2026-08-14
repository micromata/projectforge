import { z } from "zod";
import { AUFTRAG_METADATA } from "@/lib/metadata/auftrag.generated";
import { AUFTRAGS_POSITION_METADATA } from "@/lib/metadata/auftrags-position.generated";
import { PAYMENT_SCHEDULE_METADATA } from "@/lib/metadata/payment-schedule.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length, the constants of each enum — comes from `AuftragDO`,
 * `AuftragsPositionDO` and `PaymentScheduleDO` through `lib/metadata/*.generated.ts`, the same source
 * the field components read (see form-context.tsx). Which fields the form has mirrors the three DTOs in
 * projectforge-rest — that is a hand-written decision, because a DTO has neither the field set nor the
 * names of its DO. What each field *allows* is not.
 *
 * The rules the metadata cannot express are the backend's alone and stay there: the period of
 * performance (an order's begin is mandatory as soon as a position inherits it, a position's end as soon
 * as it has its own — `PeriodOfPerformanceValidator`), and everything `AuftragDao.onInsertOrModify`
 * checks. They come back as an HTTP 406 and land on the field their `fieldId` names, nested paths
 * included (`positionen[0].periodOfPerformanceEnd`).
 */
const m = fromMetadata(AUFTRAG_METADATA);
const p = fromMetadata(AUFTRAGS_POSITION_METADATA);
const s = fromMetadata(PAYMENT_SCHEDULE_METADATA);

/**
 * A referenced entity the *order's* metadata has no field for: the customer and the project are
 * `KundeDO` and `ProjektDO`, for which there is no `UIDataType`, so `ElementsRegistry` never reports
 * them and the generator cannot carry them (see UIDataTypeUtils). Written by id like every other
 * reference — and under the DTO's names `customer`/`project`, not the entity's `kunde`/`projekt`.
 */
const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/**
 * One order position. Kept in the values even when deleted, with `deleted = true`: the backend's
 * `CollectionHandler` physically removes — history and all — whatever a posted collection leaves out
 * (`AuftragDO.positionen` has `autoUpdateCollectionEntries` but no `@SoftDeleteCollection`).
 *
 * `number` travels back untouched: `AuftragsPositionDO.equals` matches on it together with the order,
 * so renumbering an existing position would read as "removed and added" to that same handler.
 */
export const orderPositionSchema = z.object({
  id: z.number().nullable(),
  deleted: z.boolean(),
  number: z.number().nullable(),
  titel: p.nullableString("titel"),
  art: p.enumField("art"),
  paymentType: p.enumField("paymentType"),
  forecastType: p.enumField("forecastType"),
  status: p.enumField("status"),
  nettoSumme: p.decimalField("nettoSumme"),
  personDays: p.decimalField("personDays"),
  bemerkung: p.nullableString("bemerkung"),
  vollstaendigFakturiert: p.booleanField("vollstaendigFakturiert"),
  periodOfPerformanceType: p.enumField("periodOfPerformanceType"),
  periodOfPerformanceBegin: p.nullableString("periodOfPerformanceBegin"),
  periodOfPerformanceEnd: p.nullableString("periodOfPerformanceEnd"),
  modeOfPaymentType: p.enumField("modeOfPaymentType"),
  task: p.entityField("task"),
});

/** One instalment of the payment schedule. `positionNumber` refers to a position's number, not its id. */
export const paymentScheduleSchema = z.object({
  id: z.number().nullable(),
  deleted: z.boolean(),
  number: z.number().nullable(),
  positionNumber: s.intField("positionNumber"),
  scheduleDate: s.nullableString("scheduleDate"),
  amount: s.decimalField("amount"),
  comment: s.nullableString("comment"),
  reached: s.booleanField("reached"),
  vollstaendigFakturiert: s.booleanField("vollstaendigFakturiert"),
});

export const orderSchema = z.object({
  // null while the order is new — Spring assigns the id, and `nummer` on the first save.
  id: z.number().nullable(),
  /**
   * Not `m.intField("nummer")`, the one rule deliberately taken away from the metadata: the column is
   * `nullable = false`, so the entity reports it as mandatory, but the form can never supply it —
   * `OrderEntityRest.transformForDB` assigns it from `AuftragDao.getNextNumber` when it is missing.
   * Validating it here would refuse to save any new order. The field is read-only in the form for the
   * same reason.
   */
  nummer: z.number().nullable(),
  titel: m.requiredString("titel"),
  referenz: m.nullableString("referenz"),
  status: m.enumField("status"),
  customer: entityRef,
  kundeText: m.nullableString("kundeText"),
  project: entityRef,
  contactPerson: m.entityField("contactPerson"),
  projectManager: m.entityField("projectManager"),
  headOfBusinessManager: m.entityField("headOfBusinessManager"),
  salesManager: m.entityField("salesManager"),
  erfassungsDatum: m.nullableString("erfassungsDatum"),
  angebotsDatum: m.nullableString("angebotsDatum"),
  entscheidungsDatum: m.nullableString("entscheidungsDatum"),
  bindungsFrist: m.nullableString("bindungsFrist"),
  beauftragungsDatum: m.nullableString("beauftragungsDatum"),
  /**
   * Held but never shown: `AuftragDO` has had the column since 2016, yet no frontend ever offered a box
   * for it — neither `AuftragEditForm` nor the legacy React page — so the next page doesn't either.
   *
   * Still part of the values, and it has to be: the form posts the whole DTO, and a key Spring doesn't
   * receive leaves the DTO's field null, which `Auftrag.copyTo` would write over the stored text. Whatever
   * an import or a historic entry put there travels back untouched.
   */
  beauftragungsBeschreibung: m.nullableString("beauftragungsBeschreibung"),
  periodOfPerformanceBegin: m.nullableString("periodOfPerformanceBegin"),
  periodOfPerformanceEnd: m.nullableString("periodOfPerformanceEnd"),
  // A percentage. The bound is the field's own meaning, not a column length, hence declared here.
  probabilityOfOccurrence: m.intField("probabilityOfOccurrence", {
    min: 0,
    max: 100,
  }),
  forecastType: m.enumField("forecastType"),
  bemerkung: m.nullableString("bemerkung"),
  statusBeschreibung: m.nullableString("statusBeschreibung"),
  positionen: z.array(orderPositionSchema),
  paymentSchedules: z.array(paymentScheduleSchema),
  // Not a property of the entity: it tells `onAfterSaveOrUpdate` whether to notify the contact person.
  sendEMailNotification: z.boolean(),
  created: m.nullableString("created"),
});

export type OrderValues = z.infer<typeof orderSchema>;
export type OrderPositionValues = z.infer<typeof orderPositionSchema>;
export type PaymentScheduleValues = z.infer<typeof paymentScheduleSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees. The nested paths of
 * the two collections are matched by their array's name, which is in here.
 */
export const ORDER_FIELDS = Object.keys(
  orderSchema.shape
) as readonly (keyof OrderValues)[];

/**
 * Names of the array fields of this form. A bare server error on one of these (e.g. "order has no
 * positions") has no mounted `<form.Field>` to display it and must surface as a toast instead of
 * being silently dropped into a field slot that nobody reads.
 */
export const ORDER_ARRAY_FIELDS: readonly string[] = [
  "positionen",
  "paymentSchedules",
];
