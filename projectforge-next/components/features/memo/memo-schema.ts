import { z } from "zod";
import { MEMO_METADATA } from "@/lib/metadata/memo.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Maximum lengths come from MemoDO through `lib/metadata/memo.generated.ts`. Which fields the form has
 * mirrors the data object, which MemoEntityRest transfers without a DTO.
 */
const m = fromMetadata(MEMO_METADATA);

/** The owner, as `MemoDO` serializes it: the id only. */
const ownerRef = z.looseObject({
  id: z.number(),
});

/**
 * The owner and `created` are carried without being edited: a save posts these values *as* the data
 * object, so a field left out here would reach the update as null. The owner is the logged-in user anyway
 * (`MemoDao.onInsertOrModify`).
 */
export const memoSchema = z.object({
  // null while the memo is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  subject: m.nullableString("subject"),
  memo: m.nullableString("memo"),
  owner: ownerRef.nullable(),
  created: z.string().nullable(),
});

export type MemoValues = z.infer<typeof memoSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const MEMO_FIELDS = Object.keys(
  memoSchema.shape
) as readonly (keyof MemoValues)[];
