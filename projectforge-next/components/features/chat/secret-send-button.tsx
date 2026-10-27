"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { LockPasswordIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import type { ChatConversation } from "@/lib/rs/chat-types";
import { SecretStartSteps } from "./secret-start-steps";

/** Opens the dialog starting a confidential conversation, in direct chats only. */
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
      <HintTooltip text={t("chat.secret.action")}>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t("chat.secret.action")}
          onClick={() => setOpen(true)}
        >
          {/* The heavier stroke sets the lock apart from the thin outline icons around it. */}
          <HugeiconsIcon icon={LockPasswordIcon} strokeWidth={2} />
        </Button>
      </HintTooltip>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <SecretStartSteps
            conversationId={conversation.id}
            partner={conversation.directPartner.displayName}
            onClose={() => setOpen(false)}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}
