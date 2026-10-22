/** A user or group as the DTO carries it: the id is what `copyTo` stores. */
// A type alias, not an interface: only an alias is assignable to the schema's loose object.
export type PollRef = {
  id: number;
  displayName?: string;
};

/** org.projectforge.business.poll.PollQuestionType. */
export type PollQuestionType = "TEXT" | "SINGLE_CHOICE" | "MULTIPLE_CHOICE";

/** org.projectforge.business.poll.PollOption: an answer names it by its uid. */
export type PollOption = {
  uid?: string | null;
  text?: string | null;
};

/** org.projectforge.business.poll.PollQuestion. */
export type PollQuestion = {
  uid?: string | null;
  text?: string | null;
  type: PollQuestionType;
  options: PollOption[];
};

/** org.projectforge.business.poll.PollAnswer. */
export type PollAnswer = {
  questionUid: string;
  text?: string | null;
  optionUids: string[];
};

export type PollState = "RUNNING" | "FINISHED" | "TEMPLATE";

/** org.projectforge.rest.poll.Poll. */
export interface PollDetail {
  id: number | null;
  title?: string;
  description?: string | null;
  location?: string | null;
  owner?: PollRef | null;
  deadline?: string | null;
  state?: PollState;
  template?: boolean;
  questions?: PollQuestion[];
  attendeeUsers?: PollRef[];
  attendeeGroups?: PollRef[];
  fullAccessUsers?: PollRef[];
  fullAccessGroups?: PollRef[];
  invitationSubject?: string | null;
  invitationText?: string | null;
  fullAccess?: boolean;
  attendee?: boolean;
  writeAccess?: boolean;
  deleteAccess?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface PollListRow extends PollDetail {
  id: number;
}

/** PollEntityRest.TemplateInfo: a built-in template (key) or a poll marked as template (pollId). */
export interface PollTemplateInfo {
  title?: string;
  key?: string;
  pollId?: number;
}

/** PollAnswerRest.AnswerData. */
export interface PollAnswerData {
  pollId: number;
  title?: string;
  description?: string | null;
  location?: string | null;
  owner?: PollRef | null;
  deadline?: string | null;
  state: PollState;
  open: boolean;
  questions: PollQuestion[];
  answerOwner: PollRef;
  answers: PollAnswer[];
  lastUpdate?: string | null;
  fullAccess: boolean;
  attendees?: PollRef[] | null;
}

/** PollResultsService.Results. */
export interface PollResults {
  pollId: number;
  title?: string;
  questions: PollQuestion[];
  rows: {
    user: PollRef;
    attendee: boolean;
    lastUpdate?: string | null;
    answers: PollAnswer[];
  }[];
}

/** Rows of the field array: a question or an option has no id of its own, only a uid (or none yet). */
export type QuestionRow = PollQuestion & { id?: null; deleted?: boolean };
export type OptionRow = PollOption & { id?: null; deleted?: boolean };
