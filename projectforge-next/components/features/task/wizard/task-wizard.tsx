"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import {
  TASK_TREE_ROUTE,
  HIGHLIGHT_ID_PARAM,
  WIZARD_TASK_PARAM,
} from "@/components/shared/tasks/task-routes";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import {
  executeTaskWizard,
  fetchTaskInfo,
  previewTaskWizard,
  type TaskWizardRequest,
  type TaskWizardResult,
} from "@/lib/rs/task";
import { GROUP_STEPS, type WizardGroupKey, type WizardGroups } from "./types";
import { stashWizardGroups, takeWizardGroups } from "./wizard-handover";
import { WizardActionStep } from "./wizard-action-step";
import { WizardGroupStepCard } from "./wizard-group-step";
import { WizardPreview } from "./wizard-preview";
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
  // url (`?highlightId=`, see useEditReturn) — and the groups that were picked before that detour,
  // which no url could carry (see wizard-handover.ts).
  const params = useSearchParams();
  const savedId = Number(params.get(HIGHLIGHT_ID_PARAM));
  const returning = savedId > 0;
  // The element a caller opened the wizard on: the task's own form, for the element on screen (see
  // TASK_PAGE.crossLinks). Only the first step is preset, the groups are picked here as always —
  // whereas a return brings those along, which is why the two parameters are not one.
  const initialTaskId = returning
    ? savedId
    : Number(params.get(WIZARD_TASK_PARAM));
  const [taskId, setTaskId] = useState<number | null>(
    initialTaskId > 0 ? initialTaskId : null
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

  const request: TaskWizardRequest = {
    taskId: taskId!,
    ...Object.fromEntries(
      GROUP_STEPS.map((step) => [step.field, groups[step.key]?.id ?? null])
    ),
  };

  // What the same request would do, asked again with every pick — the table below the steps. Only with a
  // group, because without one there is nothing to grant and nothing to show (see the action card).
  const preview = useQuery({
    queryKey: ["taskWizardPreview", request],
    queryFn: ({ signal }) => previewTaskWizard(request, signal),
    enabled: taskId != null && anyGroup,
    staleTime: Infinity,
  });

  const execute = useMutation({
    mutationFn: () => executeTaskWizard(request),
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
    <div className="flex flex-col gap-4">
      <div className="flex max-w-3xl flex-col gap-4">
        {error && <FormAlert tone="error">{error}</FormAlert>}
        <p className="text-sm text-muted-foreground">
          {t("task.wizard.intro")}
        </p>
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
      </div>
      {/* Above the buttons, so what the wizard would do is read before it is set off - and wider than
          the steps, because the table holds a column per group while the steps read best narrow. */}
      {preview.data && (
        <div className="flex max-w-5xl flex-col">
          <WizardPreview
            preview={preview.data}
            isFetching={preview.isFetching}
          />
        </div>
      )}
      <div className="flex max-w-3xl flex-col">
        <WizardActionStep
          hasAction={taskId != null && anyGroup}
          canFinish={taskId != null}
          isPending={execute.isPending}
          onCancel={() => router.push(TASK_TREE_ROUTE)}
          onFinish={() => execute.mutate()}
        />
      </div>
    </div>
  );
}
