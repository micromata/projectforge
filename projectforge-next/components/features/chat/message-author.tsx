"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Message01Icon } from "@hugeicons/core-free-icons";
import { UserAvatar } from "@/components/shared/user-avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import type { ChatRef } from "@/lib/rs/chat-types";
import { useChatContext } from "./chat-context";
import { useOpenDirect } from "./use-open-direct";

/**
 * Avatar or name of the author of a message. For somebody else than the user (and the other one of the open direct
 * message), a click offers to send a direct message (DM-01), so writing privately is possible from every
 * conversation.
 */
export function MessageAuthor({
  author,
  avatar,
}: {
  author: ChatRef;
  /** The avatar, else the name. */
  avatar?: boolean;
}) {
  const t = useTranslations();
  const { conversation } = useChatContext();
  const openDirect = useOpenDirect();
  const content = avatar ? (
    <UserAvatar name={author.displayName} className="mt-0.5" />
  ) : (
    <span className="text-sm font-semibold">{author.displayName}</span>
  );
  if (
    author.id === conversation.me.id ||
    author.id === conversation.directPartner?.id
  ) {
    return content;
  }
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className="h-fit rounded-sm hover:underline focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
        aria-label={`${author.displayName}: ${t("chat.direct.send")}`}
        tabIndex={avatar ? -1 : undefined}
      >
        {content}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        <DropdownMenuItem
          disabled={openDirect.isPending}
          onSelect={() => openDirect.mutate(author.id)}
        >
          <HugeiconsIcon icon={Message01Icon} />
          {t("chat.direct.send")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
