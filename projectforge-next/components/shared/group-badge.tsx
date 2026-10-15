"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

/**
 * Marks a value standing for several others — a customer group in the customer checklist, which picks all
 * of its customers at once. A sibling of [FreeTextBadge], in the primary tone: nothing to watch out for.
 */
export function GroupBadge({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <Badge
      variant="outline"
      className={cn(
        "h-4 shrink-0 border-primary px-1 text-[10px] font-medium not-italic text-primary",
        className
      )}
    >
      {t("fibu.customerGroups.badge")}
    </Badge>
  );
}
