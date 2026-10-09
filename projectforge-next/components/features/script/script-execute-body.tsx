"use client";

import { useTranslations } from "next-intl";
import { CopyableValue } from "@/components/shared/copyable-value";
import { CodeEditorField } from "@/components/shared/form/code-editor-field";
import { RichText } from "@/components/shared/rich-text";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useTabParam } from "@/hooks/use-tab-param";
import type {
  Script,
  ScriptDownload,
  ScriptExecuteEndpoint,
  ScriptExecutionResult,
  ScriptLogEntry,
} from "@/lib/rs/script";
import { ScriptAvailableVariables } from "./script-available-variables";
import { ScriptExecuteParameters } from "./script-execute-parameters";
import { ScriptExecuteResult } from "./script-execute-result";
import { ScriptLogTable } from "./script-log-table";

/**
 * The scrolling part of the execution page: what the script is about and what it asks for — the
 * parameters of a stored script, the code of an ad-hoc one —, then what its execution produced. Who
 * may execute it (administration only) is a tab of its own beside all of that, the tab bar on top.
 */
export function ScriptExecuteBody({
  endpoint,
  script,
  result,
  download,
  log,
  logFetching,
}: {
  endpoint: ScriptExecuteEndpoint;
  script: Script;
  result: ScriptExecutionResult | undefined;
  download: ScriptDownload | null;
  log: readonly ScriptLogEntry[] | undefined;
  logFetching: boolean;
}) {
  const t = useTranslations();
  const emails = script.executableByEmails;
  const [tab, setTab] = useTabParam(
    "execute",
    emails ? ["execute", "executableBy"] : ["execute"]
  );
  const execute = (
    <div className="space-y-4">
      {script.description && (
        <RichText
          html={script.description}
          className="rounded-md border border-primary/30 bg-primary/5 px-4 py-3 text-sm font-normal"
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
      <ScriptExecuteResult
        endpoint={endpoint}
        result={result}
        download={download}
      />
      {log && log.length > 0 && (
        <ScriptLogTable entries={log} isFetching={logFetching} />
      )}
    </div>
  );
  return (
    <div className="mx-auto w-full max-w-5xl space-y-4 p-4">
      {emails ? (
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList>
            <TabsTrigger value="execute">
              {t("scripting.script.execute")}
            </TabsTrigger>
            <TabsTrigger value="executableBy">
              {t("scripting.script.executableByUsers._")}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="execute" className="pt-2">
            {execute}
          </TabsContent>
          <TabsContent value="executableBy" className="pt-2">
            <CopyableValue
              value={emails}
              label={t("scripting.script.executableByUsers._")}
            />
          </TabsContent>
        </Tabs>
      ) : (
        execute
      )}
    </div>
  );
}
