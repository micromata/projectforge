import type { AvailabilityValues } from "./availability-schema";
import type { AvailabilityDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every value is
 * normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(
  availability: AvailabilityDetail
): AvailabilityValues {
  return {
    id: availability.id ?? null,
    user: availability.user ?? null,
    // Not the first day shown of an entry whose begin isn't told: no begin at all.
    startDate: availability.startHidden
      ? null
      : (availability.startDate ?? null),
    endDate: availability.endDate ?? null,
    type: availability.type ?? null,
    status: availability.status ?? null,
    location: availability.location ?? null,
    comment: availability.comment ?? null,
    internalComment: availability.internalComment ?? null,
    substitutes: availability.substitutes ?? [],
  };
}

/**
 * Blank form for an entry that doesn't exist yet. User and status are preset by the backend
 * (`AvailabilityEntityRest.newBaseDTO`), whose `/rs/availability/newEntry` answer the form is reset onto.
 */
export function emptyAvailabilityValues(): AvailabilityValues {
  return toFormValues({ id: null });
}
