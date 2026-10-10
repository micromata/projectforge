"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { MarkdownText } from "@/components/shared/markdown-text";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";
import { useTabParam } from "@/hooks/use-tab-param";
import { updateSearchParams } from "@/lib/search-params";
import type { ScriptExecuteEndpoint } from "@/lib/rs/script";
import { ScriptExecuteActions } from "./script-execute-actions";
import { ScriptExecuteBody, scriptExecuteTabs } from "./script-execute-body";
import { ScriptExecuteHeaderActions } from "./script-execute-header-actions";
import { SCRIPT_EXECUTE_METADATA } from "./script-execute-metadata";
import { useScriptExecution } from "./use-script-execution";

/**
 * Executes a script: a stored one with the parameter values entered here, or — in the administration,
 * `id` null — code typed or taken from an example.
 *
 * @param from The page target the script was started from by its button (see ScriptPageButtons).
 * @param backRoute The list "Back" returns to, unless started from a page: then back to that one.
 */
export function ScriptExecuteView({
  endpoint,
  id,
  example = null,
  from = null,
  backRoute,
}: {
  endpoint: ScriptExecuteEndpoint;
  id: number | null;
  example?: number | null;
  from?: string | null;
  backRoute: string;
}) {
  const t = useTranslations();
  const router = useRouter();
  const admin = endpoint === "scriptExecute";
  const { form, load, execution, log, result, download } = useScriptExecution(
    endpoint,
    id,
    example,
    from,
    // Once started, a run's output is what the user wants to see. What setTab does, which isn't declared
    // yet: the tabs offered depend on this hook's state.
    () => updateSearchParams({ [TAB_PARAM]: "output" }, "push")
  );
  const script = load.data?.script;
  // A running execution too: its log is polled into the output tab.
  const hasOutput =
    !execution.isIdle ||
    !!result ||
    !!download?.filenameAndSize ||
    !!log.data?.length;
  const [tab, setTab] = useTabParam(
    "execute",
    // Unknown while loading, so a deep link to a tab isn't lost (see useTabParam).
    script ? scriptExecuteTabs(script, hasOutput) : undefined
  );
  // Only what the backend confirmed: a page the script is configured for.
  const origin = load.data?.origin;
  const context = useMemo(
    () => ({ form, metadata: SCRIPT_EXECUTE_METADATA, data: script }),
    [form, script]
  );
  const submit = () => void form.handleSubmit();
  const onKeyDown = useSubmitShortcut(submit, !!script && !execution.isPending);

  return (
    <PageShell>
      <PageTitleRow
        category={t(admin ? "menu.reporting" : "menu.projectmanagement")}
        title={script?.name || t("scripting.script.execute")}
      >
        <ScriptExecuteHeaderActions
          admin={admin}
          id={id}
          logViewerUrl={load.data?.logViewerUrl}
        />
      </PageTitleRow>
      <EntityEditFormProvider value={context}>
        <div
          className="flex min-h-0 flex-1 flex-col overflow-hidden"
          onKeyDown={onKeyDown}
        >
          <div className="flex-1 overflow-y-auto">
            {load.isError ? (
              <p className="p-4 text-sm text-destructive">
                {load.error.message}
              </p>
            ) : !script ? (
              <div className="flex items-center justify-center p-8">
                <Spinner />
              </div>
            ) : (
              <>
                {origin && (
                  <MarkdownText
                    className="mx-auto max-w-5xl px-4 pt-3 text-sm text-muted-foreground"
                    text={t("scripting.script.execution.pageContext", {
                      arg0: origin.title,
                    })}
                  />
                )}
                <ScriptExecuteBody
                  endpoint={endpoint}
                  script={script}
                  tab={tab}
                  onTabChange={setTab}
                  hasOutput={hasOutput}
                  result={result}
                  download={download}
                  log={log.data}
                  logFetching={log.isFetching}
                />
              </>
            )}
          </div>
          <ScriptExecuteActions
            onBack={() => router.push(origin?.route ?? backRoute)}
            onExecute={submit}
            disabled={!script || execution.isPending}
            running={execution.isPending}
          />
        </div>
      </EntityEditFormProvider>
    </PageShell>
  );
}
