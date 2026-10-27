import { dayKeyFormatter, type FormatContext } from "@/lib/format";
import type { ChatMessage } from "@/lib/rs/chat-types";

/** The gap up to which a message of the same author continues the one before. */
export const GROUP_GAP_MS = 5 * 60 * 1000;

export interface MessageLayout {
  message: ChatMessage;
  /** The first message of its day (in the user's zone): the timestamp to head the day with, else null. */
  dayStart: string | null;
  /** A follower of the message before: same author, same day, within [GROUP_GAP_MS]; shown without its header. */
  continued: boolean;
}

/**
 * The messages (oldest first) with where a day starts and which ones continue the message before, as Google Chat
 * groups them. A group also ends at a deleted message and before [breakBeforeId] (the line of the first unread one).
 */
export function layoutMessages(
  messages: ChatMessage[],
  ctx: FormatContext,
  breakBeforeId?: number
): MessageLayout[] {
  const dayKeyOf = dayKeyFormatter(ctx);
  let previous: ChatMessage | null = null;
  let previousDay = "";
  return messages.map((message) => {
    const day = dayKeyOf(message.created);
    const dayStart = day !== previousDay ? message.created : null;
    const continued =
      previous != null &&
      dayStart == null &&
      message.id !== breakBeforeId &&
      !previous.deleted &&
      previous.author.id === message.author.id &&
      gap(previous, message) <= GROUP_GAP_MS;
    previous = message;
    previousDay = day;
    return { message, dayStart, continued };
  });
}

function gap(before: ChatMessage, after: ChatMessage): number {
  if (!before.created || !after.created) return Infinity;
  return Date.parse(after.created) - Date.parse(before.created);
}
