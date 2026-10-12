"use client";

import { useTranslations } from "next-intl";
import { MenuLink } from "@/components/shared/menu-link";
import { RichText } from "@/components/shared/rich-text";
import type { AppNotification } from "@/lib/rs/notification";

/** The text of a notification and the link to where its cause is resolved (e.g. the monthly report). */
export function NotificationBody({
  notification,
  onOpen,
}: {
  notification: AppNotification;
  /** Called when the link is followed, e.g. to close the toast or menu it is shown in. */
  onOpen?: () => void;
}) {
  const t = useTranslations("notification");
  return (
    <div className="space-y-1 text-sm">
      {notification.body && <RichText html={notification.body} />}
      {notification.link && (
        <MenuLink
          url={notification.link}
          className="font-medium underline"
          onClick={onOpen}
        >
          {t("open")}
        </MenuLink>
      )}
    </div>
  );
}
