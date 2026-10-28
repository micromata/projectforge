"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { openChatDirect } from "@/lib/rs/chat-direct";
import { toast } from "@/lib/toast";
import { SecretStartSteps } from "./secret-start-steps";

/**
 * Starting a confidential conversation from the chat list: the person is chosen like for a direct message (the
 * user selection, checked again by the server), then the steps of SecretStartSteps follow in the same dialog.
 * The conversation is the direct chat with that person (created if missing, but without messages).
 */
export function ChatSecretDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const t = useTranslations();
  const [user, setUser] = useState<EntityRef | null>(null);
  const [started, setStarted] = useState<{
    conversationId: number;
    partner: string;
  } | null>(null);
  const openDirect = useMutation({
    mutationFn: (selected: EntityRef) =>
      openChatDirect(selected.id).then(({ conversationId }) => ({
        conversationId,
        partner: selected.displayName,
      })),
    onSuccess: setStarted,
    onError: (error: Error) => toast.error(error.message),
  });
  const close = () => {
    setUser(null);
    setStarted(null);
    onClose();
  };
  return (
    <Dialog open={open} onOpenChange={(next) => !next && close()}>
      <DialogContent className="sm:max-w-md">
        {started ? (
          <SecretStartSteps
            conversationId={started.conversationId}
            partner={started.partner}
            onClose={close}
          />
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>{t("chat.secret.new")}</DialogTitle>
              <DialogDescription>{t("chat.secret.newInfo")}</DialogDescription>
            </DialogHeader>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="chatSecretUser">{t("chat.direct.user")}</Label>
              <EntityAutocomplete
                id="chatSecretUser"
                url="user/autosearch?search=:search"
                value={user}
                onChange={setUser}
                autoFocus
                aria-label={t("chat.direct.user")}
              />
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={close}>
                {t("chat.message.cancel")}
              </Button>
              <Button
                type="button"
                disabled={!user || openDirect.isPending}
                onClick={() => user && openDirect.mutate(user)}
              >
                {t("chat.secret.next")}
              </Button>
            </DialogFooter>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
