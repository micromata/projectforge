import { z } from "zod";
import { BANK_ACCOUNT_RECORD_METADATA } from "@/lib/metadata/bank-account-record.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * The rules BankAccountRecordDO carries come from `lib/metadata/bank-account-record.generated.ts` through
 * [fromMetadata]. Added by hand: the account, which the DAO requires (a transaction belongs to an account) and the
 * backend takes from the database for a stored record anyway (BankAccountRecordEntityRest.transformForDB).
 *
 * Which fields the form has mirrors org.projectforge.plugins.banking.BankAccountRecord: a save posts these values
 * *as* the DTO, so a field left out here would reach `copyTo` as null and overwrite the stored one.
 */
const m = fromMetadata(BANK_ACCOUNT_RECORD_METADATA);

export const bankAccountRecordSchema = z.object({
  // null while the record is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  bankAccount: z
    .looseObject({ id: z.number(), displayName: z.string().optional() })
    .nullable()
    .refine((v): boolean => v != null, REQUIRED),
  date: m.instantField("date"),
  valueDate: m.instantField("valueDate"),
  amount: m.decimalField("amount"),
  currency: m.nullableString("currency"),
  type: m.nullableString("type"),
  info: m.nullableString("info"),
  receiverSender: m.nullableString("receiverSender"),
  iban: m.nullableString("iban"),
  bic: m.nullableString("bic"),
  subject: m.nullableString("subject"),
  comment: m.nullableString("comment"),
  debteeId: m.nullableString("debteeId"),
  customerReference: m.nullableString("customerReference"),
  mandateReference: m.nullableString("mandateReference"),
  collectionReference: m.nullableString("collectionReference"),
});

export type BankAccountRecordValues = z.infer<typeof bankAccountRecordSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const BANK_ACCOUNT_RECORD_FIELDS = Object.keys(
  bankAccountRecordSchema.shape
) as readonly (keyof BankAccountRecordValues)[];
