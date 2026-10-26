"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Message01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useAuth } from "@/hooks/use-auth";
import { useOpenDirect } from "@/hooks/use-open-direct";
import {
  CHAT_DATA_TRANSFER_CHANNEL,
  CHAT_HOST_DATA_TRANSFER,
  openChatConversation,
} from "@/lib/rs/chat";
import type { DataTransferView } from "@/lib/rs/datatransfer";
import { toast } from "@/lib/toast";

/**
 * The chat of an area's page (PF-CHAT-DT): an ordinary area is a chat room of everybody with access to it, its files
 * go into the area; the personal box of another user opens the direct message with its owner, whose files go into
 * the box. The own box has none.
 */
export function DataTransferChatButton({ view }: { view: DataTransferView }) {
  const { user } = useAuth();
  const boxUserId = view.personalBoxUserId;
  if (!user) return null; // Not known yet: it might be the own box.
  if (boxUserId != null) {
    if (boxUserId === user?.userId) return null;
    return <DirectMessageButton userId={boxUserId} />;
  }
  if (!view.chatAvailable || view.area.id == null) return null;
  return <AreaChatButton areaId={view.area.id} />;
}

/** Highlighted in the yellow of the chat actions (as "Start chat" in the menu search). */
const CHAT_BUTTON_CLASSES =
  "gap-1.5 bg-brand-yellow-dark font-medium text-black hover:bg-brand-yellow-dark/85";

function AreaChatButton({ areaId }: { areaId: number }) {
  const t = useTranslations();
  const router = useRouter();
  const open = useMutation({
    mutationFn: () =>
      openChatConversation(
        CHAT_HOST_DATA_TRANSFER,
        areaId,
        CHAT_DATA_TRANSFER_CHANNEL
      ),
    onSuccess: ({ conversationId }) => {
      if (conversationId != null) router.push(`/chat/${conversationId}`);
      else toast.error(t("access.exception.noAccess"));
    },
    onError: (error: Error) => toast.error(error.message),
  });
  return (
    <HintTooltip text={t("plugins.datatransfer.button.chat.info")}>
      <Button
        type="button"
        size="sm"
        className={CHAT_BUTTON_CLASSES}
        disabled={open.isPending}
        onClick={() => open.mutate()}
      >
        <HugeiconsIcon icon={Message01Icon} size={14} aria-hidden />
        {t("plugins.datatransfer.button.chat._")}
      </Button>
    </HintTooltip>
  );
}

function DirectMessageButton({ userId }: { userId: number }) {
  const t = useTranslations();
  const openDirect = useOpenDirect();
  return (
    <HintTooltip text={t("plugins.datatransfer.button.directMessage.info")}>
      <Button
        type="button"
        size="sm"
        className={CHAT_BUTTON_CLASSES}
        disabled={openDirect.isPending}
        onClick={() => openDirect.mutate(userId)}
      >
        <HugeiconsIcon icon={Message01Icon} size={14} aria-hidden />
        {t("plugins.datatransfer.button.directMessage._")}
      </Button>
    </HintTooltip>
  );
}
