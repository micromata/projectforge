import { z } from "zod";
import { LICENSE_METADATA } from "@/lib/metadata/license.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory, maximum length and the range of the number of licenses come from LicenseDO through
 * `lib/metadata/license.generated.ts`. Which fields the form has mirrors
 * org.projectforge.plugins.licensemanagement.dto.License.
 */
const m = fromMetadata(LICENSE_METADATA);

/** An owner, as `License.owners` carries it: the id is what `copyTo` stores. */
const userRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/**
 * `status` and `created` are carried without being rendered: a save posts these values *as* the DTO, so
 * a field left out here would reach `License.copyTo` as null and overwrite the stored one. The key is
 * carried too, but the backend keeps the stored one for a user who may not see it
 * (`LicenseEntityRest.transformForDB`), the same as the files, which have endpoints of their own.
 */
export const licenseSchema = z.object({
  // null while the license is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  organization: m.nullableString("organization"),
  product: m.requiredString("product"),
  version: m.requiredString("version"),
  updateFromVersion: m.nullableString("updateFromVersion"),
  device: m.nullableString("device"),
  numberOfLicenses: m.intField("numberOfLicenses"),
  /**
   * No metadata: the owners are stored as a csv of user ids (`LicenseDO.ownerIds`), which the DTO
   * resolves to users. An empty list rather than null, which is what the picker holds.
   */
  owners: z.array(userRef),
  validSince: m.nullableString("validSince"),
  validUntil: m.nullableString("validUntil"),
  licenseHolder: m.nullableString("licenseHolder"),
  key: m.nullableString("key"),
  comment: m.nullableString("comment"),
  status: z.string().nullable(),
  created: z.string().nullable(),
});

export type LicenseValues = z.infer<typeof licenseSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const LICENSE_FIELDS = Object.keys(
  licenseSchema.shape
) as readonly (keyof LicenseValues)[];
