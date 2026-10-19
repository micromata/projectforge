"use client";

import { useTranslations } from "next-intl";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { CostSearchConverter } from "./cost-search-converter";

/**
 * The cost search alone, for the pop-out window: no PageShell, as header, menu and status bar would take
 * most of a small window's room, and the window is for this lookup only.
 */
export function CostSearchPopout() {
  const t = useTranslations();
  const title = t("menu.fibu.kostSearch");
  useDocumentTitle(title);
  return (
    <main className="flex min-h-dvh flex-col gap-3 p-3">
      <h1 className="text-sm font-semibold">{title}</h1>
      <CostSearchConverter />
    </main>
  );
}
