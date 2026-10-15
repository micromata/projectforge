import { z } from "zod";
import { ADDRESSBOOK_METADATA } from "@/lib/metadata/addressbook.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from AddressbookDO through `lib/metadata/addressbook.generated.ts`. Which
 * fields the form has mirrors org.projectforge.rest.dto.Addressbook.
 *
 * Every property of the address book is part of it: a save posts these values *as* the DTO, so a field left out
 * here would reach `Addressbook.copyTo` as null and overwrite the stored one.
 */
const m = fromMetadata(ADDRESSBOOK_METADATA);

const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

export const addressbookSchema = z.object({
  // null while the address book is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  title: m.requiredString("title"),
  description: m.nullableString("description"),
  owner: m.entityField("owner"),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  fullAccessUsers: z.array(ref),
  fullAccessGroups: z.array(ref),
  readonlyAccessUsers: z.array(ref),
  readonlyAccessGroups: z.array(ref),
});

export type AddressbookValues = z.infer<typeof addressbookSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const ADDRESSBOOK_FIELDS = Object.keys(
  addressbookSchema.shape
) as readonly (keyof AddressbookValues)[];
