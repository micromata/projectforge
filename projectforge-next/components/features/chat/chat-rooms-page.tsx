"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, Message01Icon } from "@hugeicons/core-free-icons";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { CHAT_DIRECT_QUERY_KEY, fetchChatDirect } from "@/lib/rs/chat-direct";
import { CHAT_ROOMS_QUERY_KEY, fetchChatRooms } from "@/lib/rs/chat-rooms";
import { ChatDirectDialog } from "./chat-direct-dialog";
import { ChatDirectList } from "./chat-direct-list";
import { ChatRoomDialog } from "./chat-room-dialog";
import { ChatRoomList } from "./chat-room-list";

/**
 * The chat of the user (`/next/chat`): the direct messages (PF-CHAT-DM) and the chat rooms (PF-CHAT-ROOM, ROOM-06),
 * the ones with the newest messages first, with unread counts; starting a direct message, creating a room and
 * editing those the user owns or moderates. Each opens its conversation. Minimal on purpose: the chat area with
 * the conversations beside the open one follows (ROOM-08).
 */
export function ChatRoomsPage() {
  const t = useTranslations();
  const router = useRouter();
  const query = useQuery({
    queryKey: CHAT_ROOMS_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatRooms(signal),
  });
  const direct = useQuery({
    queryKey: CHAT_DIRECT_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatDirect(signal),
  });
  const [dialog, setDialog] = useState<{ roomId: number | null } | null>(null);
  const [directDialog, setDirectDialog] = useState(false);

  return (
    <PageShell>
      <PageTitleRow category={t("menu.common")} title={t("chat.title")}>
        <Button variant="outline" onClick={() => setDirectDialog(true)}>
          <HugeiconsIcon icon={Message01Icon} data-icon="inline-start" />
          {t("chat.direct.new")}
        </Button>
        <Button onClick={() => setDialog({ roomId: null })}>
          <HugeiconsIcon icon={Add01Icon} data-icon="inline-start" />
          {t("chat.room.new")}
        </Button>
      </PageTitleRow>
      <section
        className="flex flex-col gap-3 px-4 pt-2 pb-4"
        aria-labelledby="chatDirectHeading"
      >
        <h2 id="chatDirectHeading" className="text-sm font-semibold">
          {t("chat.direct._")}
        </h2>
        {direct.isError && (
          <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
        )}
        {direct.isPending && <Spinner className="h-5 w-5 border-2" />}
        {direct.data?.length === 0 && (
          <p className="text-sm text-muted-foreground">
            {t("chat.direct.empty")}
          </p>
        )}
        {direct.data && direct.data.length > 0 && (
          <ChatDirectList entries={direct.data} />
        )}
      </section>
      <section
        className="flex flex-col gap-3 px-4 pb-6"
        aria-labelledby="chatRoomsHeading"
      >
        <h2 id="chatRoomsHeading" className="text-sm font-semibold">
          {t("chat.rooms._")}
        </h2>
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
      </section>
      <ChatDirectDialog
        open={directDialog}
        onClose={() => setDirectDialog(false)}
      />
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
