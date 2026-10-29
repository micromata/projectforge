"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Notification03Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { OrderNotificationDialog } from "./order-notification-dialog";
import type { OrderNotificationRule } from "@/lib/rs/order-notification";

/**
 * Opens the user's order notification settings (immediately, daily summary, muted; exceptions per project
 * or customer) in a dialog, so they are reachable from the order list and from an order without a detour
 * over the my-account page. The dialog saves or discards its changes as a whole.
 */
export function OrderNotificationButton({
  suggestions,
}: {
  /** Project and customer of the order being edited, offered as exceptions to add. */
  suggestions?: Pick<OrderNotificationRule, "type" | "id" | "displayName">[];
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        onClick={() => setOpen(true)}
      >
        <HugeiconsIcon icon={Notification03Icon} size={14} aria-hidden />
        {t("fibu.auftrag.notification.prefs.title")}
      </Button>
      {open && (
        <OrderNotificationDialog
          suggestions={suggestions}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
