/**
 * The calls of the poll pages besides the standard CRUD of `lib/rs/entity.ts`: finishing a poll, its Excel
 * export and templates (`PollEntityRest`), the answers and results (`PollAnswerRest`).
 */

import { request } from "./client";
import { downloadFile } from "./download";
import type {
  PollAnswer,
  PollAnswerData,
  PollDetail,
  PollQuestion,
  PollResults,
  PollTemplateInfo,
} from "@/components/features/poll/types";

/** Finishes the poll before its deadline; everybody is mailed, the users with full access get the results. */
export function finishPoll(id: number): Promise<PollDetail> {
  return request<PollDetail>(`/rs/poll/finish/${id}`, { method: "POST" });
}

export function downloadPollExcel(id: number): Promise<void> {
  return downloadFile(`/rs/poll/exportAsExcel/${id}`);
}

export function fetchPollTemplates(
  signal?: AbortSignal
): Promise<PollTemplateInfo[]> {
  return request<PollTemplateInfo[]>(
    "/rs/poll/templates",
    { method: "GET" },
    signal
  );
}

/** The questions of a template, with new uids, in the language of the user. */
export function fetchPollTemplateQuestions(
  template: PollTemplateInfo
): Promise<PollQuestion[]> {
  const query =
    template.pollId != null
      ? `pollId=${template.pollId}`
      : `key=${encodeURIComponent(template.key ?? "")}`;
  return request<PollQuestion[]>(`/rs/poll/templates/questions?${query}`, {
    method: "GET",
  });
}

/** The answers of the logged-in user, or of the attendee [ownerId] a user with full access answers for. */
export function fetchPollAnswers(
  pollId: number,
  ownerId: number | null,
  signal?: AbortSignal
): Promise<PollAnswerData> {
  const query = ownerId != null ? `?owner=${ownerId}` : "";
  return request<PollAnswerData>(
    `/rs/pollAnswer/${pollId}${query}`,
    { method: "GET" },
    signal
  );
}

export function savePollAnswers(
  pollId: number,
  ownerId: number | null,
  answers: PollAnswer[]
): Promise<PollAnswerData> {
  return request<PollAnswerData>("/rs/pollAnswer/save", {
    method: "POST",
    body: JSON.stringify({ pollId, ownerId, answers }),
  });
}

export function fetchPollResults(
  pollId: number,
  signal?: AbortSignal
): Promise<PollResults> {
  return request<PollResults>(
    `/rs/pollAnswer/results/${pollId}`,
    { method: "GET" },
    signal
  );
}
