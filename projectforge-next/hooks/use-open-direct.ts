"use client";

import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { openChatDirect } from "@/lib/rs/chat-direct";
import { toast } from "@/lib/toast";

/**
 * Opens the direct message with a user (created if missing) and goes there. Refused by the server for users the
 * logged-in one may not see, shown as it came.
 */
export function useOpenDirect() {
  const router = useRouter();
  return useMutation({
    mutationFn: (userId: number) => openChatDirect(userId),
    onSuccess: ({ conversationId }) => router.push(`/chat/${conversationId}`),
    onError: (error: Error) => toast.error(error.message),
  });
}
