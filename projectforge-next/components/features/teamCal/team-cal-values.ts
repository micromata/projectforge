import type { TeamCalValues } from "./team-cal-schema";
import type { TeamCalDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every value is
 * normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(cal: TeamCalDetail): TeamCalValues {
  return {
    id: cal.id ?? null,
    title: cal.title ?? "",
    description: cal.description ?? null,
    owner: cal.owner ?? null,
    fullAccessUsers: cal.fullAccessUsers ?? [],
    fullAccessGroups: cal.fullAccessGroups ?? [],
    readonlyAccessUsers: cal.readonlyAccessUsers ?? [],
    readonlyAccessGroups: cal.readonlyAccessGroups ?? [],
    minimalAccessUsers: cal.minimalAccessUsers ?? [],
    minimalAccessGroups: cal.minimalAccessGroups ?? [],
    includeLeaveDaysForUsers: cal.includeLeaveDaysForUsers ?? [],
    includeLeaveDaysForGroups: cal.includeLeaveDaysForGroups ?? [],
    externalSubscription: cal.externalSubscription ?? false,
    externalSubscriptionUrl: cal.externalSubscriptionUrl ?? null,
    externalSubscriptionUpdateInterval:
      cal.externalSubscriptionUpdateInterval ?? null,
  };
}

/**
 * Blank form for a calendar that doesn't exist yet. The backend presets the logged-in user as owner
 * (`TeamCalEntityRest.newBaseDTO`), and its `/rs/teamCal/newEntry` answer is what the form is reset onto.
 */
export function emptyTeamCalValues(): TeamCalValues {
  return toFormValues({ id: null });
}
