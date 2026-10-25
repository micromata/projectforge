"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { InformationCircleIcon } from "@hugeicons/core-free-icons";
import { Label } from "@/components/ui/label";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { WizardStepCard } from "@/components/features/task/wizard/wizard-step-card";
import type { ValidationError } from "@/lib/rs/types";
import type { ElementChoice } from "./use-wizard-elements";
import { WizardElementPick } from "./wizard-element-pick";
import { WizardStructurePath } from "./wizard-structure-path";
import { WizardTaskPicker } from "./wizard-task-picker";

interface WizardParentTaskStepProps {
  number: number;
  /** Whether the customer is created with the project, which decides what is picked here. */
  newCustomer: boolean;
  /** A new customer without project: the element is optional, without it only the customer is created. */
  customerOnly: boolean;
  /** Existing customer: its element; new customer: the parent of its new element. */
  parentTaskId: number | null;
  onChange: (taskId: number | null) => void;
  /** The parent the request takes: none while an existing element of a new customer is chosen. */
  pathParentTaskId: number | null;
  /** New customer only: a new element or an existing one. */
  customerElement: ElementChoice;
  onCustomerElementChange: (choice: ElementChoice) => void;
  projectElement: ElementChoice;
  onProjectElementChange: (choice: ElementChoice) => void;
  /** The existing elements the server uses (`ProjectWizardRest.ValidateResponse`), picked or found by title. */
  reusedCustomerTaskId: number | null;
  reusedProjectTaskId: number | null;
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
 * the elements of all its projects (see ProjectWizardService):
 *
 * - an existing customer: its element, preset from its projects where they tell;
 * - a new customer: a new element below a picked one (a customer group, a business unit or the top level),
 *   or an existing element;
 * - the project: a new element below the customer's, or an existing one below it.
 *
 * A new customer only, without project: optionally its element.
 */
export function WizardParentTaskStep({
  number,
  newCustomer,
  customerOnly,
  parentTaskId,
  onChange,
  pathParentTaskId,
  customerElement,
  onCustomerElementChange,
  projectElement,
  onProjectElementChange,
  reusedCustomerTaskId,
  reusedProjectTaskId,
  customerName,
  projectName,
  rootTaskId,
  errors,
}: WizardParentTaskStepProps) {
  const t = useTranslations();
  const parentLabel = t("project.wizard.parentTask.newCustomer");
  return (
    <WizardStepCard
      number={number}
      heading={t("project.wizard.parentTask.heading")}
      intro={
        customerOnly
          ? t("project.wizard.parentTask.customerOnly.intro")
          : t("project.wizard.parentTask.intro")
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
      {newCustomer ? (
        <WizardElementPick
          id="wizard-customer-element"
          label={t("project.wizard.result.customerElement")}
          intro={t("project.wizard.element.customerIntro")}
          mode={customerElement.mode}
          onModeChange={(mode) =>
            onCustomerElementChange({ ...customerElement, mode })
          }
          taskId={customerElement.taskId}
          onTaskChange={(taskId) =>
            onCustomerElementChange({ ...customerElement, taskId })
          }
          reusedTaskId={reusedCustomerTaskId}
          fieldId="customerTaskId"
          errors={errors}
        >
          <Label className="text-xs">{parentLabel}</Label>
          <WizardTaskPicker
            taskId={parentTaskId}
            onChange={onChange}
            ariaLabel={parentLabel}
            errors={errors}
            fieldId="parentTaskId"
          />
        </WizardElementPick>
      ) : (
        <div className="flex flex-col gap-2">
          <h3 className="text-sm font-medium">
            {t("project.wizard.parentTask.existingCustomer._")}
          </h3>
          <p className="text-xs text-muted-foreground">
            {t("project.wizard.parentTask.existingCustomer.intro")}
          </p>
          <WizardTaskPicker
            taskId={parentTaskId}
            onChange={onChange}
            ariaLabel={t("project.wizard.parentTask.existingCustomer._")}
            errors={errors}
            fieldId="parentTaskId"
          />
        </div>
      )}
      {!customerOnly && (
        <WizardElementPick
          id="wizard-project-element"
          label={t("project.wizard.result.projectElement")}
          intro={t("project.wizard.element.projectIntro")}
          mode={projectElement.mode}
          onModeChange={(mode) =>
            onProjectElementChange({ ...projectElement, mode })
          }
          taskId={projectElement.taskId}
          onTaskChange={(taskId) =>
            onProjectElementChange({ ...projectElement, taskId })
          }
          reusedTaskId={reusedProjectTaskId}
          fieldId="projectTaskId"
          errors={errors}
        />
      )}
      <WizardStructurePath
        parentTaskId={pathParentTaskId}
        newCustomer={newCustomer}
        customerOnly={customerOnly}
        customerName={customerName}
        projectName={projectName}
        reusedCustomerTaskId={reusedCustomerTaskId}
        reusedProjectTaskId={reusedProjectTaskId}
        rootTaskId={rootTaskId}
      />
    </WizardStepCard>
  );
}
