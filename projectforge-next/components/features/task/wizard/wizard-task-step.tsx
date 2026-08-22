"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { TaskSelectControl } from "@/components/shared/tasks/task-select-control";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";
import {
  newTaskHref,
  TASK_WIZARD_ROUTE,
} from "@/components/shared/tasks/task-routes";
import { WizardStepCard } from "./wizard-step-card";

interface WizardTaskStepProps {
  taskId: number | null;
  onChange: (taskId: number | null) => void;
  /** Called before leaving for the task form, so what is picked survives the detour. */
  onLeave: () => void;
}

/**
 * Step 1: the structure element the rights are granted on — the one required value of the wizard.
 *
 * Built from [TaskSelectControl] and [TaskSelectModal] directly rather than from `TaskSelectField`:
 * the wizard is no entity form, so there is no field metadata and no form context for that field to
 * read (see the form rules in projectforge-next/CLAUDE.md).
 */
export function WizardTaskStep({
  taskId,
  onChange,
  onLeave,
}: WizardTaskStepProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  return (
    <WizardStepCard
      number={1}
      heading={t("task._")}
      intro={t("task.wizard.task.intro")}
    >
      <TaskSelectControl
        taskId={taskId}
        ariaLabel={t("task._")}
        onOpen={() => setOpen(true)}
        onSelect={(task) => onChange(task?.id ?? null)}
      />
      <TaskSelectModal
        value={taskId}
        onChange={(task) => onChange(task?.id ?? null)}
        open={open}
        onOpenChange={setOpen}
      />
      {/* Into this app, since the task pages are migrated — and back here with the id of what was
          saved (`?savedId=`, see useEditReturn), so the element just added is the one the wizard goes
          on with. The groups picked so far don't fit into a url and travel outside React instead (see
          wizard-handover.ts). */}
      <HintTooltip text={t("task.wizard.button.createTask.tooltip")}>
        <Link
          onClick={onLeave}
          href={newTaskHref({ returnTo: TASK_WIZARD_ROUTE })}
          className="flex w-fit items-center gap-1 text-xs text-primary hover:underline"
        >
          <HugeiconsIcon icon={PlusSignIcon} size={12} />
          {t("task.wizard.button.createTask._")}
        </Link>
      </HintTooltip>
    </WizardStepCard>
  );
}
