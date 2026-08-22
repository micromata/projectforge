"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { DynamicFormDialog } from "@/components/dynamic/dynamic-form-dialog";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Label } from "@/components/ui/label";
import type { WizardGroupStep } from "./types";
import { WizardStepCard } from "./wizard-step-card";

/** REST category of the group page, whose form the dialog renders. */
const GROUP_CATEGORY = "group";

interface WizardGroupStepProps {
  step: WizardGroupStep;
  /** Its position in the wizard — the task is 1, the three groups follow. */
  number: number;
  value: EntityRef | null;
  onChange: (group: EntityRef | null) => void;
  /** Title of the picked structure element, from which the suggested group name is built. */
  taskTitle?: string | null;
}

/**
 * One of the steps 2 to 4: picks the group that is given this role on the structure element.
 *
 * All three are the same step with another key, so the heading and the intro come from
 * `task.wizard.<key>` — see [WizardGroupStep].
 */
export function WizardGroupStepCard({
  step,
  number,
  value,
  onChange,
  taskTitle,
}: WizardGroupStepProps) {
  const t = useTranslations();
  const [creating, setCreating] = useState(false);
  const id = `wizard-${step.key}`;
  const heading = t(`task.wizard.${step.key}._`);
  const suggestedName = suggestGroupName(step, taskTitle, t);

  return (
    <WizardStepCard
      number={number}
      heading={heading}
      intro={t(`task.wizard.${step.key}.intro`)}
    >
      <div className="flex flex-col gap-1.5">
        <Label htmlFor={id} className="text-xs">
          {t("group._")}
        </Label>
        <EntityAutocomplete
          id={id}
          url="group/autosearch?search=:search"
          value={value}
          onChange={onChange}
          // The visible label is the bare „Group", as in Wicket, where the heading above it names the
          // role. Three of them stand on this page, so the accessible name has to add that role — a
          // „Group" that could be any of the three names none of them.
          aria-label={`${t("group._")}: ${heading}`}
          className="max-w-md"
        />
      </div>
      {/* In a dialog on this page, not on the group's own page: whatever is created has to end up in
          the step below, and leaving the wizard would throw away the choices already made. */}
      <HintTooltip text={t("task.wizard.button.createGroup.tooltip")}>
        <button
          type="button"
          onClick={() => setCreating(true)}
          className="flex w-fit items-center gap-1 text-xs text-primary hover:underline"
        >
          <HugeiconsIcon icon={PlusSignIcon} size={12} />
          {t("task.wizard.button.createGroup._")}
        </button>
      </HintTooltip>
      <DynamicFormDialog
        category={GROUP_CATEGORY}
        open={creating}
        onOpenChange={setCreating}
        // Wicket's suggestion, and now an actual prefill rather than a name to copy by hand.
        prefill={suggestedName ? { name: suggestedName } : undefined}
        // The new group is what this step goes on with — the point of creating it here.
        onSaved={(id, data) =>
          onChange({ id, displayName: String(data.name ?? "") })
        }
      />
    </WizardStepCard>
  );
}

/** The name Wicket suggested: the element's title, plus the role's suffix for all but the team. */
function suggestGroupName(
  step: WizardGroupStep,
  taskTitle: string | null | undefined,
  t: (key: string) => string
): string | null {
  if (!taskTitle) return null;
  if (!step.hasNameSuffix) return taskTitle;
  return `${taskTitle}-${t(`task.wizard.${step.key}.groupNameSuffix`)}`;
}
