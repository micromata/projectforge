import type { CalendarSubscriptionInfo } from "@/lib/rs/calendar-subscription";

/** A user or group as the DTO carries it: the id is what `copyTo` stores. */
// A type alias, not an interface: only an alias is assignable to the schema's loose object.
export type TeamCalRef = {
  id: number;
  displayName?: string;
};

/** `CalendarAccessStatus` of the logged-in user for a calendar. */
export type CalendarAccessStatus =
  | "OWNER"
  | "FULL_ACCESS"
  | "READONLY_ACCESS"
  | "MINIMAL_ACCESS"
  | "ADMIN_ACCESS";

/** org.projectforge.rest.dto.TeamCal. */
export interface TeamCalDetail {
  id: number | null;
  title?: string;
  description?: string | null;
  owner?: TeamCalRef | null;
  accessStatus?: CalendarAccessStatus | null;
  /** The translated [accessStatus]. */
  accessStatusString?: string | null;
  fullAccessUsers?: TeamCalRef[];
  fullAccessGroups?: TeamCalRef[];
  readonlyAccessUsers?: TeamCalRef[];
  readonlyAccessGroups?: TeamCalRef[];
  minimalAccessUsers?: TeamCalRef[];
  minimalAccessGroups?: TeamCalRef[];
  includeLeaveDaysForUsers?: TeamCalRef[];
  includeLeaveDaysForGroups?: TeamCalRef[];
  externalSubscription?: boolean;
  /** Only sent in edit mode to users who may update the calendar (it may hold credentials). */
  externalSubscriptionUrl?: string | null;
  /** In seconds. */
  externalSubscriptionUpdateInterval?: number | null;
  externalSubscriptionUrlAnonymized?: string | null;
  /** The feeds of an existing calendar, sent in edit mode only. */
  subscriptionInfo?: CalendarSubscriptionInfo | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface TeamCalListRow extends TeamCalDetail {
  id: number;
}
