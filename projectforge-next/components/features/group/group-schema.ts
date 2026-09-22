import { z } from "zod";
import { GROUP_METADATA } from "@/lib/metadata/group.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { INTEGER } from "@/lib/validation/markers";

/**
 * Every rule below — maximum length, whether a field is mandatory — comes from GroupDO through
 * `lib/metadata/group.generated.ts`. Hand-written are only the three fields the metadata cannot
 * describe, because GroupDO has no such property: the members (a collection), the LDAP gid and the
 * computed mail addresses.
 */
const m = fromMetadata(GROUP_METADATA);

/** A member, as `Group.assignedUsers` carries it: the id is what `copyTo` resolves the user by. */
const userRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Group — a hand-written decision,
 * because the DTO has neither the field set nor the names of the DO.
 *
 * `ldapValues` and `created` are carried without being rendered: a save posts these values *as* the
 * DTO (there is no merge with what was loaded), so a field left out here would reach `Group.copyTo`
 * as null and overwrite what the LDAP sync had written. The legacy frontends keep them for the same
 * reason — they post the DTO they were given, unchanged.
 */
export const groupSchema = z.object({
  // null while the group is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  name: m.nullableString("name"),
  organization: m.nullableString("organization"),
  description: m.nullableString("description"),
  localGroup: m.booleanField("localGroup"),
  groupOwner: m.entityField("groupOwner"),
  /**
   * The members. No metadata: a collection is no `@PropertyInfo` field of GroupDO, so nothing
   * generated describes it. An empty list rather than null, which is what the picker holds.
   */
  assignedUsers: z.array(userRef),
  /**
   * The LDAP gid, editable only where posix accounts are configured (see [LdapGidField]). Whole
   * number, as `Group.gidNumber` is an `Int`; uniqueness is the backend's check
   * (`GroupPagesRest.validate` answers `ldap.gidNumber.alreadyInUse`).
   */
  gidNumber: z
    .number()
    .nullable()
    .refine((v) => v == null || Number.isInteger(v), INTEGER),
  /** Read-only flag of the request, not a property of the group — see `GroupDetail`. */
  ldapPosixConfigured: z.boolean(),
  /** Computed on read (`Group.populateEmails`), shown read-only, sent back untouched. */
  emails: z.string().nullable(),
  ldapValues: m.nullableString("ldapValues"),
  created: z.string().nullable(),
});

export type GroupValues = z.infer<typeof groupSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const GROUP_FIELDS = Object.keys(
  groupSchema.shape
) as readonly (keyof GroupValues)[];
