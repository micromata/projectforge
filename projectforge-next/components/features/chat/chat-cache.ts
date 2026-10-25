import type { InfiniteData, QueryClient } from "@tanstack/react-query";
import type {
  ChatMessage,
  ChatMessagesPage,
  ChatThread,
} from "@/lib/rs/chat-types";

export function chatMessagesQueryKey(conversationId: number) {
  return ["chatMessages", conversationId] as const;
}

export function chatThreadQueryKey(conversationId: number, rootId: number) {
  return ["chatThread", conversationId, rootId] as const;
}

type MessagePages = InfiniteData<ChatMessagesPage, number | null>;

/** The page list with the message replaced, or a new one put on top; one older than all loaded is left out. */
function upsertTopLevel(
  data: MessagePages,
  message: ChatMessage
): MessagePages {
  let found = false;
  const pages = data.pages.map((page) => {
    if (!page.messages.some((it) => it.id === message.id)) return page;
    found = true;
    return {
      ...page,
      messages: page.messages.map((it) =>
        it.id === message.id ? message : it
      ),
    };
  });
  if (found) return { ...data, pages };
  const first = pages[0];
  const newest = first?.messages[0]?.id ?? 0;
  const lastPage = pages[pages.length - 1];
  const oldest = lastPage?.messages[lastPage.messages.length - 1]?.id;
  // Newer than everything loaded: the newest page gets it. Older than the loaded ones (not loaded yet, or
  // loaded later with its page): nothing to do — unless all are loaded, then it belongs in between.
  if (first && (message.id > newest || first.messages.length === 0)) {
    pages[0] = { ...first, messages: [message, ...first.messages] };
  } else if (
    lastPage &&
    !lastPage.hasMore &&
    oldest != null &&
    message.id > oldest
  ) {
    const index = pages.findIndex((page) =>
      page.messages.some((it) => it.id < message.id)
    );
    const page = pages[index];
    pages[index] = {
      ...page,
      messages: [...page.messages, message].sort((a, b) => b.id - a.id),
    };
  }
  return { ...data, pages };
}

function upsertReply(thread: ChatThread, message: ChatMessage): ChatThread {
  const replies = thread.replies.some((it) => it.id === message.id)
    ? thread.replies.map((it) => (it.id === message.id ? message : it))
    : [...thread.replies, message].sort((a, b) => a.id - b.id);
  return { ...thread, replies };
}

/**
 * Merges changed messages (live updates, own actions) into the cached pages of the conversation and the
 * cached threads: a changed message replaces its old version, a new one is added where it belongs.
 */
export function applyChatChanges(
  queryClient: QueryClient,
  conversationId: number,
  messages: ChatMessage[]
) {
  if (messages.length === 0) return;
  queryClient.setQueryData<MessagePages>(
    chatMessagesQueryKey(conversationId),
    (data) =>
      data
        ? messages
            .filter((it) => it.threadRootId == null)
            .reduce((acc, message) => upsertTopLevel(acc, message), data)
        : data
  );
  messages.forEach((message) => {
    const rootId = message.threadRootId ?? message.id;
    queryClient.setQueryData<ChatThread>(
      chatThreadQueryKey(conversationId, rootId),
      (thread) => {
        if (!thread) return thread;
        return message.threadRootId == null
          ? { ...thread, root: message }
          : upsertReply(thread, message);
      }
    );
  });
}
