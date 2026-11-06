"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { LockPasswordIcon, Message01Icon } from "@hugeicons/core-free-icons";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { RefName } from "@/components/shared/ref-list";
import { UserAvatar } from "@/components/shared/user-avatar";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { useOpenDirect } from "@/hooks/use-open-direct";
import { openChatDirect } from "@/lib/rs/chat-direct";
import { toast } from "@/lib/toast";
import { SecretStartSteps } from "./secret-start-steps";

/**
 * Users found by the search: a direct message (the conversation is created if missing) or a confidential
 * conversation can be started with each.
 */
export function ChatDirectCandidates({
  users,
  highlight,
}: {
  users: EntityRef[];
  highlight?: string;
}) {
  const t = useTranslations();
  const openDirect = useOpenDirect();
  const [secret, setSecret] = useState<{
    conversationId: number;
    partner: string;
  } | null>(null);
  const openSecret = useMutation({
    mutationFn: (user: EntityRef) =>
      openChatDirect(user.id).then(({ conversationId }) => ({
        conversationId,
        partner: user.displayName,
      })),
    onSuccess: setSecret,
    onError: (error: Error) => toast.error(error.message),
  });
  const pending = openDirect.isPending || openSecret.isPending;
  return (
    <>
      <ul className="flex max-w-3xl flex-col divide-y rounded-md border">
        {users.map((user) => (
          <li key={user.id} className="flex items-center gap-2.5 px-3 py-2">
            <UserAvatar name={user.displayName} flags={user} userId={user.id} />
            <RefName
              value={user}
              highlight={highlight}
              className="min-w-0 flex-1 truncate font-semibold"
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={pending}
              aria-label={`${t("chat.direct.new")}: ${user.displayName}`}
              onClick={() => openDirect.mutate(user.id)}
            >
              <HugeiconsIcon icon={Message01Icon} data-icon="inline-start" />
              {t("chat.direct.new")}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              disabled={pending}
              aria-label={`${t("chat.secret.new")}: ${user.displayName}`}
              onClick={() => openSecret.mutate(user)}
            >
              <HugeiconsIcon
                icon={LockPasswordIcon}
                strokeWidth={2}
                data-icon="inline-start"
              />
              {t("chat.secret.new")}
            </Button>
          </li>
        ))}
      </ul>
      <Dialog open={secret != null} onOpenChange={(o) => !o && setSecret(null)}>
        <DialogContent className="sm:max-w-md">
          {secret && (
            <SecretStartSteps
              conversationId={secret.conversationId}
              partner={secret.partner}
              onClose={() => setSecret(null)}
            />
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
