import type { BankAccountRecordValues } from "./bank-account-record-schema";
import type {
  BankAccountRecordAccount,
  BankAccountRecordDetail,
} from "./types";
import { toPickerRef } from "@/lib/page-def/reference";

/** How an account reads in the picker and the list: its name, else its IBAN. */
export function accountLabel(
  account: BankAccountRecordAccount | null | undefined
): string {
  return account?.name || account?.iban || "";
}

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every value is
 * normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(
  record: BankAccountRecordDetail
): BankAccountRecordValues {
  return {
    id: record.id ?? null,
    // The reference the picker binds to (see toPickerRef).
    bankAccount: toPickerRef(
      record.bankAccount,
      record.bankAccount ? accountLabel(record.bankAccount) : null
    ),
    date: record.date ?? null,
    valueDate: record.valueDate ?? null,
    amount: record.amount ?? null,
    currency: record.currency ?? null,
    type: record.type ?? null,
    info: record.info ?? null,
    receiverSender: record.receiverSender ?? null,
    iban: record.iban ?? null,
    bic: record.bic ?? null,
    subject: record.subject ?? null,
    comment: record.comment ?? null,
    debteeId: record.debteeId ?? null,
    customerReference: record.customerReference ?? null,
    mandateReference: record.mandateReference ?? null,
    collectionReference: record.collectionReference ?? null,
  };
}

/** Blank form for a transaction that doesn't exist yet (`/rs/bankAccountRecord/newEntry`). */
export function emptyBankAccountRecordValues(): BankAccountRecordValues {
  return toFormValues({ id: null });
}
