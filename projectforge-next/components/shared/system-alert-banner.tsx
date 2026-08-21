"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Notification03Icon } from "@hugeicons/core-free-icons";
import { useAuth } from "@/hooks/use-auth";

/**
 * The system alert message an admin sets on the admin page (Wicket, `/wa/admin`): an announcement
 * every logged-in user has to see, a planned downtime being the typical one. Shown on every page
 * of this app, as in Wicket and the legacy React app, and closeable by nobody - it goes away when
 * the admin clears it.
 *
 * The text is the admin's own, in the language they wrote it in, so nothing here is translated.
 * It arrives with `userStatus`, whose refresh policy (see useAuth) is what lets an announcement
 * reach a tab that is already open.
 */
export function SystemAlertBanner() {
  const { alertMessage } = useAuth();
  if (!alertMessage) return null;

  return (
    <div
      role="alert"
      data-testid="system-alert-message"
      // shrink-0: PageShell is a flex column of fixed height, and a long message must push the
      // page content down rather than give up its own lines.
      className="flex shrink-0 items-start gap-2 bg-destructive px-4 py-2 text-sm font-medium text-white"
    >
      <HugeiconsIcon
        icon={Notification03Icon}
        size={18}
        aria-hidden
        className="mt-px shrink-0"
      />
      {/* The admin writes into a textarea, so line breaks are part of the message. */}
      <span className="break-words whitespace-pre-wrap">{alertMessage}</span>
    </div>
  );
}
