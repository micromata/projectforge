"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { MarkdownText } from "@/components/shared/markdown-text";
import { cn } from "@/lib/utils";
import type { BankAccountDetail } from "./types";

/**
 * The help on the import settings beside their field: the current settings with every importable field, for copy
 * & paste (`BankAccount.importSettingsHelp`, built by the backend from the stored settings).
 */
export function BankAccountImportSettingsHelp({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const help = useEntityData<BankAccountDetail>()?.importSettingsHelp;
  if (!help) return null;
  return (
    <div
      className={cn(
        "flex flex-col gap-2 rounded-md bg-muted/40 p-3 text-xs text-muted-foreground",
        className
      )}
    >
      <p className="text-sm font-semibold text-foreground">
        {t("import.help.settings.title")}
      </p>
      <MarkdownText text={help} />
    </div>
  );
}
