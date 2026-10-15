"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { Clock01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { navigateInGesture } from "@/lib/navigate-in-gesture";

/**
 * The way from the rule list to the log of the notifications of all rules (see NotificationLogPage).
 * No gate: whoever sees the rule list (admins and finance) may read the log as well.
 */
export function NotificationRuleListActions() {
  const t = useTranslations();
  const router = useRouter();
  return (
    <Button
      type="button"
      variant="outline"
      onClick={() => navigateInGesture(router, "/notificationRule/log")}
    >
      <HugeiconsIcon icon={Clock01Icon} />
      {t("notification.log")}
    </Button>
  );
}
