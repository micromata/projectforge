"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { CostLookup } from "./cost-lookup";

/**
 * The cost search ("Kost-Suche"): a cost 1 and a cost 2 unit looked up to their number and long name.
 * One component for its three places — the page `/next/costSearch`, the floating panel on every page
 * (CostSearchPanel) and the pop-out window (`/next/costSearch/popout`) —, compact enough for the panel:
 * a label column beside the pickers, the results right below them.
 */
export function CostSearchConverter({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <div
      className={cn(
        "grid grid-cols-[auto_minmax(0,1fr)] items-center gap-x-3 gap-y-1.5",
        className
      )}
    >
      <CostLookup kind="cost1" label={t("fibu.kost1._")} />
      <CostLookup kind="cost2" label={t("fibu.kost2._")} className="mt-2" />
    </div>
  );
}
