"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { Spinner } from "@/components/shared/spinner";
import {
  TASK_TREE_ROUTE,
  SAVED_ID_PARAM,
} from "@/components/shared/tasks/task-routes";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  executeTaskWizard,
  fetchTaskInfo,
  type TaskWizardResult,
} from "@/lib/rs/task";
import { GROUP_STEPS, type WizardGroupKey, type WizardGroups } from "./types";
import { stashWizardGroups, takeWizardGroups } from "./wizard-handover";
import { WizardGroupStepCard } from "./wizard-group-step";
import { WizardResult } from "./wizard-result";
import { WizardTaskStep } from "./wizard-task-step";

/**
 * The structure wizard: picks one structure element and up to three groups, then grants each group its
 * rights on that element and read access on its ancestors.
 *
 * Plain `useState` rather than a form library, like the other forms of this app that stand for no
 * entity (see the login and the password reset): there are four values, no field metadata to read and
 * no client side validation beyond "an element is picked" — the rules are the backend's
 * (`TaskWizardService`).
 *
 * Afterwards the steps give way to what was granted (see WizardResult) instead of a toast and a jump
 * back to the tree: which rights were new, which were raised and which were already there is the
 * answer to what the wizard was started for, and a toast cannot hold it.
 */
export function TaskWizard() {
  const t = useTranslations();
  const router = useRouter();
  // What a return from the task form brings back: the element that was just created — its id is in the
  // url (`?savedId=`, see useEditReturn) — and the groups that were picked before that detour, which no
  // url could carry (see wizard-handover.ts). Without the parameter this is a wizard opened fresh.
  const savedId = Number(useSearchParams().get(SAVED_ID_PARAM));
  const returning = savedId > 0;
  const [taskId, setTaskId] = useState<number | null>(
    returning ? savedId : null
  );
  const [groups, setGroups] = useState<WizardGroups>(
    returning ? takeWizardGroups() : {}
  );
  const [error, setError] = useState<string | null>(null);
  // What the wizard granted, once it has: with it the steps give way to the report of the rights (see
  // WizardResult), because "it worked" is not what the user came here to learn.
  const [result, setResult] = useState<TaskWizardResult | null>(null);

  // For the suggested group names only — the picked element's own step shows its whole path anyway.
  const { data: task } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId!, signal),
    enabled: taskId != null,
    staleTime: Infinity,
  });

  const anyGroup = GROUP_STEPS.some((step) => groups[step.key]);

  const execute = useMutation({
    mutationFn: () =>
      executeTaskWizard({
        taskId: taskId!,
        ...Object.fromEntries(
          GROUP_STEPS.map((step) => [step.field, groups[step.key]?.id ?? null])
        ),
      }),
    onSuccess: (granted) => setResult(granted),
    onError: (err: unknown) =>
      setError(err instanceof Error ? err.message : String(err)),
  });

  const setGroup = (key: WizardGroupKey, group: EntityRef | null) =>
    setGroups((previous) => ({ ...previous, [key]: group }));

  if (result) {
    return (
      <div className="flex max-w-3xl flex-col gap-4">
        <WizardResult
          result={result}
          onAgain={() => {
            setResult(null);
            setTaskId(null);
            setGroups({});
          }}
        />
      </div>
    );
  }

  return (
    <div className="flex max-w-3xl flex-col gap-4">
      {error && <FormAlert tone="error">{error}</FormAlert>}
      <p className="text-sm text-muted-foreground">{t("task.wizard.intro")}</p>
      <WizardTaskStep
        taskId={taskId}
        onChange={(id) => {
          setTaskId(id);
          setError(null);
        }}
        onLeave={() => stashWizardGroups(groups)}
      />
      {GROUP_STEPS.map((step, index) => (
        <WizardGroupStepCard
          key={step.key}
          step={step}
          number={index + 2}
          value={groups[step.key] ?? null}
          onChange={(group) => setGroup(step.key, group)}
          taskTitle={task?.title}
        />
      ))}
      <SectionCard className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold">{t("task.wizard.action._")}</h2>
        {/* Unlike Wicket, which announces the rights as soon as an element is picked although its own
            `noactionRequired` text says the opposite: without a group there is nothing to grant. */}
        <FormAlert tone={taskId != null && anyGroup ? "info" : "success"}>
          {taskId != null && anyGroup
            ? t("task.wizard.action.taskAndgroupsGiven")
            : t("task.wizard.action.noactionRequired")}
        </FormAlert>
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push(TASK_TREE_ROUTE)}
          >
            {t("cancel")}
          </Button>
          <Button
            type="button"
            disabled={taskId == null || execute.isPending}
            onClick={() => execute.mutate()}
            className="gap-1.5"
            aria-busy={execute.isPending}
          >
            {/* In place of the icon, not next to it, so the label doesn't move (see EntityEditActions).
                The grant itself is a handful of rows and thus quick, but it writes them one by one over
                the whole path up to the root. */}
            {execute.isPending ? (
              <Spinner className="h-3.5 w-3.5 border-2" />
            ) : (
              <HugeiconsIcon icon={Tick02Icon} size={14} />
            )}
            {t("task.wizard.finish")}
          </Button>
        </div>
      </SectionCard>
    </div>
  );
}
