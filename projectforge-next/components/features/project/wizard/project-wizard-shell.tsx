"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { WizardFinishButton } from "@/components/features/task/wizard/wizard-finish-button";

export interface ProjectWizardStep {
  id: string;
  /** The tab's label: the step's heading, without its number. */
  label: string;
  content: ReactNode;
}

interface ProjectWizardShellProps {
  steps: ProjectWizardStep[];
  /** Above the first step: the errors of a failed finish and the wizard's intro. */
  aboveSteps?: ReactNode;
  isPending: boolean;
  onCancel: () => void;
  onFinish: () => void;
  finishLabel: string;
}

/**
 * The frame of the project wizard, that of the edit pages (EditPageShell): a tab per step, which
 * scrolls to it, the steps in one scroll column, and the pinned action bar with Cancel and Finish.
 */
export function ProjectWizardShell({
  steps,
  aboveSteps,
  isPending,
  onCancel,
  onFinish,
  finishLabel,
}: ProjectWizardShellProps) {
  const t = useTranslations();
  return (
    <EditPageShell
      header={null}
      tabs={steps.map(({ id, label }) => ({ id, label }))}
      sections={steps.map((step) => step.content)}
      aboveSections={aboveSteps}
      actions={
        <FormActionBar>
          <Button type="button" variant="outline" onClick={onCancel}>
            {t("cancel")}
          </Button>
          {/* Never disabled: what is missing is told in the action step and at its field. */}
          <WizardFinishButton
            isPending={isPending}
            onFinish={onFinish}
            label={finishLabel}
          />
        </FormActionBar>
      }
    />
  );
}
