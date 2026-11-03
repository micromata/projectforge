"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import {
  blockChatUser,
  chatBlockedQueryKey,
  fetchBlockedChatUsers,
  unblockChatUser,
} from "@/lib/rs/chat";

/**
 * The users the logged-in user has blocked (DM-07) and blocking or unblocking one. A change reloads everything of the
 * chat that depends on it: the conversations (can the user write, notice), the messages (folded ones), the lists.
 */
export function useBlockedUsers() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: chatBlockedQueryKey,
    queryFn: ({ signal }) => fetchBlockedChatUsers(signal),
    staleTime: 60_000,
  });
  const blocked = query.data ?? [];
  const refresh = () =>
    queryClient.invalidateQueries({
      predicate: (q) => String(q.queryKey[0]).startsWith("chat"),
    });
  const block = useMutation({
    mutationFn: (userId: number) => blockChatUser(userId),
    onSuccess: () => {
      toast.success(t("chat.block.blocked"));
      return refresh();
    },
    onError: (error: Error) => toast.error(error.message),
  });
  const unblock = useMutation({
    mutationFn: (userId: number) => unblockChatUser(userId),
    onSuccess: () => {
      toast.success(t("chat.block.unblocked"));
      return refresh();
    },
    onError: (error: Error) => toast.error(error.message),
  });
  return {
    blocked,
    isBlocked: (userId: number) => blocked.some((user) => user.id === userId),
    block,
    unblock,
  };
}
