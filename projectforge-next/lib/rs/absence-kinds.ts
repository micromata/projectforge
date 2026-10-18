/**
 * What the shared view of absences shows (`org.projectforge.business.availability.AbsenceKind`): the vacations and
 * the availability entries by their status, whose constants have the same names (`AvailabilityStatus.kind`). Only
 * vacations and `NOT_AVAILABLE` are absences for the substitutes. Chosen in the calendar, for a team calendar and in
 * the team vacation calendar (`AbsenceService`).
 */
export const ABSENCE_KINDS = [
  "VACATION",
  "NOT_AVAILABLE",
  "REMOTE_REACHABLE",
  "REACHABLE_IF_NEEDED",
  "REACHABLE_IF_URGENT",
] as const;
export type AbsenceKind = (typeof ABSENCE_KINDS)[number];

/** The given kinds in their canonical order, each once; unknown values are dropped. */
export function sortAbsenceKinds(
  kinds: readonly string[] | null | undefined
): AbsenceKind[] {
  return ABSENCE_KINDS.filter((kind) => kinds?.includes(kind));
}

/** The kind of an availability entry by its status (`AvailabilityStatus.kind`), null for an unknown one. */
export function absenceKindOfStatus(
  status: string | null | undefined
): AbsenceKind | null {
  return (
    ABSENCE_KINDS.find((kind) => kind !== "VACATION" && kind === status) ?? null
  );
}
