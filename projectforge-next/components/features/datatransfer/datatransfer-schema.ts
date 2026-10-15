import { z } from "zod";
import { DATA_TRANSFER_AREA_METADATA } from "@/lib/metadata/data-transfer-area.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from DataTransferAreaDO through
 * `lib/metadata/data-transfer-area.generated.ts`. Which fields the form has mirrors
 * org.projectforge.plugins.datatransfer.rest.DataTransferArea.
 *
 * The rules of the external access (a token and a password of six characters at least, once a flag is
 * set) and the allowed choices of the two selects stay the backend's (`DataTransferAreaEntityRest.validate`):
 * the form fills token and password in by itself (see DataTransferExternalAccess), so a refusal there is
 * an edge case the server's message covers.
 */
const m = fromMetadata(DATA_TRANSFER_AREA_METADATA);

/** A user or group, as the DTO carries it: the id is what `copyTo` stores. */
const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/**
 * `created` and `personalBox` are carried without being rendered: a save posts these values *as* the DTO,
 * and `copyTo` reads `personalBox` to restore a box's stored name.
 */
export const dataTransferSchema = z.object({
  // null while the area is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  areaName: m.requiredString("areaName"),
  description: m.nullableString("description"),
  expiryDays: m.intField("expiryDays"),
  maxUploadSizeKB: m.intField("maxUploadSizeKB"),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  admins: z.array(ref),
  observers: z.array(ref),
  accessUsers: z.array(ref),
  accessGroups: z.array(ref),
  externalDownloadEnabled: m.booleanField("externalDownloadEnabled"),
  externalUploadEnabled: m.booleanField("externalUploadEnabled"),
  externalAccessToken: m.nullableString("externalAccessToken"),
  externalPassword: m.nullableString("externalPassword"),
  personalBox: z.boolean().nullable(),
  created: z.string().nullable(),
});

export type DataTransferValues = z.infer<typeof dataTransferSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const DATA_TRANSFER_FIELDS = Object.keys(
  dataTransferSchema.shape
) as readonly (keyof DataTransferValues)[];
