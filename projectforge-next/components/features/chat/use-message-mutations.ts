"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import {
  deleteChatMessage,
  editChatMessage,
  followChatThread,
  hideChatMessage,
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
    mutationFn: ({
      id,
      html,
      imageIds,
    }: {
      id: number;
      html: string;
      imageIds: string[];
    }) => editChatMessage(id, html, imageIds),
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
  const follow = useMutation({
    mutationFn: ({ id, value }: { id: number; value: boolean }) =>
      followChatThread(id, value),
    ...options,
  });
  return { react, edit, remove, hide, follow };
}
