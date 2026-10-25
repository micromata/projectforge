"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon } from "@hugeicons/core-free-icons";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { CHAT_ROOMS_QUERY_KEY, fetchChatRooms } from "@/lib/rs/chat-rooms";
import { ChatRoomDialog } from "./chat-room-dialog";
import { ChatRoomList } from "./chat-room-list";

/**
 * The chat rooms of the user (`/next/chat`, PF-CHAT-ROOM, ROOM-06): the ones with the newest messages first,
 * with unread counts; creating one and editing those the user owns or moderates. A room opens its conversation.
 * Minimal on purpose: the chat area with the rooms beside the conversation follows (ROOM-08).
 */
export function ChatRoomsPage() {
  const t = useTranslations();
  const router = useRouter();
  const query = useQuery({
    queryKey: CHAT_ROOMS_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatRooms(signal),
  });
  const [dialog, setDialog] = useState<{ roomId: number | null } | null>(null);

  return (
    <PageShell>
      <PageTitleRow category={t("menu.common")} title={t("chat.rooms._")}>
        <Button onClick={() => setDialog({ roomId: null })}>
          <HugeiconsIcon icon={Add01Icon} data-icon="inline-start" />
          {t("chat.room.new")}
        </Button>
      </PageTitleRow>
      <div className="flex flex-col gap-3 px-4 pt-2 pb-6">
        {query.isError && (
          <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
        )}
        {query.isPending && <Spinner className="h-5 w-5 border-2" />}
        {query.data?.length === 0 && (
          <p className="text-sm text-muted-foreground">
            {t("chat.rooms.empty")}
          </p>
        )}
        {query.data && query.data.length > 0 && (
          <ChatRoomList
            rooms={query.data}
            onEdit={(roomId) => setDialog({ roomId })}
          />
        )}
      </div>
      <ChatRoomDialog
        open={dialog != null}
        roomId={dialog?.roomId ?? null}
        onClose={() => setDialog(null)}
        onSaved={(conversationId) => {
          setDialog(null);
          if (conversationId) router.push(`/chat/${conversationId}`);
        }}
      />
    </PageShell>
  );
}
