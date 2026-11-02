"use client";

import { useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Notification03Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { OrderNotificationDialog } from "./order-notification-dialog";
import type { OrderNotificationRule } from "@/lib/rs/order-notification";

/**
 * The query parameter opening the dialog on load (`/order/4711?notificationSettings`, or on the list
 * without an order), the target of the button in the order mails; the counterpart of
 * `OrderNotificationPrefService.SETTINGS_PARAM`.
 */
export const ORDER_NOTIFICATION_SETTINGS_PARAM = "notificationSettings";

type Suggestions = Pick<OrderNotificationRule, "type" | "id" | "displayName">[];

/**
 * Whether the url asks for the settings dialog ([ORDER_NOTIFICATION_SETTINGS_PARAM]), and the way to take
 * that request back off the url, so a reload doesn't open the dialog again.
 */
function useNotificationSettingsParam() {
  const params = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const requested = params.has(ORDER_NOTIFICATION_SETTINGS_PARAM);
  const clear = () => {
    if (!requested) return;
    const rest = new URLSearchParams(params);
    rest.delete(ORDER_NOTIFICATION_SETTINGS_PARAM);
    const query = rest.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, {
      scroll: false,
    });
  };
  return { requested, clear };
}

/**
 * The settings dialog, open as long as the url asks for it. Mounted where it is always rendered (the banner
 * of the order edit page, see OrderEditBanner) rather than at the button, which only a user with write access
 * sees; the readers of an order get the mails, too.
 */
export function OrderNotificationUrlDialog({
  suggestions,
}: {
  suggestions?: Suggestions;
}) {
  const { requested, clear } = useNotificationSettingsParam();
  if (!requested) return null;
  return <OrderNotificationDialog suggestions={suggestions} onClose={clear} />;
}

/**
 * Opens the user's order notification settings (immediately, daily summary, muted; exceptions per project
 * or customer) in a dialog, so they are reachable from the order list and from an order without a detour
 * over the my-account page. The dialog saves or discards its changes as a whole.
 */
export function OrderNotificationButton({
  suggestions,
  openFromUrl,
}: {
  /** Project and customer of the order being edited, offered as exceptions to add. */
  suggestions?: Suggestions;
  /** Opens the dialog at once if the url carries [ORDER_NOTIFICATION_SETTINGS_PARAM] (link of the order mails). */
  openFromUrl?: boolean;
}) {
  const t = useTranslations();
  const fromUrl = useNotificationSettingsParam();
  const [open, setOpen] = useState(() => !!openFromUrl && fromUrl.requested);
  const close = () => {
    setOpen(false);
    if (openFromUrl) fromUrl.clear();
  };
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
        <OrderNotificationDialog suggestions={suggestions} onClose={close} />
      )}
    </>
  );
}
