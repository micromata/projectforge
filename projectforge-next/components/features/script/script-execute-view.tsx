"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlayIcon } from "@hugeicons/core-free-icons";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { MarkdownText } from "@/components/shared/markdown-text";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import type { ScriptExecuteEndpoint } from "@/lib/rs/script";
import { ScriptExecuteBody } from "./script-execute-body";
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
  const { form, load, execution, log, download } = useScriptExecution(
    endpoint,
    id,
    example,
    from
  );
  const script = load.data?.script;
  // Only what the backend confirmed: a page the script is configured for.
  const origin = load.data?.origin;
  const context = useMemo(
    () => ({ form, metadata: SCRIPT_EXECUTE_METADATA, data: script }),
    [form, script]
  );
  const submit = () => void form.handleSubmit();
  const onKeyDown = useSubmitShortcut(submit, !!script && !execution.isPending);
  const shortcutHint = useSubmitShortcutHint();

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
                  result={execution.data}
                  download={download}
                  log={log.data}
                  logFetching={log.isFetching}
                />
              </>
            )}
          </div>
          <FormActionBar className="mx-auto max-w-5xl">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(origin?.route ?? backRoute)}
            >
              {t("back")}
            </Button>
            <HintTooltip {...shortcutHint}>
              <Button
                type="button"
                disabled={!script || execution.isPending}
                onClick={submit}
              >
                {execution.isPending ? (
                  <Spinner className="h-3.5 w-3.5 border-2" />
                ) : (
                  <HugeiconsIcon icon={PlayIcon} size={14} aria-hidden />
                )}
                {t("execute")}
              </Button>
            </HintTooltip>
          </FormActionBar>
        </div>
      </EntityEditFormProvider>
    </PageShell>
  );
}
