"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import type { ChatRef } from "@/lib/rs/chat-types";
import { useBlockedUsers } from "./use-blocked-users";

/**
 * Block or unblock a user from a menu: [label] and [run] for the item, [dialog] to render beside the menu (it must
 * outlive it): blocking asks first, unblocking doesn't.
 */
export function useBlockUserAction(user: ChatRef) {
  const t = useTranslations();
  const { isBlocked, block, unblock } = useBlockedUsers();
  const [confirming, setConfirming] = useState(false);
  const blocked = isBlocked(user.id);
  const dialog = (
    <ConfirmDialog
      open={confirming}
      onOpenChange={setConfirming}
      title={t("chat.block.user")}
      description={t("chat.block.confirm", { arg0: user.displayName })}
      confirmLabel={t("chat.block.user")}
      destructive
      onConfirm={() => block.mutate(user.id)}
    />
  );
  return {
    blocked,
    pending: block.isPending || unblock.isPending,
    label: blocked ? t("chat.block.unblock") : t("chat.block.user"),
    run: () => (blocked ? unblock.mutate(user.id) : setConfirming(true)),
    dialog,
  };
}
