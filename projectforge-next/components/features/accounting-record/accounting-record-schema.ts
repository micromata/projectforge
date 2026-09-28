import { z } from "zod";
import { BUCHUNGSSATZ_METADATA } from "@/lib/metadata/buchungssatz.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

const m = fromMetadata(BUCHUNGSSATZ_METADATA);

/** A `{id, displayName}` reference (kost1/kost2/konto/gegenKonto), nullable. */
const entityRef = z
  .looseObject({
    id: z.number(),
    displayName: z.string().nullable().optional(),
  })
  .nullable();

/**
 * Edit form of an accounting record. `satznr` (the formatted record number) is display only — it is derived
 * from year/month/satznr and cannot round-trip through the DTO — so it is not part of the writable schema.
 */
export const accountingRecordSchema = z.object({
  id: z.number().nullable(),
  datum: m.nullableString("datum"),
  year: m.intField("year"),
  month: m.intField("month"),
  betrag: m.decimalField("betrag"),
  sh: m.enumField("sh"),
  beleg: m.nullableString("beleg"),
  text: m.nullableString("text"),
  menge: m.nullableString("menge"),
  comment: m.nullableString("comment"),
  kost1: entityRef,
  kost2: entityRef,
  konto: entityRef,
  gegenKonto: entityRef,
});

export type AccountingRecordValues = z.infer<typeof accountingRecordSchema>;

export const ACCOUNTING_RECORD_FIELDS = Object.keys(
  accountingRecordSchema.shape
) as readonly (keyof AccountingRecordValues)[];
