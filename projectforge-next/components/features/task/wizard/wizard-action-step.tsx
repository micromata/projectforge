"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { WizardFinishButton } from "./wizard-finish-button";

export interface WizardActionStepProps {
  /** Whether an element is picked and at least one group with it, i.e. whether there is anything to do. */
  hasAction: boolean;
  /** The finish button is offered as soon as an element is picked, as in Wicket. */
  canFinish: boolean;
  isPending: boolean;
  onCancel: () => void;
  onFinish: () => void;
  /** The text of the finish button, the structure wizard's by default. */
  finishLabel?: string;
}

/** The last card of the wizard: what it is about to do, and the two buttons that decide it. */
export function WizardActionStep({
  hasAction,
  canFinish,
  isPending,
  onCancel,
  onFinish,
  finishLabel,
}: WizardActionStepProps) {
  const t = useTranslations();
  return (
    <SectionCard className="flex flex-col gap-3">
      <h2 className="text-sm font-semibold">{t("task.wizard.action._")}</h2>
      {/* Unlike Wicket, which announces the rights as soon as an element is picked although its own
          `noactionRequired` text says the opposite: without a group there is nothing to grant. */}
      <FormAlert tone={hasAction ? "info" : "success"}>
        {hasAction
          ? t("task.wizard.action.taskAndgroupsGiven")
          : t("task.wizard.action.noactionRequired")}
      </FormAlert>
      <div className="flex items-center gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          {t("cancel")}
        </Button>
        <WizardFinishButton
          disabled={!canFinish}
          isPending={isPending}
          onFinish={onFinish}
          label={finishLabel}
        />
      </div>
    </SectionCard>
  );
}
