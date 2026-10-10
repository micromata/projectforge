"use client";

import { useTranslations } from "next-intl";
import {
  downloadScriptResult,
  type ScriptDownload,
  type ScriptExecuteEndpoint,
} from "@/lib/rs/script";
import { ScriptDownloadButton } from "./script-download-button";

/**
 * The file the execution produced — or the user's last execution, which the backend keeps for a few
 * minutes — as a box at the top of a tab, where it is seen at once: on the output tab and on the
 * execution tab, which a user returns to for the next run.
 */
export function ScriptDownloadBanner({
  endpoint,
  download,
}: {
  endpoint: ScriptExecuteEndpoint;
  download: ScriptDownload | null;
}) {
  const t = useTranslations();
  if (!download?.filenameAndSize) return null;
  return (
    <section className="flex flex-wrap items-center gap-3 rounded-md border border-primary/30 bg-primary/5 dark:border-primary/50 dark:bg-primary/15 px-4 py-3 text-sm">
      <h2 className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
        {t("scripting.download.filename._")}
      </h2>
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
    </section>
  );
}
