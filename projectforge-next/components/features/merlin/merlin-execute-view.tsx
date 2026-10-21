"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlayIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { Spinner } from "@/components/shared/spinner";
import {
  useSubmitShortcut,
  useSubmitShortcutHint,
} from "@/hooks/use-submit-shortcut";
import { MerlinExecuteHeaderActions } from "./merlin-execute-header-actions";
import { MerlinExecuteInputs } from "./merlin-execute-inputs";
import { merlinExecuteMetadata } from "./merlin-execute-metadata";
import { MERLIN_ROUTE } from "./merlin-routes";
import { MerlinSerialExecution } from "./merlin-serial-execution";
import { useMerlinExecution } from "./use-merlin-execution";

/**
 * Executes a Word® template: one document from the values entered here (Word® or PDF), or the documents of
 * all rows of an Excel® file (serial execution).
 */
export function MerlinExecuteView({ id }: { id: number }) {
  const t = useTranslations();
  const router = useRouter();
  const shortcutHint = useSubmitShortcutHint();
  const { form, load, execution, submit } = useMerlinExecution(id);
  const data = load.data;
  const context = useMemo(
    () => ({
      form,
      metadata: merlinExecuteMetadata(data?.variables ?? []),
      data,
    }),
    [form, data]
  );
  const onKeyDown = useSubmitShortcut(submit, !!data && !execution.isPending);

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.misc")}
        title={data?.name || t("plugins.merlin.templateExecutor.heading")}
      >
        <MerlinExecuteHeaderActions execution={data} />
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
            ) : !data ? (
              <div className="flex items-center justify-center p-8">
                <Spinner />
              </div>
            ) : (
              <div className="mx-auto flex max-w-5xl flex-col gap-4 p-4">
                <SectionCard>
                  <SectionHeader
                    title={t("plugins.merlin.templateExecutor.heading")}
                  />
                  {data.description && (
                    <p className="mb-4 whitespace-pre-wrap text-sm text-muted-foreground">
                      {data.description}
                    </p>
                  )}
                  <MerlinExecuteInputs variables={data.variables} />
                </SectionCard>
                <SectionCard>
                  <SectionHeader
                    title={t("plugins.merlin.serial.template.download._")}
                  />
                  <MerlinSerialExecution id={id} />
                </SectionCard>
              </div>
            )}
          </div>
          <FormActionBar className="mx-auto max-w-5xl">
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(MERLIN_ROUTE)}
            >
              {t("back")}
            </Button>
            <HintTooltip {...shortcutHint}>
              <Button
                type="button"
                disabled={!data || execution.isPending}
                onClick={submit}
              >
                {execution.isPending ? (
                  <Spinner className="h-3.5 w-3.5 border-2" />
                ) : (
                  <HugeiconsIcon icon={PlayIcon} size={14} aria-hidden />
                )}
                {t("plugins.merlin.templateExecutor.execute")}
              </Button>
            </HintTooltip>
          </FormActionBar>
        </div>
      </EntityEditFormProvider>
    </PageShell>
  );
}
