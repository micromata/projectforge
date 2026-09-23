import { z } from "zod";
import { KUNDE_METADATA } from "@/lib/metadata/kunde.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * Every rule below — mandatory, maximum length, the constants of the status enum — comes from KundeDO
 * through `lib/metadata/kunde.generated.ts`. Hand-written are only the fields the metadata cannot
 * describe: the referenced `konto` (a foreign DO, no `UIDataType`), and the range of `nummer` (see
 * below).
 */
const m = fromMetadata(KUNDE_METADATA);

/** The account, as `Customer.konto` carries it: the id is what `copyTo` resolves the KontoDO by. */
const kontoRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Customer — a hand-written decision,
 * because the DTO has neither the field set nor the names of the DO.
 *
 * `kost` and `created` are carried without being edited: a save posts these values *as* the DTO
 * (there is no merge with what was loaded), the same way the legacy frontends post the DTO they were
 * given. `kost` is a read-only computed number; leaving it out changes nothing the entity would keep,
 * but carrying it keeps the round trip honest.
 */
export const customerSchema = z.object({
  // Equals `nummer` once saved; null while the customer is new (see types.ts).
  id: z.number().nullable(),
  /**
   * The customer number, the entity's user-assigned id. Required (a customer has no identity without
   * one) and bounded 0..999 — `KundeDO.MAX_ID`, which the metadata cannot carry (no `@PropertyInfo`
   * min/max on `nummer`). Uniqueness is the backend's check
   * (`fibu.kunde.validation.existingCustomerNr`).
   */
  nummer: m
    .intField("nummer", { min: 0, max: 999 })
    .refine((v): boolean => v != null, REQUIRED),
  name: m.requiredString("name"),
  identifier: m.nullableString("identifier"),
  division: m.nullableString("division"),
  status: m.enumField("status"),
  description: m.nullableString("description"),
  konto: kontoRef.nullable(),
  kost: z.string().nullable(),
  created: z.string().nullable(),
});

export type CustomerValues = z.infer<typeof customerSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const CUSTOMER_FIELDS = Object.keys(
  customerSchema.shape
) as readonly (keyof CustomerValues)[];
