import type { PollValues } from "./poll-schema";
import type { PollDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every value is
 * normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(poll: PollDetail): PollValues {
  return {
    id: poll.id ?? null,
    title: poll.title ?? "",
    description: poll.description ?? null,
    location: poll.location ?? null,
    owner: poll.owner ?? null,
    deadline: poll.deadline ?? null,
    state: poll.state ?? "RUNNING",
    template: poll.template ?? false,
    questions: (poll.questions ?? []).map((question) => ({
      uid: question.uid ?? null,
      text: question.text ?? "",
      type: question.type ?? "TEXT",
      options: (question.options ?? []).map((option) => ({
        uid: option.uid ?? null,
        text: option.text ?? "",
      })),
    })),
    attendeeUsers: poll.attendeeUsers ?? [],
    attendeeGroups: poll.attendeeGroups ?? [],
    fullAccessUsers: poll.fullAccessUsers ?? [],
    fullAccessGroups: poll.fullAccessGroups ?? [],
    invitationSubject: poll.invitationSubject ?? null,
    invitationText: poll.invitationText ?? null,
  };
}

/**
 * Blank form for a poll that doesn't exist yet. The backend presets the logged-in user as owner and a deadline
 * (`PollEntityRest.newBaseDTO`), and its `/rs/poll/newEntry` answer is what the form is reset onto.
 */
export function emptyPollValues(): PollValues {
  return toFormValues({ id: null });
}
