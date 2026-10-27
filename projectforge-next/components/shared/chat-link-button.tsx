"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Message01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { buttonVariants } from "@/components/ui/button";
import { useChatSecretOffers } from "@/hooks/use-chat-secret-offers";
import { useFeatureEnabled } from "@/hooks/use-feature-enabled";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import { CHAT_DIRECT_QUERY_KEY, fetchChatDirect } from "@/lib/rs/chat-direct";
import { CHAT_ROOMS_QUERY_KEY, fetchChatRooms } from "@/lib/rs/chat-rooms";
import { cn } from "@/lib/utils";

/**
 * The unread messages of the rooms and direct messages, plus the confidential conversations offered and not yet
 * answered (they wait for the user like an unread message). Shares the queries of the chat page, whose invalidations
 * (read state changed) refresh it; no polling, as every call touches the HTTP session — it refetches on focus.
 */
function useChatUnreadCount() {
  const options = {
    retry: false,
    staleTime: 60 * 1000,
    refetchOnWindowFocus: true,
  };
  const rooms = useQuery({
    queryKey: CHAT_ROOMS_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatRooms(signal),
    ...options,
  });
  const direct = useQuery({
    queryKey: CHAT_DIRECT_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatDirect(signal),
    ...options,
  });
  const sum = (entries?: { unread: number }[]) =>
    (entries ?? []).reduce((total, entry) => total + entry.unread, 0);
  const offers = useChatSecretOffers();
  return sum(rooms.data) + sum(direct.data) + offers.length;
}

/** The top bar's speech bubble, linking to the chat. */
export function ChatLinkButton() {
  const t = useTranslations();
  const label = t("chat.title");
  const unread = useChatUnreadCount();
  // Data exchange is only offered with the data transfer switched on.
  const hint = useFeatureEnabled("datatransfer")
    ? t("chat.linkHintDataTransfer")
    : t("chat.linkHint");
  return (
    <HintTooltip text={hint}>
      <Link
        href="/chat"
        className={cn(
          buttonVariants({ variant: "ghost", size: "sm" }),
          "relative shrink-0"
        )}
        aria-label={label}
        data-testid="chat-link"
      >
        <HugeiconsIcon icon={Message01Icon} size={18} />
        <MenuCounterBadge badge={{ counter: unread }} variant="corner" />
      </Link>
    </HintTooltip>
  );
}
