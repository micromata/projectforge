"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, Settings02Icon } from "@hugeicons/core-free-icons";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import {
  RICH_TEXT_MUTED_CLASSES,
  RichText,
} from "@/components/shared/rich-text";
import { Button } from "@/components/ui/button";
import { chatConversationQueryKey, fetchChatConversation } from "@/lib/rs/chat";
import { cn } from "@/lib/utils";
import { ChatMembersButton } from "./chat-members-button";
import { ChatPanel } from "./chat-panel";
import { ChatRoomDialog } from "./chat-room-dialog";

/** The host type of the chat rooms (ChatRoomChatHost.HOST_TYPE). */
const CHAT_ROOM_HOST_TYPE = "chatRoom";

/**
 * A conversation as a page of its own (`/next/chat/{id}`, CTX-04): the title of its host object with the way
 * back to it, its description, the conversation full size. `?message={id}` points at a message (MSG-05). The
 * owner and the moderators of a chat room edit it from here (the server checks it again).
 */
export function ChatConversationPage({
  conversationId,
}: {
  conversationId: number;
}) {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const highlight = Number(searchParams.get("message"));
  const query = useQuery({
    queryKey: chatConversationQueryKey(conversationId),
    queryFn: ({ signal }) => fetchChatConversation(conversationId, signal),
    refetchOnWindowFocus: false,
  });
  const [editRoom, setEditRoom] = useState(false);
  const conversation = query.data;
  const roomId =
    conversation?.hostType === CHAT_ROOM_HOST_TYPE && conversation.isModerator
      ? conversation.hostId
      : null;
  return (
    <PageShell>
      <PageTitleRow
        category={t("chat.title")}
        title={conversation?.title ?? t("chat.title")}
      >
        <Button asChild variant="outline">
          <Link href={conversation?.link ?? "/chat"}>
            <HugeiconsIcon icon={ArrowLeft01Icon} data-icon="inline-start" />
            {t("chat.backToHost")}
          </Link>
        </Button>
        {conversation && (
          <ChatMembersButton
            conversationId={conversationId}
            meId={conversation.me.id}
          />
        )}
        {roomId != null && (
          <Button
            variant="ghost"
            size="icon"
            aria-label={t("chat.room.edit")}
            onClick={() => setEditRoom(true)}
          >
            <HugeiconsIcon icon={Settings02Icon} />
          </Button>
        )}
      </PageTitleRow>
      {conversation?.description && (
        // Its own scrolling: a long description leaves the conversation the rest of the page.
        <RichText
          html={conversation.description}
          className={cn(
            "max-h-32 shrink-0 overflow-y-auto px-4 pt-1 text-sm",
            RICH_TEXT_MUTED_CLASSES
          )}
        />
      )}
      {conversation?.notice && (
        // E.g. that the external users of a data transfer area don't see its chat.
        <div className="shrink-0 px-4 pt-2">
          <FormAlert tone="info">{conversation.notice}</FormAlert>
        </div>
      )}
      <div className="flex min-h-0 flex-1 flex-col">
        <ChatPanel
          conversationId={conversationId}
          highlightId={
            Number.isInteger(highlight) && highlight > 0 ? highlight : null
          }
        />
      </div>
      {roomId != null && (
        <ChatRoomDialog
          open={editRoom}
          roomId={roomId}
          onClose={() => setEditRoom(false)}
          onSaved={() => {
            setEditRoom(false);
            void queryClient.invalidateQueries({
              queryKey: chatConversationQueryKey(conversationId),
            });
          }}
          onDeleted={() => router.push("/chat")}
        />
      )}
    </PageShell>
  );
}
