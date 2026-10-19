import { z } from "zod";
import { LEAVE_ACCOUNT_ENTRY_METADATA } from "@/lib/metadata/leave-account-entry.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * The rules LeaveAccountEntryDO carries — date and amount mandatory, the maximum length of the description — come
 * from `lib/metadata/leave-account-entry.generated.ts` through [fromMetadata]. Added by hand: the employee, which
 * the entity marks optional but the React form required as well (a correction belongs to somebody's account). That
 * the amount has at most one decimal place is checked by the backend (LeaveAccountEntryEntityRest.validate).
 *
 * Which fields the form has mirrors org.projectforge.rest.dto.LeaveAccountEntry: a save posts these values *as*
 * the DTO, so a field left out here would reach `copyTo` as null and overwrite the stored one.
 */
const m = fromMetadata(LEAVE_ACCOUNT_ENTRY_METADATA);

export const leaveAccountEntrySchema = z.object({
  // null while the entry is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  employee: m
    .entityField("employee")
    .refine((v): boolean => v != null, REQUIRED),
  date: m.instantField("date"),
  amount: m.decimalField("amount"),
  description: m.nullableString("description"),
});

export type LeaveAccountEntryValues = z.infer<typeof leaveAccountEntrySchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const LEAVE_ACCOUNT_ENTRY_FIELDS = Object.keys(
  leaveAccountEntrySchema.shape
) as readonly (keyof LeaveAccountEntryValues)[];
