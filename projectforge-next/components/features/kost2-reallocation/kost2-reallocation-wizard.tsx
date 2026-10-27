"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { useDocumentTitle } from "@/hooks/use-document-title";
import {
  fetchKost2ReallocationRun,
  type Kost2ReallocationRun,
} from "@/lib/rs/kost2-reallocation";
import { PlanView } from "./plan-view";
import { RunDefinitionStep } from "./run-definition-step";
import { TimesheetsStep } from "./timesheets-step";
import { emptyRunDraft, type RunDraft } from "./types";
import { runKey, useRunActions } from "./use-run-actions";
import { statusLabel } from "./status-label";
import { WizardActions } from "./wizard-actions";

const toDraft = (run: Kost2ReallocationRun): RunDraft => ({
  description: run.description ?? "",
  projekt: run.projekt ?? null,
  taskId: run.taskId ?? null,
  fromYear: run.fromYear ?? null,
  fromMonth: run.fromMonth ?? null,
  toYear: run.toYear ?? null,
  toMonth: run.toMonth ?? null,
  comment: run.comment ?? "",
  rules: run.rules,
});

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="border-b pb-1 text-base font-semibold">{title}</h2>
      {children}
    </section>
  );
}

function WizardBody({ run }: { run: Kost2ReallocationRun | null }) {
  const t = useTranslations();
  const [draft, setDraft] = useState<RunDraft>(
    run ? toDraft(run) : emptyRunDraft()
  );
  const [manualIds, setManualIds] = useState<Set<number>>(new Set());
  const actions = useRunActions(run?.id ?? null, draft, manualIds);
  const readOnly = run != null && run.status !== "DRAFT";
  const title = draft.description || t("fibu.kost2Reallocation.title");
  useDocumentTitle(title);

  return (
    <PageShell>
      <PageTitleRow
        category={t("fibu.kost2Reallocation.title")}
        title={title}
      />
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        <div className="flex flex-1 flex-col gap-6 overflow-y-auto p-4">
          {run && (
            <FormAlert tone={readOnly ? "info" : "success"}>
              {statusLabel(t, run.status ?? "DRAFT")}
              {run.needsReapply &&
                ` – ${t("fibu.kost2Reallocation.needsReapply")}`}
            </FormAlert>
          )}
          {actions.error && <FormAlert tone="error">{actions.error}</FormAlert>}
          <Section title={t("fibu.kost2Reallocation.step.definition")}>
            <RunDefinitionStep
              draft={draft}
              onChange={setDraft}
              readOnly={readOnly}
            />
          </Section>
          {run?.id != null && (
            <Section title={t("fibu.kost2Reallocation.step.timesheets")}>
              <TimesheetsStep
                runId={run.id}
                manualIds={manualIds}
                onManualIdsChange={setManualIds}
                readOnly={readOnly}
              />
            </Section>
          )}
          <Section title={t("fibu.kost2Reallocation.step.preview")}>
            {!readOnly && (
              <div>
                <Button
                  variant="outline"
                  disabled={actions.pending}
                  onClick={() => actions.preview.mutate()}
                >
                  {t("fibu.kost2Reallocation.preview")}
                </Button>
              </div>
            )}
            {actions.plan && <PlanView plan={actions.plan} />}
          </Section>
        </div>
        <WizardActions run={run} actions={actions} />
      </div>
    </PageShell>
  );
}

/** The Kost2 reallocation wizard of one run (`runId` null: a new draft): define, select time sheets, preview, apply. */
export function Kost2ReallocationWizard({ runId }: { runId: number | null }) {
  const query = useQuery({
    queryKey: runKey(runId),
    queryFn: ({ signal }) => fetchKost2ReallocationRun(runId!, signal),
    enabled: runId != null,
    // The draft on screen is edited locally: a refetch (focus) must not replace it.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  if (runId != null && !query.data) {
    return (
      <PageShell>
        <div className="flex flex-1 items-center justify-center">
          <Spinner />
        </div>
      </PageShell>
    );
  }
  // Remounted when the server's run changes (saved, applied, reverted): the local draft starts from it.
  return <WizardBody key={query.dataUpdatedAt} run={query.data ?? null} />;
}
