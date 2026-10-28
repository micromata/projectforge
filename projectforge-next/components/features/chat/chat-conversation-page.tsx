"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowLeft01Icon,
  ArrowUp01Icon,
  Settings02Icon,
} from "@hugeicons/core-free-icons";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { RICH_TEXT_MUTED_CLASSES } from "@/components/shared/rich-text";
import { ChatDescription } from "./chat-description";
import { Button } from "@/components/ui/button";
import { Collapsible, CollapsibleTrigger } from "@/components/ui/collapsible";
import {
  CHAT_HOST_CHAT_ROOM,
  CHAT_HOST_DATA_TRANSFER,
  chatConversationQueryKey,
  fetchChatConversation,
} from "@/lib/rs/chat";
import { cn } from "@/lib/utils";
import { ChatMembersButton } from "./chat-members-button";
import { ChatDataTransferPill } from "./chat-host-pill";
import { ChatPanel } from "./chat-panel";
import { ChatRoomDialog } from "./chat-room-dialog";

/** The host type of the chat rooms (ChatRoomChatHost.HOST_TYPE). */

/**
 * A conversation as a page of its own (`/next/chat/{id}`, CTX-04): the title of its host object with the way
 * back to it, its description, the conversation full size. `?message={id}` points at a message (MSG-05),
 * `&thread={rootId}` opens its thread for a reply. The
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
  const thread = Number(searchParams.get("thread"));
  const query = useQuery({
    queryKey: chatConversationQueryKey(conversationId),
    queryFn: ({ signal }) => fetchChatConversation(conversationId, signal),
    refetchOnWindowFocus: false,
  });
  const [editRoom, setEditRoom] = useState(false);
  // The slot of the title row for search, muting and the like, which ChatPanel fills (a portal).
  const [headerSlot, setHeaderSlot] = useState<HTMLElement | null>(null);
  const [expanded, setExpanded] = useState(false);
  const conversation = query.data;
  const teaser = useMemo(
    () => teaserOf(conversation?.description),
    [conversation?.description]
  );
  const roomId =
    conversation?.hostType === CHAT_HOST_CHAT_ROOM && conversation.isModerator
      ? conversation.hostId
      : null;
  return (
    <PageShell>
      <PageTitleRow
        category={t("chat.title")}
        title={conversation?.title ?? t("chat.title")}
        titleAddon={
          <>
            {conversation?.hostType === CHAT_HOST_DATA_TRANSFER && (
              <ChatDataTransferPill link={conversation.link ?? null} />
            )}
          </>
        }
      >
        <div ref={setHeaderSlot} className="flex items-center gap-1" />
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
      {conversation?.description &&
        (teaser.multiline ? (
          <Collapsible
            open={expanded}
            onOpenChange={setExpanded}
            className="flex shrink-0 items-start gap-1 px-4 pt-1"
          >
            <div className="min-w-0 flex-1">
              {expanded ? (
                // Its own scrolling: a long description leaves the conversation the rest of the page.
                <ChatDescription
                  html={conversation.description}
                  className={cn(
                    "max-h-32 overflow-y-auto text-sm",
                    RICH_TEXT_MUTED_CLASSES
                  )}
                />
              ) : (
                // The ellipsis is its own element: it shows even if the line fits and there is more below.
                <p className="flex text-sm text-muted-foreground">
                  <span className="truncate">{teaser.first}</span>
                  <span className="shrink-0" aria-hidden>
                    &nbsp;…
                  </span>
                </p>
              )}
            </div>
            <CollapsibleTrigger asChild>
              <Button
                variant="ghost"
                size="icon-sm"
                aria-label={t(
                  expanded ? "chat.description.hide" : "chat.description.show"
                )}
              >
                <HugeiconsIcon
                  icon={expanded ? ArrowUp01Icon : ArrowDown01Icon}
                />
              </Button>
            </CollapsibleTrigger>
          </Collapsible>
        ) : (
          <ChatDescription
            html={conversation.description}
            className={cn(
              "shrink-0 px-4 pt-1 text-sm",
              RICH_TEXT_MUTED_CLASSES
            )}
          />
        ))}
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
          threadRootId={Number.isInteger(thread) && thread > 0 ? thread : null}
          headerSlot={headerSlot}
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

/** A first line longer than this is not shown whole in the teaser: the description is worth expanding. */
const TEASER_LENGTH = 200;

/**
 * The first line of an html description as text (parsed inert: nothing is loaded or run) and whether there is
 * more than that line to expand.
 */
function teaserOf(html: string | null | undefined): {
  first: string;
  multiline: boolean;
} {
  if (!html) return { first: "", multiline: false };
  const lines = (
    new DOMParser().parseFromString(
      html.replace(/<br\s*\/?>|<\/(?:p|div|li|h[1-6]|blockquote)>/gi, "$&\n"),
      "text/html"
    ).body.textContent ?? ""
  )
    .split("\n")
    .map((line) => line.replace(/\s+/g, " ").trim())
    .filter(Boolean);
  const first = lines[0] ?? "";
  return { first, multiline: lines.length > 1 || first.length > TEASER_LENGTH };
}
