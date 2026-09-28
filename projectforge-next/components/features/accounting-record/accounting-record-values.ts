import type { AccountingRecordValues } from "./accounting-record-schema";
import type { AccountingRecordDetail, EntityRef } from "./types";

function ref(value: EntityRef | null | undefined): EntityRef | null {
  return value ?? null;
}

export function toFormValues(
  record: AccountingRecordDetail
): AccountingRecordValues {
  return {
    id: record.id ?? null,
    datum: record.datum ?? null,
    year: record.year ?? null,
    month: record.month ?? null,
    betrag: record.betrag ?? null,
    sh: record.sh ?? null,
    beleg: record.beleg ?? null,
    text: record.text ?? null,
    menge: record.menge ?? null,
    comment: record.comment ?? null,
    kost1: ref(record.kost1),
    kost2: ref(record.kost2),
    konto: ref(record.konto),
    gegenKonto: ref(record.gegenKonto),
  };
}

export function emptyAccountingRecordValues(): AccountingRecordValues {
  return {
    id: null,
    datum: null,
    year: null,
    month: null,
    betrag: null,
    sh: null,
    beleg: null,
    text: null,
    menge: null,
    comment: null,
    kost1: null,
    kost2: null,
    konto: null,
    gegenKonto: null,
  };
}
