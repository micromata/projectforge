import { z } from "zod";
import { LIQUIDITY_ENTRY_METADATA } from "@/lib/metadata/liquidity-entry.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * The rules of the liquidity entry form, taken from `LiquidityEntryDO` through
 * `lib/metadata/liquidity-entry.generated.ts` — the same source the field components read.
 *
 * The generated metadata reports every field as `required: false`: `LiquidityEntryDO` declares no
 * `@PropertyInfo(required = true)` and its columns are nullable. The Wicket form, however, made **amount**
 * and **subject** mandatory (`LiquidityEntryEditForm`), and `LiquidityEntryDao.onSaveOrModify` rejects a
 * missing subject. So the two are marked required here explicitly, the way the customer form does for its
 * `nummer` — the metadata cannot express it, and the backend refuses the write regardless.
 */
const m = fromMetadata(LIQUIDITY_ENTRY_METADATA);

export const liquiditySchema = z.object({
  // null while the entry is new — Spring assigns the id.
  id: z.number().nullable(),
  dateOfPayment: m.nullableString("dateOfPayment"),
  // Required, unlike what the metadata reports (see the module comment): an entry without an amount plans
  // no cash flow, and the Wicket form never allowed one.
  amount: m.decimalField("amount").refine((v): boolean => v != null, REQUIRED),
  // Three-state override, unlike the metadata's plain BOOLEAN (see LiquidityEntryDO.paid): null follows
  // the autoSetPaid rule, true/false force the status. `effectivePaid = paid ?? (autoSetPaid && …)`.
  paid: m.booleanField("paid").nullable(),
  // Once set, the entry counts as paid after its date of payment has passed — unless `paid` overrides it.
  autoSetPaid: m.booleanField("autoSetPaid"),
  // Required for the same reason, and the DAO refuses a blank subject. `requiredString` keeps "" for an
  // emptied input (never null), the invariant the text field relies on — the customer's `name` does the same.
  subject: m.requiredString("subject"),
  comment: m.nullableString("comment"),
  // The series this materialized occurrence belongs to (null for a plain entry). Read-only in the form —
  // it drives the "part of series …" link but is never edited here. The occurrence's stable anchor day is
  // its identity within the series; the backend sets it from the prefill, so the form only carries it back.
  seriesId: z.number().nullable(),
  seriesDate: z.string().nullable(),
  // The "repeat" block that turns a *new* entry into a recurring series. Only read when the entry is new and
  // has no seriesId (see repeat-fields.tsx); the backend creates the LiquiditySeriesDO from it on first save.
  repeat: z.object({
    enabled: z.boolean(),
    intervalMonths: z.number().int().min(1),
    // null = endless.
    count: z.number().int().min(1).nullable(),
  }),
  created: m.nullableString("created"),
});

export type LiquidityValues = z.infer<typeof liquiditySchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders (see
 * applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const LIQUIDITY_FIELDS = Object.keys(
  liquiditySchema.shape
) as readonly (keyof LiquidityValues)[];
