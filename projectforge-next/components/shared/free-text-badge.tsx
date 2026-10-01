"use client";

import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

/**
 * Marks a value as typed text rather than a picked record — the free-text customer of an order or an
 * invoice, which looks like a customer's name but links to no customer.
 *
 * In the warning tone on purpose: a free text is a legitimate choice, but one that is easily made by
 * mistake when a record was meant, so it has to stand out wherever the name is shown (the picker's
 * trigger, the list cell).
 */
export function FreeTextBadge({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <Badge
      variant="outline"
      className={cn(
        "h-4 shrink-0 border-warning px-1 text-[10px] font-medium not-italic text-warning",
        className
      )}
    >
      {t("fibu.kunde.freeText._")}
    </Badge>
  );
}
