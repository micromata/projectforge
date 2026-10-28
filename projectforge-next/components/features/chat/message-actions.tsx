"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Comment01Icon,
  MoreHorizontalIcon,
  QuoteDownIcon,
} from "@hugeicons/core-free-icons";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { BASE_PATH } from "@/lib/config";
import { toast } from "@/lib/toast";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { useChatContext } from "./chat-context";
import { EmojiPicker } from "@/components/shared/emoji/emoji-picker";
import { HideMessageDialog } from "./hide-message-dialog";
import { MessageHistoryDialog } from "./message-history-dialog";
import { useMessageMutations } from "./use-message-mutations";

/**
 * The actions of a message, shown on hover (always on touch devices): react, quote, reply in the thread, and in the
 * menu edit and delete (own messages), copy the link, mark unread (messages of others), hide and history
 * (moderators).
 */
export function MessageActions({
  message,
  inThread,
  onEdit,
}: {
  message: ChatMessage;
  inThread: boolean;
  onEdit: () => void;
}) {
  const t = useTranslations();
  const { conversation, openThread, markUnread, setQuoting } = useChatContext();
  const mutations = useMessageMutations();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [hiding, setHiding] = useState(false);
  const [history, setHistory] = useState(false);
  const writable = conversation.canWrite && !message.deleted && !message.hidden;
  const copyLink = () => {
    const url = `${window.location.origin}${BASE_PATH}/chat/${conversation.id}?message=${message.id}`;
    void navigator.clipboard
      .writeText(url)
      .then(() => toast.success(t("chat.message.linkCopied")));
  };
  if (message.deleted && !conversation.isModerator) return null;

  return (
    <div className="flex items-center gap-0.5 rounded-md border bg-background shadow-xs">
      {writable && (
        <EmojiPicker
          ariaLabel={t("chat.message.react")}
          onPick={(emoji) => mutations.react.mutate({ id: message.id, emoji })}
        />
      )}
      {writable && (
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={t("chat.message.quote._")}
          onClick={() => setQuoting(inThread, message)}
        >
          <HugeiconsIcon icon={QuoteDownIcon} />
        </Button>
      )}
      {!inThread && writable && (
        <Button
          variant="ghost"
          size="icon-sm"
          aria-label={t("chat.message.reply")}
          onClick={() => openThread(message.id)}
        >
          <HugeiconsIcon icon={Comment01Icon} />
        </Button>
      )}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={`${t("chat.message.edit")} …`}
          >
            <HugeiconsIcon icon={MoreHorizontalIcon} />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {message.own && writable && (
            <DropdownMenuItem onSelect={onEdit}>
              {t("chat.message.edit")}
            </DropdownMenuItem>
          )}
          {message.own && !message.deleted && conversation.canWrite && (
            <DropdownMenuItem
              variant="destructive"
              onSelect={() => setConfirmDelete(true)}
            >
              {t("chat.message.delete._")}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onSelect={copyLink}>
            {t("chat.message.copyLink")}
          </DropdownMenuItem>
          {!message.own && !message.deleted && (
            <DropdownMenuItem onSelect={() => markUnread(message)}>
              {t("chat.message.markUnread")}
            </DropdownMenuItem>
          )}
          {conversation.isModerator && !message.deleted && (
            <DropdownMenuItem
              onSelect={() =>
                message.hidden
                  ? mutations.hide.mutate({
                      id: message.id,
                      hidden: false,
                      reason: null,
                    })
                  : setHiding(true)
              }
            >
              {message.hidden
                ? t("chat.message.unhide")
                : t("chat.message.hide._")}
            </DropdownMenuItem>
          )}
          {conversation.isModerator && (
            <DropdownMenuItem onSelect={() => setHistory(true)}>
              {t("chat.message.history")}
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title={t("chat.message.delete._")}
        description={t("chat.message.delete.confirm")}
        confirmLabel={t("chat.message.delete._")}
        destructive
        onConfirm={() => mutations.remove.mutate(message.id)}
      />
      <HideMessageDialog
        open={hiding}
        onOpenChange={setHiding}
        onConfirm={(reason) => {
          setHiding(false);
          mutations.hide.mutate({ id: message.id, hidden: true, reason });
        }}
      />
      <MessageHistoryDialog
        messageId={history ? message.id : null}
        onClose={() => setHistory(false)}
      />
    </div>
  );
}
