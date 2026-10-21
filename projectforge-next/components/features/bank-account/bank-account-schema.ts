import { z } from "zod";
import { BANK_ACCOUNT_METADATA } from "@/lib/metadata/bank-account.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from BankAccountDO through `lib/metadata/bank-account.generated.ts`. Which
 * fields the form has mirrors org.projectforge.rest.dto.BankAccount.
 *
 * Every property of the account is part of it, the minimal access lists too, which the form doesn't offer: a save
 * posts these values *as* the DTO, so a field left out here would reach `BankAccount.copyTo` as null and overwrite
 * the stored one.
 */
const m = fromMetadata(BANK_ACCOUNT_METADATA);

const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

export const bankAccountSchema = z.object({
  // null while the account is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  name: m.requiredString("name"),
  bank: m.requiredString("bank"),
  iban: m.nullableString("iban"),
  bic: m.nullableString("bic"),
  description: m.nullableString("description"),
  importSettings: m.nullableString("importSettings"),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  fullAccessUsers: z.array(ref),
  fullAccessGroups: z.array(ref),
  readonlyAccessUsers: z.array(ref),
  readonlyAccessGroups: z.array(ref),
  minimalAccessUsers: z.array(ref),
  minimalAccessGroups: z.array(ref),
});

export type BankAccountValues = z.infer<typeof bankAccountSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const BANK_ACCOUNT_FIELDS = Object.keys(
  bankAccountSchema.shape
) as readonly (keyof BankAccountValues)[];

/** The lists of users and groups, the array fields of the form. */
export const BANK_ACCOUNT_ARRAY_FIELDS = [
  "fullAccessUsers",
  "fullAccessGroups",
  "readonlyAccessUsers",
  "readonlyAccessGroups",
  "minimalAccessUsers",
  "minimalAccessGroups",
] as const;
