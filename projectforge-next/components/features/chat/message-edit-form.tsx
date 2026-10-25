"use client";

import { useRef } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { useChatContext } from "./chat-context";
import { ChatEditor } from "./chat-editor";
import type { ChatEditorHandle } from "./types";
import { useMessageMutations } from "./use-message-mutations";

/** Editing an own message in place (MSG-03); its images stay. Enter saves, Escape is the cancel button. */
export function MessageEditForm({
  message,
  onDone,
}: {
  message: ChatMessage;
  onDone: () => void;
}) {
  const t = useTranslations();
  const { conversation } = useChatContext();
  const { edit } = useMessageMutations();
  const editorRef = useRef<ChatEditorHandle>(null);
  const save = () => {
    const html = editorRef.current?.getHtml() ?? "";
    edit.mutate(
      { id: message.id, html, imageIds: message.imageIds },
      { onSuccess: onDone }
    );
  };
  return (
    <div className="mt-1 flex flex-col gap-1.5">
      <ChatEditor
        ref={editorRef}
        conversationId={conversation.id}
        initialHtml={message.content ?? ""}
        placeholder={t("chat.message.placeholder")}
        ariaLabel={t("chat.message.edit")}
        onSubmit={save}
        autoFocus
        className="rounded-md border border-input bg-background"
      />
      <div className="flex justify-end gap-1.5">
        <Button variant="outline" size="sm" onClick={onDone}>
          {t("chat.message.cancel")}
        </Button>
        <Button size="sm" disabled={edit.isPending} onClick={save}>
          {t("chat.message.save")}
        </Button>
      </div>
    </div>
  );
}
