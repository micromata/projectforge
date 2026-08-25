import { z } from "zod";
import { TEAM_EVENT_METADATA } from "@/lib/metadata/team-event.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { i18nMarker, REQUIRED } from "@/lib/validation/markers";

/**
 * Every rule of an edited field — mandatory, maximum length — comes from TeamEventDO through
 * `lib/metadata/team-event.generated.ts`. Nothing here restates one (see
 * lib/validation/from-metadata.ts).
 */
const m = fromMetadata(TEAM_EVENT_METADATA);

/**
 * A team calendar as the form holds it. Not from the metadata: `TeamEventDO.calendar` is a `TeamCalDO`
 * relation, which the generator does not carry as a field, so the shape is stated here. Only the id is
 * written back — Spring resolves the calendar object from it (`TeamEventPagesRest.onBeforeDatabaseAction`)
 * — but the option list sends a `title` and the select shows a `displayName`, and both are kept via the
 * loose object so a value handed straight back is not stripped.
 *
 * Required, though the metadata does not mark it so: an event cannot be saved into no calendar (the
 * access check refuses it), and the answer needs nothing the client doesn't have — so it is anticipated
 * here rather than left to an HTTP 406.
 */
const calendarField = z
  .looseObject({
    id: z.number(),
    displayName: z.string().optional(),
    title: z.string().nullish(),
  })
  .nullable()
  .refine((v): boolean => v != null, REQUIRED);

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.TeamEvent — hand-written, because the DTO
 * has neither the field set nor the names of the DO. What each edited field *allows* is not.
 *
 * The fields below `note` are carried through untouched (see types.ts): a hand-built form posts its
 * values *as* the DTO (EntityEditPage.save), so a field left out here would be dropped on save — silent
 * data loss for a recurring event, an event with attendees or a reminder, whose editing UI is a later
 * phase. Editing such an event still fails loudly rather than corrupting it: the server refuses a
 * recurring event without a `seriesModificationMode` (`TeamEventPagesRest.validate`), which no field on
 * this form yet sets.
 *
 * The server validates too and has the last word: it requires the subject and, for a recurring event,
 * the modification mode (HTTP 406 → see lib/validation/server-errors.ts).
 */
const teamEventEditObject = z.object({
  // null while the event is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  // Optional per the entity, but the server refuses an event without one
  // (`validation.error.fieldRequired` on `subject`): anticipated as required, since the box is on the
  // form and the answer is the client's to give.
  subject: m.requiredString("subject"),
  calendar: calendarField,
  location: m.nullableString("location"),
  note: m.nullableString("note"),
  // Both ends as the ISO instant the shared DateTimeInput consumes and produces (see lib/user-zone.ts).
  startDate: m.instantField("startDate"),
  endDate: m.instantField("endDate"),
  allDay: m.booleanField("allDay"),
  // --- carried through untouched (see the object doc above) ---
  recurrenceRule: z.string().nullable(),
  recurrenceExDate: z.string().nullable(),
  recurrenceReferenceDate: z.string().nullable(),
  recurrenceReferenceId: z.string().nullable(),
  recurrenceUntil: z.string().nullable(),
  attendees: z.array(z.unknown()).nullable(),
  reminderDuration: z.number().nullable(),
  reminderDurationUnit: z.string().nullable(),
  reminderActionType: z.string().nullable(),
  organizer: z.string().nullable(),
  organizerAdditionalParams: z.string().nullable(),
  ownership: z.boolean().nullable(),
  sequence: z.number().nullable(),
  uid: z.string().nullable(),
  dtStamp: z.string().nullable(),
  lastEmail: z.string().nullable(),
  attachments: z.array(z.unknown()).nullable(),
  created: z.string().nullable(),
  lastUpdate: z.string().nullable(),
});

export const teamEventEditSchema = teamEventEditObject
  // The one cross-field rule worth anticipating, because both ends are on the screen together and the
  // answer needs nothing the client doesn't have. Reported on the end, which is what the user fixes —
  // the same key the legacy period panel used (`timePeriodPanel.startTimeAfterStopTime`).
  .refine((v) => !v.startDate || !v.endDate || v.startDate <= v.endDate, {
    path: ["endDate"],
    message: i18nMarker("timePeriodPanel.startTimeAfterStopTime"),
  });

export type TeamEventEditValues = z.infer<typeof teamEventEditSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 *
 * Read off the pre-refine object: `teamEventEditSchema` is a `ZodPipe` after the `refine` above and has
 * no `shape` of its own.
 */
export const TEAM_EVENT_EDIT_FIELDS = Object.keys(
  teamEventEditObject.shape
) as readonly (keyof TeamEventEditValues)[];
