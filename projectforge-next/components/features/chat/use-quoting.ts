"use client";

import { useCallback, useState } from "react";
import type { ChatMessage } from "@/lib/rs/chat-types";
import type { ChatContextValue } from "./chat-context";

/** The messages being quoted in the composer of the conversation and of the open thread (MSG-08). */
export function useQuoting() {
  const [quoting, setQuotingState] = useState<ChatContextValue["quoting"]>({
    main: null,
    thread: null,
  });
  const setQuoting = useCallback(
    (inThread: boolean, message: ChatMessage | null) =>
      setQuotingState((it) =>
        inThread ? { ...it, thread: message } : { ...it, main: message }
      ),
    []
  );
  return { quoting, setQuoting };
}
