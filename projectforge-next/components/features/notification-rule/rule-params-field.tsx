"use client";

import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import type { NotificationRuleParam } from "@/lib/rs/notification";
import { cn } from "@/lib/utils";
import { useChosenRuleType } from "./use-rule-types";

/**
 * The settings of the chosen rule type, as its handler in the backend describes them
 * (NotificationRuleParam): the form values are `params.<key>`, checked by the backend.
 */
export function RuleParamsField({ className }: { className?: string }) {
  const params = useChosenRuleType()?.params ?? [];
  if (params.length === 0) return null;
  const checkboxes = params.filter((param) => param.type === "BOOLEAN");
  const others = params.filter((param) => param.type !== "BOOLEAN");
  return (
    <div className={cn("grid gap-4 sm:grid-cols-2", className)}>
      {others.map((param) => (
        <ParamField key={param.key} param={param} />
      ))}
      {checkboxes.length > 0 && (
        <div className="space-y-2">
          {checkboxes.map((param) => (
            <ParamField key={param.key} param={param} />
          ))}
        </div>
      )}
    </div>
  );
}

function ParamField({ param }: { param: NotificationRuleParam }) {
  const name = `params.${param.key}`;
  const hint = param.hint ?? undefined;
  switch (param.type) {
    case "INT":
      return (
        <NumberField
          name={name}
          label={param.label}
          hint={hint}
          metadataLess
          fractionDigits={0}
          maxDigits={param.max != null ? String(param.max).length : undefined}
        />
      );
    case "BOOLEAN":
      return <CheckboxField name={name} label={param.label} hint={hint} />;
    case "SELECT":
      return (
        <SelectField
          name={name}
          label={param.label}
          hint={hint}
          metadataLess
          clearable={false}
          options={param.options ?? []}
        />
      );
    case "ENTITY":
      return (
        <EntityAutocompleteField
          name={name}
          label={param.label}
          hint={hint}
          entity={param.entity ?? ""}
          metadataLess
        />
      );
  }
}
