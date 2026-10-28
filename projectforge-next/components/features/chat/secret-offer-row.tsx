"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Button } from "@/components/ui/button";
import type { ChatSecretSession } from "@/lib/rs/chat-secret";

/**
 * A confidential conversation offered to the user: accept or decline. One already accepted that isn't
 * open in this window runs in another one, or its key is gone (reload): discarding it asks first, as it
 * would also end the other window's session.
 */
export function SecretOfferRow({
  session,
  accepting,
  canAccept,
  onAccept,
  onDiscard,
}: {
  session: ChatSecretSession;
  accepting: boolean;
  canAccept: boolean;
  onAccept: () => void;
  onDiscard: () => void;
}) {
  const t = useTranslations();
  const [confirm, setConfirm] = useState(false);
  const sender = session.initiator.displayName;
  const offered = session.state === "OFFERED";
  return (
    <div
      role="status"
      // Comes in noticeably: it appears below a conversation that may be read at the moment.
      className="flex shrink-0 flex-wrap items-center gap-2 border-t bg-primary/5 px-3 py-2 text-sm duration-500 animate-in fade-in slide-in-from-bottom-2"
      data-testid="chat-secret-offer"
    >
      <p className="flex-1">
        {offered
          ? t("chat.secret.offered", { arg0: sender })
          : t("chat.secret.elsewhere", { arg0: sender })}
      </p>
      <Button
        type="button"
        size="sm"
        variant="outline"
        onClick={() => (offered ? onDiscard() : setConfirm(true))}
      >
        {offered ? t("chat.secret.decline") : t("chat.secret.discard")}
      </Button>
      {offered && (
        <Button
          type="button"
          size="sm"
          disabled={accepting || !canAccept}
          onClick={onAccept}
        >
          {t("chat.secret.accept")}
        </Button>
      )}
      <ConfirmDialog
        open={confirm}
        onOpenChange={setConfirm}
        title={t("chat.secret.discard")}
        description={t("chat.secret.discardConfirm")}
        confirmLabel={t("chat.secret.discard")}
        destructive
        onConfirm={onDiscard}
      />
    </div>
  );
}
