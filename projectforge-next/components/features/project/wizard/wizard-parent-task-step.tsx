"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { InformationCircleIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { TaskSelectControl } from "@/components/shared/tasks/task-select-control";
import { TaskSelectModal } from "@/components/shared/tasks/task-select-modal";
import { WizardStepCard } from "@/components/features/task/wizard/wizard-step-card";
import type { ValidationError } from "@/lib/rs/types";
import { WizardFieldError } from "./wizard-field-error";
import { WizardStructurePath } from "./wizard-structure-path";

interface WizardParentTaskStepProps {
  number: number;
  /** Whether the customer is created with the project, which decides what is picked here. */
  newCustomer: boolean;
  /** A new customer without project: the element is optional, without it only the customer is created. */
  customerOnly: boolean;
  parentTaskId: number | null;
  onChange: (taskId: number | null) => void;
  /** The title of the customer's new element, the customer's name. */
  customerName: string;
  /** The title of the project's new element, the project's name. */
  projectName: string;
  /** The root of the tree, which the path leaves out as the tree's breadcrumbs do. */
  rootTaskId?: number | null;
  errors: ValidationError[];
}

/**
 * Step 3: where the project goes in the structure. A customer has an element of its own, the parent of
 * the elements of all its projects (see ProjectWizardService), so what is picked here depends on the
 * customer:
 *
 * - an existing one: its element, preset from its projects where they tell — the project's element is
 *   created below it;
 * - a new one: the element its own element is created below (a customer group, a business unit or the
 *   top level), and the project's below that.
 *
 * - a new customer only, without project: optionally the element its own element is created below.
 *
 * Built like the first step of the structure wizard (WizardTaskStep), from the select control and
 * dialog directly, as this is no entity form.
 */
export function WizardParentTaskStep({
  number,
  newCustomer,
  customerOnly,
  parentTaskId,
  onChange,
  customerName,
  projectName,
  rootTaskId,
  errors,
}: WizardParentTaskStepProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const heading = newCustomer
    ? t("project.wizard.parentTask.newCustomer._")
    : t("project.wizard.parentTask.existingCustomer._");
  return (
    <WizardStepCard
      number={number}
      heading={heading}
      intro={
        customerOnly
          ? t("project.wizard.parentTask.customerOnly.intro")
          : newCustomer
            ? t("project.wizard.parentTask.newCustomer.intro")
            : t("project.wizard.parentTask.existingCustomer.intro")
      }
    >
      <HintTooltip
        title={t("project.wizard.parentTask.structure.title")}
        text={t("project.wizard.parentTask.structure._")}
        wide
        openOnTap
      >
        <span className="flex w-fit items-center gap-1 text-xs text-primary">
          <HugeiconsIcon icon={InformationCircleIcon} size={14} aria-hidden />
          {t("project.wizard.parentTask.structure.title")}
        </span>
      </HintTooltip>
      <TaskSelectControl
        taskId={parentTaskId}
        ariaLabel={heading}
        onOpen={() => setOpen(true)}
        onSelect={(task) => onChange(task?.id ?? null)}
      />
      <TaskSelectModal
        value={parentTaskId}
        onChange={(task) => onChange(task?.id ?? null)}
        open={open}
        onOpenChange={setOpen}
      />
      <WizardFieldError errors={errors} fieldId="parentTaskId" />
      <WizardStructurePath
        parentTaskId={parentTaskId}
        newCustomer={newCustomer}
        customerOnly={customerOnly}
        customerName={customerName}
        projectName={projectName}
        rootTaskId={rootTaskId}
      />
    </WizardStepCard>
  );
}
