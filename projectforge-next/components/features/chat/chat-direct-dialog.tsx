"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
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
import { useOpenDirect } from "@/hooks/use-open-direct";

/**
 * Starting a direct message (PF-CHAT-DM): the user is chosen from those the user may see (the user selection,
 * checked again by the server); opening goes to the conversation, created if missing.
 */
export function ChatDirectDialog({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const t = useTranslations();
  const [user, setUser] = useState<EntityRef | null>(null);
  const openDirect = useOpenDirect();
  const close = () => {
    setUser(null);
    onClose();
  };
  return (
    <Dialog open={open} onOpenChange={(next) => !next && close()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t("chat.direct.new")}</DialogTitle>
          <DialogDescription>{t("chat.direct.info")}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatDirectUser">{t("chat.direct.user")}</Label>
          <EntityAutocomplete
            id="chatDirectUser"
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
            onClick={() => user && openDirect.mutate(user.id)}
          >
            {t("chat.direct.open")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
