"use client";

import { useTranslations } from "next-intl";
import { RichTextField } from "@/components/shared/form/rich-text-field";

/** What the script is about, as rich text: the execution page shows it above the parameters. */
export function ScriptDescriptionField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <RichTextField
      name="description"
      label={t("description")}
      className={className}
    />
  );
}
