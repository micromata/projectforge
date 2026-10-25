"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Attachment01Icon, SentIcon } from "@hugeicons/core-free-icons";
import { AttachmentDropZone } from "@/components/shared/attachments/attachment-drop-zone";
import { Button } from "@/components/ui/button";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import { sendChatMessage } from "@/lib/rs/chat";
import type { ChatConversation, ChatMessage } from "@/lib/rs/chat-types";
import { ChatEditor } from "./chat-editor";
import { PendingFiles } from "./pending-files";
import type { ChatEditorHandle } from "./types";
import { useChatDraft } from "./use-chat-draft";
import { usePendingFiles } from "./use-pending-files";

/**
 * Writing a message of the conversation or a reply in a thread: the editor, the files (paste, the file dialog or
 * dropped anywhere on the composer, sent with the message into the data transfer) and the send button. The draft
 * is kept until sent.
 */
export function ChatComposer({
  conversation,
  threadRootId,
  onSent,
  autoFocus,
}: {
  conversation: ChatConversation;
  threadRootId: number | null;
  onSent: (message: ChatMessage) => void;
  autoFocus?: boolean;
}) {
  const t = useTranslations();
  const editorRef = useRef<ChatEditorHandle>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const draft = useChatDraft(conversation.id, threadRootId, conversation.me.id);
  const [hasText, setHasText] = useState(!!draft.initial);
  const pending = usePendingFiles(conversation);
  const send = useMutation({
    mutationFn: (html: string) =>
      sendChatMessage(
        conversation.id,
        html,
        threadRootId,
        pending.files.map((it) => it.file)
      ),
    onSuccess: (message) => {
      editorRef.current?.clear();
      pending.clear();
      draft.save("");
      setHasText(false);
      onSent(message);
    },
    onError: (error: Error) => toast.error(error.message),
  });
  const submit = () => {
    const html = editorRef.current?.getHtml() ?? "";
    if (send.isPending || pending.tooLarge) return;
    if (!html && pending.files.length === 0) return;
    send.mutate(html);
  };
  const placeholder = threadRootId
    ? t("chat.thread.placeholder")
    : t("chat.message.placeholder");

  return (
    <AttachmentDropZone onFiles={pending.add}>
      <div className="shrink-0 border-t bg-background">
        <PendingFiles files={pending.files} onRemove={pending.remove} />
        {pending.files.length > 0 && (
          <p
            className={cn(
              "px-2 pt-1 text-xs",
              pending.tooLarge ? "text-destructive" : "text-muted-foreground"
            )}
          >
            {pending.tooLarge
              ? t("chat.file.error.tooLargeTogether")
              : conversation.directPartner &&
                t("chat.file.directTarget", {
                  arg0: conversation.directPartner.displayName,
                })}
          </p>
        )}
        <div className="flex items-end gap-1 p-2">
          {conversation.filesEnabled && (
            <>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label={t("chat.file.add")}
                onClick={() => fileInputRef.current?.click()}
              >
                <HugeiconsIcon icon={Attachment01Icon} />
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                className="sr-only"
                tabIndex={-1}
                aria-hidden
                onChange={(event) => {
                  pending.add(Array.from(event.target.files ?? []));
                  event.target.value = "";
                }}
              />
            </>
          )}
          <ChatEditor
            ref={editorRef}
            conversationId={conversation.id}
            initialHtml={draft.initial}
            placeholder={placeholder}
            ariaLabel={placeholder}
            autoFocus={autoFocus}
            onSubmit={submit}
            onFiles={pending.add}
            onChange={(html) => {
              setHasText(!!html);
              draft.save(html);
            }}
            className="rounded-md border border-input focus-within:border-ring"
          />
          <Button
            type="button"
            size="icon"
            aria-label={t("chat.message.send")}
            disabled={
              send.isPending ||
              pending.tooLarge ||
              (!hasText && pending.files.length === 0)
            }
            onClick={submit}
          >
            <HugeiconsIcon icon={SentIcon} />
          </Button>
        </div>
      </div>
    </AttachmentDropZone>
  );
}
