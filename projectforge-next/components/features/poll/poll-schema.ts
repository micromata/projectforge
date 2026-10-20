import { z } from "zod";
import { POLL_METADATA } from "@/lib/metadata/poll.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from PollDO through `lib/metadata/poll.generated.ts`. Which fields the form
 * has mirrors org.projectforge.rest.poll.Poll.
 *
 * Every property of the poll is part of it: a save posts these values *as* the DTO, so a field left out here
 * would reach `Poll.copyTo` as null and overwrite the stored one. The rules depending on more than one field
 * (attendees and deadline, not for a template) are checked by the backend (`PollEntityRest.validate`).
 */
const m = fromMetadata(POLL_METADATA);

const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

const option = z.object({
  uid: z.string().nullable().optional(),
  text: z.string().nullable().optional(),
});

const question = z.object({
  uid: z.string().nullable().optional(),
  text: z.string().nullable().optional(),
  type: z.enum(["TEXT", "SINGLE_CHOICE", "MULTIPLE_CHOICE"]),
  options: z.array(option),
});

export const pollSchema = z.object({
  // null while the poll is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  title: m.requiredString("title"),
  description: m.nullableString("description"),
  location: m.nullableString("location"),
  owner: m.entityField("owner"),
  deadline: z.string().nullable(),
  state: m.enumField("state"),
  template: m.booleanField("template"),
  // No metadata: json of the DO.
  questions: z.array(question),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  attendeeUsers: z.array(ref),
  attendeeGroups: z.array(ref),
  fullAccessUsers: z.array(ref),
  fullAccessGroups: z.array(ref),
  // Only for a new poll, not stored: the invitation mail.
  invitationSubject: z.string().nullable(),
  invitationText: z.string().nullable(),
});

export type PollValues = z.infer<typeof pollSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const POLL_FIELDS = Object.keys(
  pollSchema.shape
) as readonly (keyof PollValues)[];
