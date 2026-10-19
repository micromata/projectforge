"use client";

import { useTranslations } from "next-intl";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import type { WizardGroupStep } from "@/components/features/task/wizard/types";
import { WizardStepCard } from "@/components/features/task/wizard/wizard-step-card";
import type { ValidationError } from "@/lib/rs/types";
import { ROLE_FIELDS, type RoleMode, type RoleState } from "./types";
import { WizardFieldError } from "./wizard-field-error";
import { WizardModeRadio } from "./wizard-mode-radio";

interface WizardRoleStepProps {
  step: WizardGroupStep;
  number: number;
  value: RoleState;
  onChange: (value: RoleState) => void;
  /** The name a new group gets unless one is typed, made of the project's name. */
  suggestedName: string | null;
  errors: ValidationError[];
}

/**
 * One of the steps 4 to 6: the group that is given this role on the new element — none, an existing
 * one, or a new one with its members.
 *
 * A new group is created by the wizard itself rather than in the group dialog the structure wizard
 * offers: its users may create groups only here, as an exception (see ProjectWizardService), and the
 * group must not exist before the project does, so a failing project leaves no group behind.
 */
export function WizardRoleStep({
  step,
  number,
  value,
  onChange,
  suggestedName,
  errors,
}: WizardRoleStepProps) {
  const t = useTranslations();
  const me = useCurrentUserRef();
  const id = `project-wizard-${step.key}`;
  const heading = t(`task.wizard.${step.key}._`);
  const field = ROLE_FIELDS[step.key];
  const set = (patch: Partial<RoleState>) => onChange({ ...value, ...patch });
  return (
    <WizardStepCard
      number={number}
      heading={heading}
      intro={t(`task.wizard.${step.key}.intro`)}
    >
      <WizardModeRadio<RoleMode>
        id={`${id}-mode`}
        value={value.mode}
        onChange={(mode) => set({ mode })}
        ariaLabel={heading}
        options={[
          { value: "new", label: t("project.wizard.new.group") },
          { value: "existing", label: t("project.wizard.existing.group") },
          { value: "none", label: t("project.wizard.none.group") },
        ]}
      />
      {value.mode === "existing" && (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor={id} className="text-xs">
            {t("group._")}
          </Label>
          <EntityAutocomplete
            id={id}
            url="group/autosearch?search=:search"
            value={value.group}
            onChange={(group: EntityRef | null) => set({ group })}
            aria-label={`${t("group._")}: ${heading}`}
            className="max-w-md"
          />
        </div>
      )}
      {value.mode === "new" && (
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={`${id}-name`} className="text-xs">
              {t("name")}
            </Label>
            <Input
              id={`${id}-name`}
              value={value.name ?? suggestedName ?? ""}
              onChange={(event) => set({ name: event.target.value })}
              aria-label={`${t("name")}: ${heading}`}
              className="max-w-md"
            />
            <WizardFieldError errors={errors} fieldId={`${field}.name`} />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor={`${id}-members`} className="text-xs">
              {t("group.assignedUsers")}
            </Label>
            <EntityMultiAutocomplete
              id={`${id}-members`}
              url="user/autosearch?search=:search"
              value={value.members}
              onChange={(members) => set({ members })}
              removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
              aria-label={`${t("group.assignedUsers")}: ${heading}`}
              sorted
              selectMe={me}
              className="max-w-md"
            />
          </div>
        </div>
      )}
      {step.key === "managerGroup" && value.mode !== "none" && (
        <p className="text-xs text-muted-foreground">
          {t("project.wizard.managerGroup.projectManagerGroup")}
        </p>
      )}
    </WizardStepCard>
  );
}
