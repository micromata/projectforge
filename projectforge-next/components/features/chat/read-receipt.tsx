"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { UserAvatar } from "@/components/shared/user-avatar";
import { useChatContext } from "./chat-context";

/** Under the newest own message the other one of a direct message has read: its small avatar and "Read". */
export function ReadReceipt() {
  const t = useTranslations();
  const partner = useChatContext().conversation.directPartner;
  if (!partner) return null;
  return (
    <HintTooltip text={t("chat.message.seenBy", { arg0: partner.displayName })}>
      <span className="flex items-center gap-1 text-[10px] text-muted-foreground">
        <UserAvatar
          name={partner.displayName}
          flags={partner}
          userId={partner.id}
          avatarVersion={partner.avatarVersion}
          className="size-4 border text-[7px]"
        />
        {t("chat.message.seen")}
      </span>
    </HintTooltip>
  );
}
