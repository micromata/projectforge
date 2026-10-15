import { z } from "zod";
import { KONTO_METADATA } from "@/lib/metadata/konto.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length, the constants of the status enum — comes from KontoDO
 * through `lib/metadata/konto.generated.ts`. Which fields the form has mirrors
 * org.projectforge.rest.dto.Konto; what each field *allows* is not restated here.
 */
const m = fromMetadata(KONTO_METADATA);

export const accountSchema = z.object({
  // The range Wicket's `KontoEditForm` bounds the number to (MinMaxNumberField 0..99999999); the entity
  // declares none. A duplicate is rejected by the backend (`fibu.konto.validate.duplicate`).
  nummer: m.intField("nummer", { min: 0, max: 99_999_999 }),
  status: m.enumField("status"),
  bezeichnung: m.requiredString("bezeichnung"),
  description: m.nullableString("description"),
  contactPerson: m.nullableString("contactPerson"),
  street: m.nullableString("street"),
  zipCode: m.nullableString("zipCode"),
  city: m.nullableString("city"),
  country: m.nullableString("country"),
  vatId: m.nullableString("vatId"),
  leitwegId: m.nullableString("leitwegId"),
  eInvoiceEmail: m.nullableString("eInvoiceEmail"),
  sellerBankAccountName: m.nullableString("sellerBankAccountName"),
});

export type AccountValues = z.infer<typeof accountSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const ACCOUNT_FIELDS = Object.keys(
  accountSchema.shape
) as readonly (keyof AccountValues)[];
