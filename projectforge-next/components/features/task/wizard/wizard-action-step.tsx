"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { Spinner } from "@/components/shared/spinner";

export interface WizardActionStepProps {
  /** Whether an element is picked and at least one group with it, i.e. whether there is anything to do. */
  hasAction: boolean;
  /** The finish button is offered as soon as an element is picked, as in Wicket. */
  canFinish: boolean;
  isPending: boolean;
  onCancel: () => void;
  onFinish: () => void;
}

/** The last card of the wizard: what it is about to do, and the two buttons that decide it. */
export function WizardActionStep({
  hasAction,
  canFinish,
  isPending,
  onCancel,
  onFinish,
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
        <Button
          type="button"
          disabled={!canFinish || isPending}
          onClick={onFinish}
          className="gap-1.5"
          aria-busy={isPending}
        >
          {/* In place of the icon, not next to it, so the label doesn't move (see EntityEditActions).
              The grant itself is a handful of rows and thus quick, but it writes them one by one over
              the whole path up to the root. */}
          {isPending ? (
            <Spinner className="h-3.5 w-3.5 border-2" />
          ) : (
            <HugeiconsIcon icon={Tick02Icon} size={14} />
          )}
          {t("task.wizard.finish")}
        </Button>
      </div>
    </SectionCard>
  );
}
