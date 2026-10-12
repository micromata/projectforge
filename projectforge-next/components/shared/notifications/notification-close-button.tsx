"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/**
 * Closes a toast or banner, so it never blocks the work. Not a confirmation: the notification stays
 * in the bell, where it can be confirmed (or marked as done) later.
 */
export function NotificationCloseButton({
  onClose,
  className,
}: {
  onClose: () => void;
  className?: string;
}) {
  const t = useTranslations();
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      aria-label={t("close")}
      className={cn("size-7 shrink-0", className)}
      onClick={onClose}
    >
      <HugeiconsIcon icon={Cancel01Icon} size={16} />
    </Button>
  );
}
