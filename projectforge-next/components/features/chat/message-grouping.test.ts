import { describe, expect, it } from "vitest";
import type { FormatContext } from "@/lib/format";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { layoutMessages } from "./message-grouping";

const berlin: FormatContext = { locale: "de-DE", timeZone: "Europe/Berlin" };

let nextId = 1;
function message(
  authorId: number,
  created: string,
  extra: Partial<ChatMessage> = {}
): ChatMessage {
  return {
    id: nextId++,
    author: { id: authorId, displayName: `user ${authorId}` },
    created,
    deleted: false,
    ...extra,
  } as ChatMessage;
}

const continued = (messages: ChatMessage[], breakBeforeId?: number) =>
  layoutMessages(messages, berlin, breakBeforeId).map((it) => it.continued);

describe("layoutMessages", () => {
  it("continues the same author within five minutes only", () => {
    const messages = [
      message(1, "2026-10-20T10:00:00Z"),
      message(1, "2026-10-20T10:05:00Z"),
      message(1, "2026-10-20T10:10:01Z"),
      message(2, "2026-10-20T10:11:00Z"),
      message(1, "2026-10-20T10:12:00Z"),
    ];
    expect(continued(messages)).toEqual([false, true, false, false, false]);
  });

  it("starts a day at midnight in the user's zone", () => {
    // 21:59 and 22:01 UTC are 23:59 and 00:01 in Berlin (summer time).
    const messages = [
      message(1, "2026-08-20T21:59:00Z"),
      message(1, "2026-08-20T22:01:00Z"),
    ];
    const layout = layoutMessages(messages, berlin);
    expect(layout.map((it) => it.dayStart)).toEqual([
      "2026-08-20T21:59:00Z",
      "2026-08-20T22:01:00Z",
    ]);
    expect(layout[1].continued).toBe(false);
  });

  it("breaks at the line of the first unread message and after a deleted one", () => {
    const first = message(1, "2026-10-20T10:00:00Z");
    const unread = message(1, "2026-10-20T10:01:00Z");
    const deleted = message(1, "2026-10-20T10:02:00Z", { deleted: true });
    const after = message(1, "2026-10-20T10:03:00Z");
    expect(continued([first, unread, deleted, after], unread.id)).toEqual([
      false,
      false,
      true,
      false,
    ]);
  });
});
