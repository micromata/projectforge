import { z } from "zod";
import { ABSENCE_KINDS } from "@/lib/rs/absence-kinds";
import { TEAM_CAL_METADATA } from "@/lib/metadata/team-cal.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from TeamCalDO through `lib/metadata/team-cal.generated.ts`. Which fields
 * the form has mirrors org.projectforge.rest.dto.TeamCal.
 *
 * Every property of the calendar is part of it, rendered or not: a save posts these values *as* the DTO, so a
 * field left out here would reach `TeamCal.copyTo` as null and overwrite the stored one — the subscription url
 * above all, which only users allowed to save receive.
 */
const m = fromMetadata(TEAM_CAL_METADATA);

const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

export const teamCalSchema = z.object({
  // null while the calendar is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  title: m.requiredString("title"),
  description: m.nullableString("description"),
  owner: m.entityField("owner"),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  fullAccessUsers: z.array(ref),
  fullAccessGroups: z.array(ref),
  readonlyAccessUsers: z.array(ref),
  readonlyAccessGroups: z.array(ref),
  minimalAccessUsers: z.array(ref),
  minimalAccessGroups: z.array(ref),
  includeLeaveDaysForUsers: z.array(ref),
  includeLeaveDaysForGroups: z.array(ref),
  // All of them are stored as null (TeamCal.copyTo), as for the calendars stored before.
  absenceKinds: z.array(z.enum(ABSENCE_KINDS)).min(1),
  externalSubscription: m.booleanField("externalSubscription"),
  externalSubscriptionUrl: m.nullableString("externalSubscriptionUrl"),
  externalSubscriptionUpdateInterval: m.intField(
    "externalSubscriptionUpdateInterval"
  ),
});

export type TeamCalValues = z.infer<typeof teamCalSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const TEAM_CAL_FIELDS = Object.keys(
  teamCalSchema.shape
) as readonly (keyof TeamCalValues)[];
