"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { SquareLock02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import type { ChatConversation } from "@/lib/rs/chat-types";
import { SecretSendSteps } from "./secret-send-dialog";

/** Opens the dialog sending a confidential message, in direct chats only. */
export function SecretSendButton({
  conversation,
}: {
  conversation: ChatConversation;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  if (!conversation.directPartner) return null;
  return (
    <>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label={t("chat.secret.action")}
        onClick={() => setOpen(true)}
      >
        <HugeiconsIcon icon={SquareLock02Icon} />
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <SecretSendSteps
            conversationId={conversation.id}
            partner={conversation.directPartner.displayName}
            onClose={() => setOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
