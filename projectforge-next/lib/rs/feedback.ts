/**
 * The "Send feedback" page (`org.projectforge.rest.FeedbackPageRest`), successor of Wicket's
 * `wa/feedback`. A non-entity, standalone page, so it has its own small client here rather than going
 * through `fetchList` / the entity plumbing (mirrors `lib/rs/send-text-message.ts`).
 */

import { request } from "./client";
import type {
  FeedbackInitialData,
  FeedbackRequest,
  FeedbackResult,
} from "@/components/features/feedback/types";

/** Initial form data: the prefilled (read-only) receiver and sender. */
export function fetchFeedbackData(
  signal?: AbortSignal
): Promise<FeedbackInitialData> {
  return request<FeedbackInitialData>(
    "/rs/feedback",
    { method: "GET" },
    signal
  );
}

/** Sends the feedback; the result carries a localized success or error text for the toast. */
export function sendFeedback(
  body: FeedbackRequest,
  signal?: AbortSignal
): Promise<FeedbackResult> {
  return request<FeedbackResult>(
    "/rs/feedback/send",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}
