"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Image01Icon, SentIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { toast } from "@/lib/toast";
import { sendChatMessage } from "@/lib/rs/chat";
import type { ChatConversation, ChatMessage } from "@/lib/rs/chat-types";
import { ChatEditor } from "./chat-editor";
import { PendingImages } from "./pending-images";
import type { ChatEditorHandle } from "./types";
import { useChatDraft } from "./use-chat-draft";
import { useImageUploads } from "./use-image-uploads";

/**
 * Writing a message of the conversation or a reply in a thread: the editor, the images (paste, drop or the file
 * dialog) and the send button. The draft is kept until sent.
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
  const uploads = useImageUploads(
    conversation.id,
    conversation.maxImagesPerMessage,
    conversation.maxImageSize
  );
  const send = useMutation({
    mutationFn: (html: string) =>
      sendChatMessage(conversation.id, html, threadRootId, uploads.fileIds),
    onSuccess: (message) => {
      editorRef.current?.clear();
      uploads.clear();
      draft.save("");
      setHasText(false);
      onSent(message);
    },
    onError: (error: Error) => toast.error(error.message),
  });
  const submit = () => {
    const html = editorRef.current?.getHtml() ?? "";
    if (send.isPending || uploads.uploading) return;
    if (!html && uploads.fileIds.length === 0) return;
    send.mutate(html);
  };
  const placeholder = threadRootId
    ? t("chat.thread.placeholder")
    : t("chat.message.placeholder");

  return (
    <div className="shrink-0 border-t bg-background">
      <PendingImages images={uploads.images} onRemove={uploads.remove} />
      <div className="flex items-end gap-1 p-2">
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t("chat.image.add")}
          onClick={() => fileInputRef.current?.click()}
        >
          <HugeiconsIcon icon={Image01Icon} />
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/png,image/jpeg,image/gif,image/webp"
          multiple
          className="sr-only"
          tabIndex={-1}
          aria-hidden
          onChange={(event) => {
            uploads.add(Array.from(event.target.files ?? []));
            event.target.value = "";
          }}
        />
        <ChatEditor
          ref={editorRef}
          conversationId={conversation.id}
          initialHtml={draft.initial}
          placeholder={placeholder}
          ariaLabel={placeholder}
          autoFocus={autoFocus}
          onSubmit={submit}
          onImages={uploads.add}
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
            uploads.uploading ||
            (!hasText && uploads.fileIds.length === 0)
          }
          onClick={submit}
        >
          <HugeiconsIcon icon={SentIcon} />
        </Button>
      </div>
    </div>
  );
}
