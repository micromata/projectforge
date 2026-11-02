"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import {
  deleteChatMessage,
  editChatMessage,
  followChatThread,
  hideChatMessage,
  reportChatMessage,
  toggleChatReaction,
} from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { applyChatChanges } from "./chat-cache";
import { useChatContext } from "./chat-context";

/**
 * The actions on a message. Every one is followed by fetching the changes of the conversation, which brings
 * the changed message (and its thread root) into the cache; an error is shown as it came from the server.
 */
export function useMessageMutations() {
  const t = useTranslations();
  const { conversation, sync } = useChatContext();
  const queryClient = useQueryClient();
  const options = {
    onSuccess: () => sync(),
    onError: (error: Error) => toast.error(error.message),
  };
  const react = useMutation({
    mutationFn: ({ id, emoji }: { id: number; emoji: string }) =>
      toggleChatReaction(id, emoji),
    ...options,
  });
  const edit = useMutation({
    mutationFn: ({ id, html }: { id: number; html: string }) =>
      editChatMessage(id, html),
    onSuccess: (message: ChatMessage) => {
      applyChatChanges(queryClient, conversation.id, [message]);
      return sync();
    },
    onError: options.onError,
  });
  const remove = useMutation({
    mutationFn: (id: number) => deleteChatMessage(id),
    ...options,
  });
  const hide = useMutation({
    mutationFn: ({
      id,
      hidden,
      reason,
    }: {
      id: number;
      hidden: boolean;
      reason: string | null;
    }) => hideChatMessage(id, hidden, reason),
    ...options,
  });
  const report = useMutation({
    mutationFn: ({
      id,
      reason,
      block,
    }: {
      id: number;
      reason: string | null;
      block: boolean;
    }) => reportChatMessage(id, reason, block),
    onSuccess: (_result, { block }) => {
      toast.success(t("chat.message.report.done"));
      // Blocking the author changes what the conversation shows.
      return block
        ? queryClient.invalidateQueries({
            predicate: (q) => String(q.queryKey[0]).startsWith("chat"),
          })
        : sync();
    },
    onError: options.onError,
  });
  const follow = useMutation({
    mutationFn: ({ id, value }: { id: number; value: boolean }) =>
      followChatThread(id, value),
    ...options,
  });
  return { react, edit, remove, hide, report, follow };
}
