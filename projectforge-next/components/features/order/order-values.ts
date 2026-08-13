import type {
  OrderPositionValues,
  OrderValues,
  PaymentScheduleValues,
} from "./order-schema";
import type {
  OrderDetail,
  OrderPositionDto,
  PaymentScheduleDto,
} from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 *
 * Module level and never wrapped in a hook: `useEntityEditForm` resets the form whenever this function
 * changes identity, so a per-render one would reset on every render and throw away what is being typed.
 */
export function toFormValues(order: OrderDetail): OrderValues {
  return {
    id: order.id ?? null,
    nummer: order.nummer ?? null,
    // "" rather than null: the title is mandatory, and an emptied input has to stay a string (see
    // `requiredString` in from-metadata.ts).
    titel: order.titel ?? "",
    referenz: order.referenz ?? null,
    status: order.status ?? null,
    customer: order.customer ?? null,
    kundeText: order.kundeText ?? null,
    project: order.project ?? null,
    contactPerson: order.contactPerson ?? null,
    projectManager: order.projectManager ?? null,
    headOfBusinessManager: order.headOfBusinessManager ?? null,
    salesManager: order.salesManager ?? null,
    erfassungsDatum: order.erfassungsDatum ?? null,
    angebotsDatum: order.angebotsDatum ?? null,
    entscheidungsDatum: order.entscheidungsDatum ?? null,
    bindungsFrist: order.bindungsFrist ?? null,
    beauftragungsDatum: order.beauftragungsDatum ?? null,
    beauftragungsBeschreibung: order.beauftragungsBeschreibung ?? null,
    periodOfPerformanceBegin: order.periodOfPerformanceBegin ?? null,
    periodOfPerformanceEnd: order.periodOfPerformanceEnd ?? null,
    probabilityOfOccurrence: order.probabilityOfOccurrence ?? null,
    forecastType: order.forecastType ?? null,
    bemerkung: order.bemerkung ?? null,
    statusBeschreibung: order.statusBeschreibung ?? null,
    // Deleted rows are kept — see the comment on `orderPositionSchema`.
    positionen: (order.positionen ?? []).map(toPositionValues),
    paymentSchedules: (order.paymentSchedules ?? []).map(toScheduleValues),
    // The backend decides whether the notification is offered at all and preselects it (contact person
    // ≠ the user editing); from here on it is the user's choice.
    sendEMailNotification: order.sendEMailNotification === true,
    created: order.created ?? null,
  };
}

function toPositionValues(pos: OrderPositionDto): OrderPositionValues {
  return {
    id: pos.id ?? null,
    deleted: pos.deleted === true,
    number: pos.number ?? null,
    titel: pos.titel ?? null,
    art: pos.art ?? null,
    paymentType: pos.paymentType ?? null,
    forecastType: pos.forecastType ?? null,
    status: pos.status ?? null,
    nettoSumme: pos.nettoSumme ?? null,
    personDays: pos.personDays ?? null,
    bemerkung: pos.bemerkung ?? null,
    vollstaendigFakturiert: pos.vollstaendigFakturiert === true,
    // SEEABOVE is the entity's own default (`AuftragsPositionDO`): a position follows the order's
    // period unless it is given one.
    periodOfPerformanceType: pos.periodOfPerformanceType ?? "SEEABOVE",
    periodOfPerformanceBegin: pos.periodOfPerformanceBegin ?? null,
    periodOfPerformanceEnd: pos.periodOfPerformanceEnd ?? null,
    modeOfPaymentType: pos.modeOfPaymentType ?? null,
    task: pos.task ?? null,
  };
}

function toScheduleValues(schedule: PaymentScheduleDto): PaymentScheduleValues {
  return {
    id: schedule.id ?? null,
    deleted: schedule.deleted === true,
    number: schedule.number ?? null,
    positionNumber: schedule.positionNumber ?? null,
    scheduleDate: schedule.scheduleDate ?? null,
    amount: schedule.amount ?? null,
    comment: schedule.comment ?? null,
    reached: schedule.reached === true,
    vollstaendigFakturiert: schedule.vollstaendigFakturiert === true,
  };
}

/**
 * Blank form for an order that doesn't exist yet — the empty DTO run through the very same
 * normalisation, rather than a second list of the same fields: two lists are two places for a field to
 * be forgotten in, and the one that would be forgotten is this one.
 *
 * Nothing is proposed here, dates and status included: the backend presets them in `newBaseDTO`
 * (offer/entry/decision date = today, contact person = the logged-in user when they are a project
 * manager), and the edit page fetches `/rs/order/edit` for a new entry — so those are the values a user
 * actually sees. This is only the shape the form starts out with.
 */
export function emptyOrderValues(): OrderValues {
  return toFormValues({ id: null });
}

/**
 * A fresh position, with the defaults its entity brings.
 *
 * The number is the form's own, provisional one, not the backend's: the payment schedule refers to a
 * position **by number** (`PaymentScheduleDO.positionNumber`), so a position without one cannot be
 * picked as an instalment's position — which used to leave the select of a new order empty. The backend
 * still has the last word and renumbers every new row on save (`AuftragPagesRest.transformForDB`),
 * carrying the schedules along; it just needs something to carry.
 *
 * @param number What [nextPositionNumber] yields for the rows the form currently holds.
 */
export function emptyPositionValues(number: number): OrderPositionValues {
  return toPositionValues({ number });
}

/**
 * The number the next position gets: one past the highest in the form, deleted and stored rows
 * included — a number is what a schedule and the backend's collection handler identify a position by,
 * so reusing one would re-point whatever still refers to it.
 */
export function nextPositionNumber(
  positions: readonly OrderPositionValues[]
): number {
  return positions.reduce((max, pos) => Math.max(max, pos.number ?? 0), 0) + 1;
}

/**
 * A fresh instalment of the payment schedule, carrying the number it will be stored with.
 *
 * Numbered here rather than only on save for the same reason a position is: the header of a row shows
 * its number, and a preview that differs from what the backend then assigns is worse than none.
 *
 * @param number What [nextScheduleNumber] yields for the rows the form currently holds.
 */
export function emptyScheduleValues(number: number): PaymentScheduleValues {
  return toScheduleValues({ number });
}

/**
 * The number the next instalment gets: one past the highest in the form, deleted and stored rows
 * included — `PaymentScheduleDO`'s identity is `(number, auftrag)` and its history key is
 * `payment#<number>`, so reusing one would merge a new row with a deleted row's past.
 */
export function nextScheduleNumber(
  schedules: readonly PaymentScheduleValues[]
): number {
  return (
    schedules.reduce(
      (max, schedule) => Math.max(max, schedule.number ?? 0),
      0
    ) + 1
  );
}
