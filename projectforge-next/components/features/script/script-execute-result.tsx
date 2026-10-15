"use client";

import { useTranslations } from "next-intl";
import { MarkdownText } from "@/components/shared/markdown-text";
import type { ScriptExecutionResult } from "@/lib/rs/script";
import { cn } from "@/lib/utils";

/** What the execution returned (Markdown, red when it failed). */
export function ScriptExecuteResult({
  result,
}: {
  result: ScriptExecutionResult | undefined;
}) {
  const t = useTranslations();
  if (!result?.result) return null;
  return (
    <section
      className={cn(
        "rounded-md border px-3 py-2 text-sm",
        result.hasErrors && "border-destructive text-destructive"
      )}
    >
      <h2 className="mb-1 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {t("scripting.script.result")}
      </h2>
      <MarkdownText text={result.result} />
    </section>
  );
}
