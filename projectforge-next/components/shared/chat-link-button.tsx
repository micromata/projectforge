"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Message01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { buttonVariants } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { useChatSecretOffers } from "@/hooks/use-chat-secret-offers";
import { useFeatureEnabled } from "@/hooks/use-feature-enabled";
import { MenuCounterBadge } from "@/components/shared/menu-counter-badge";
import { cn } from "@/lib/utils";

/**
 * The unread messages of the rooms and direct messages (they arrive with `userStatus`, whose invalidations by the
 * chat pages refresh them), plus the confidential conversations offered and not yet answered (they wait for the user
 * like an unread message). No polling, as every call touches the HTTP session; both refetch on focus.
 */
function useChatUnreadCount() {
  const { chatUnread } = useAuth();
  const offers = useChatSecretOffers();
  return chatUnread + offers.length;
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
