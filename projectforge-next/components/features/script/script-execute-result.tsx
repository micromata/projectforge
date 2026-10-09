"use client";

import { useTranslations } from "next-intl";
import { MarkdownText } from "@/components/shared/markdown-text";
import {
  downloadScriptResult,
  type ScriptDownload,
  type ScriptExecuteEndpoint,
  type ScriptExecutionResult,
} from "@/lib/rs/script";
import { cn } from "@/lib/utils";
import { ScriptDownloadButton } from "./script-download-button";

/**
 * What the execution returned (Markdown, red when it failed) and the file it produced — or the file of
 * the user's last execution, which the backend keeps for a few minutes.
 */
export function ScriptExecuteResult({
  endpoint,
  result,
  download,
}: {
  endpoint: ScriptExecuteEndpoint;
  result: ScriptExecutionResult | undefined;
  download: ScriptDownload | null;
}) {
  const t = useTranslations();
  if (!result?.result && !download?.filenameAndSize) return null;
  return (
    <section className="space-y-3">
      {result?.result && (
        <div
          className={cn(
            "rounded-md border px-3 py-2 text-sm",
            result.hasErrors && "border-destructive text-destructive"
          )}
        >
          <h2 className="mb-1 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
            {t("scripting.script.result")}
          </h2>
          <MarkdownText text={result.result} />
        </div>
      )}
      {download?.filenameAndSize && (
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <ScriptDownloadButton
            download={() => downloadScriptResult(endpoint)}
            hint={t("scripting.download.filename.info")}
          >
            {download.filenameAndSize}
          </ScriptDownloadButton>
          {download.availableUntil && (
            <span className="text-xs text-muted-foreground">
              {t("scripting.download.filename.additional", {
                arg0: download.availableUntil,
              })}
            </span>
          )}
        </div>
      )}
    </section>
  );
}
