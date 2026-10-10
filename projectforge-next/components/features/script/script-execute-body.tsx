"use client";

import { useTranslations } from "next-intl";
import { CopyableValue } from "@/components/shared/copyable-value";
import { CodeEditorField } from "@/components/shared/form/code-editor-field";
import { RichText } from "@/components/shared/rich-text";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type {
  Script,
  ScriptDownload,
  ScriptExecuteEndpoint,
  ScriptExecutionResult,
  ScriptLogEntry,
} from "@/lib/rs/script";
import { ScriptAvailableVariables } from "./script-available-variables";
import { ScriptDownloadBanner } from "./script-download-banner";
import { ScriptExecuteParameters } from "./script-execute-parameters";
import { ScriptExecuteResult } from "./script-execute-result";
import { ScriptLogTable } from "./script-log-table";

/** The tabs of the execution page, see [scriptExecuteTabs]. */
export type ScriptExecuteTab = "execute" | "output" | "executableBy";

/**
 * The tabs the page offers: the execution always, its output once there is one (this execution's, or
 * the file of the user's last one), who may execute it where the backend tells (administration only).
 */
export function scriptExecuteTabs(
  script: Script | undefined,
  hasOutput: boolean
): ScriptExecuteTab[] {
  return [
    "execute",
    ...(hasOutput ? (["output"] as const) : []),
    ...(script?.executableByEmails ? (["executableBy"] as const) : []),
  ];
}

/**
 * The scrolling part of the execution page, as tabs: what the script is about and what it asks for —
 * the parameters of a stored script, the code of an ad-hoc one —, then what its execution produced
 * (the page switches there after a run), and who may execute it.
 *
 * A file to download is on top of both of the first tabs: it is what the run was for, and the user
 * comes back to the first tab for the next run.
 */
export function ScriptExecuteBody({
  endpoint,
  script,
  tab,
  onTabChange,
  hasOutput,
  result,
  download,
  log,
  logFetching,
}: {
  endpoint: ScriptExecuteEndpoint;
  script: Script;
  tab: string;
  onTabChange: (tab: string) => void;
  hasOutput: boolean;
  result: ScriptExecutionResult | undefined;
  download: ScriptDownload | null;
  log: readonly ScriptLogEntry[] | undefined;
  logFetching: boolean;
}) {
  const t = useTranslations();
  const emails = script.executableByEmails;
  const banner = (
    <ScriptDownloadBanner endpoint={endpoint} download={download} />
  );
  return (
    <Tabs
      value={tab}
      onValueChange={onTabChange}
      className="mx-auto w-full max-w-5xl p-4"
    >
      <TabsList>
        <TabsTrigger value="execute">
          {t("scripting.script.execute")}
        </TabsTrigger>
        {hasOutput && (
          <TabsTrigger value="output">
            {t("scripting.script.output")}
          </TabsTrigger>
        )}
        {emails && (
          <TabsTrigger value="executableBy">
            {t("scripting.script.executableByUsers._")}
          </TabsTrigger>
        )}
      </TabsList>
      <TabsContent value="execute" className="space-y-4 pt-2">
        {banner}
        {script.description && (
          <RichText
            html={script.description}
            className="rounded-md border border-primary/30 bg-primary/5 dark:border-primary/50 dark:bg-primary/15 px-4 py-3 text-sm"
          />
        )}
        {script.id == null ? (
          <>
            <CodeEditorField
              name="script"
              label={t("scripting.script.code")}
              language={script.type === "GROOVY" ? "groovy" : "kotlin"}
              minHeight="24rem"
            />
            <ScriptAvailableVariables variables={script.availableVariables} />
          </>
        ) : (
          <ScriptExecuteParameters script={script} />
        )}
      </TabsContent>
      {hasOutput && (
        <TabsContent value="output" className="space-y-4 pt-2">
          {banner}
          <ScriptExecuteResult result={result} />
          {log && log.length > 0 && (
            <ScriptLogTable entries={log} isFetching={logFetching} />
          )}
        </TabsContent>
      )}
      {emails && (
        <TabsContent value="executableBy" className="pt-2">
          <CopyableValue
            value={emails}
            label={t("scripting.script.executableByUsers._")}
          />
        </TabsContent>
      )}
    </Tabs>
  );
}
