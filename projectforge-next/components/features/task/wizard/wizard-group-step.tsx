"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { GROUP_PAGE } from "@/components/features/group/group.page";
import { EntityEditModal } from "@/components/shared/edit/entity-edit-modal";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { GroupNomenclatureHint } from "@/components/shared/group-nomenclature-hint";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useGroupNomenclature } from "@/hooks/use-group-nomenclature";
import {
  groupBaseName,
  suggestedGroupName,
  type GroupNomenclatureConfig,
} from "@/lib/group-nomenclature";
import type { TaskNode } from "@/lib/rs/task";
import { Label } from "@/components/ui/label";
import type { WizardGroupStep } from "./types";
import { WizardStepCard } from "./wizard-step-card";

interface WizardGroupStepProps {
  step: WizardGroupStep;
  /** Its position in the wizard — the task is 1, the three groups follow. */
  number: number;
  value: EntityRef | null;
  onChange: (group: EntityRef | null) => void;
  /**
   * The base of the suggested group name: the project's names following the configured nomenclature,
   * or the title of the picked structure element (see groupBaseNameOfTask).
   */
  baseName?: string | null;
  /** The configured description of the group naming convention, shown with the create link. */
  nomenclatureDescription?: string | null;
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
  baseName,
  nomenclatureDescription,
}: WizardGroupStepProps) {
  const t = useTranslations();
  const [creating, setCreating] = useState(false);
  const id = `wizard-${step.key}`;
  const heading = t(`task.wizard.${step.key}._`);
  const nomenclature = useGroupNomenclature();
  const suggestedName = suggestedGroupName(
    nomenclature,
    step.key,
    baseName,
    defaultGroupNameSuffix(step, t)
  );

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
          the step below, and leaving the wizard would throw away the choices already made. The
          group's own page definition is what the dialog renders — the one place this feature reaches
          into another one, because "create a group here" *is* a reference to the group page, and
          rebuilding its form would be a second form to keep in step with GroupEntityRest. */}
      <div className="flex items-center gap-1">
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
        <GroupNomenclatureHint
          description={nomenclatureDescription}
          label={`${t("group.nomenclature._")}: ${heading}`}
        />
      </div>
      <EntityEditModal
        page={GROUP_PAGE}
        id={null}
        open={creating}
        onOpenChange={setCreating}
        // Wicket's suggestion, and now an actual prefill rather than a name to copy by hand.
        prefill={suggestedName ? { name: suggestedName } : undefined}
        // The new group is what this step goes on with — the point of creating it here. A group
        // without an id cannot happen (a save answers with one), and nothing is picked if it does.
        onSaved={(id, values) =>
          id == null
            ? undefined
            : onChange({
                id,
                displayName: (values as { name?: string }).name ?? "",
              })
        }
      />
    </WizardStepCard>
  );
}

/**
 * The suffix a role's group got in Wicket, the translated `groupNameSuffix` — the team has none. A
 * configured nomenclature replaces it (see suggestedGroupName).
 */
export function defaultGroupNameSuffix(
  step: WizardGroupStep,
  t: (key: string) => string
): string | null {
  return step.hasNameSuffix
    ? t(`task.wizard.${step.key}.groupNameSuffix`)
    : null;
}

/**
 * The base of the group names for a picked structure element: with a configured pattern and a project
 * the element belongs to (its own or an ancestor's), the project's names following the pattern — the
 * groups of a project are named the same whichever wizard creates them —, otherwise the element's
 * title, as Wicket did.
 */
export function groupBaseNameOfTask(
  config: GroupNomenclatureConfig | null | undefined,
  task: Pick<TaskNode, "title" | "projekt"> | null | undefined
): string | null {
  if (!task) return null;
  const projekt = task.projekt;
  if (config?.pattern?.trim() && projekt) {
    const name = groupBaseName(config, {
      customer: projekt.customer,
      project: projekt,
    });
    if (name) return name;
  }
  return task.title?.trim() || null;
}
