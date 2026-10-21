"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { FormActionBar } from "@/components/shared/form-action-bar";
import { WizardFinishButton } from "@/components/features/task/wizard/wizard-finish-button";
import { WizardRunningOverlay } from "./wizard-running-overlay";

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
  /** What the overlay says while Finish runs. */
  runningLabel: string;
}

/**
 * The frame of the project wizard, that of the edit pages (EditPageShell): a tab per step, which
 * scrolls to it, the steps in one scroll column, and the pinned action bar with Cancel and Finish.
 * While Finish runs, an overlay with a moving bar covers it all (WizardRunningOverlay).
 */
export function ProjectWizardShell({
  steps,
  aboveSteps,
  isPending,
  onCancel,
  onFinish,
  finishLabel,
  runningLabel,
}: ProjectWizardShellProps) {
  const t = useTranslations();
  return (
    <div className="relative flex min-h-0 flex-1 flex-col">
      <EditPageShell
        header={null}
        tabs={steps.map(({ id, label }) => ({ id, label }))}
        sections={steps.map((step) => step.content)}
        aboveSections={aboveSteps}
        actions={
          <FormActionBar>
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={isPending}
            >
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
      {isPending && <WizardRunningOverlay label={runningLabel} />}
    </div>
  );
}
