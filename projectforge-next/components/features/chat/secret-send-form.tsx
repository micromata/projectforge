"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { SECRET_MAX_LENGTH } from "./use-chat-secret";

/**
 * The text of a confidential message. Kept in this component only (not in the chat draft, not in the
 * browser's form history) and gone with the dialog.
 */
export function SecretSendForm({
  pending,
  onSend,
}: {
  pending: boolean;
  onSend: (text: string) => void;
}) {
  const t = useTranslations();
  const [text, setText] = useState("");
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor="chatSecretText">{t("chat.secret.text")}</Label>
      <Textarea
        id="chatSecretText"
        value={text}
        onChange={(event) => setText(event.target.value)}
        maxLength={SECRET_MAX_LENGTH}
        autoComplete="off"
        spellCheck={false}
        autoFocus
        className="min-h-24 font-mono"
      />
      <Button
        type="button"
        className="self-end"
        disabled={pending || !text.trim()}
        onClick={() => onSend(text)}
      >
        {t("chat.secret.send")}
      </Button>
    </div>
  );
}
