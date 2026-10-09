"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { DatePeriodField } from "@/components/shared/form/date-period-field";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { TaskSelectField } from "@/components/shared/tasks/task-select-field";
import type { ScriptParam, ScriptParameterKey } from "@/lib/rs/script";

/**
 * The input of one parameter of the execution, by its type, bound to the property of `Script.Param`
 * that type uses, labelled with the parameter's name and explained by [hint] (its description, see
 * ScriptExecuteParameters).
 */
export function ScriptParameterInput({
  paramKey,
  param,
  hint,
}: {
  paramKey: ScriptParameterKey;
  param: ScriptParam;
  hint?: ReactNode;
}) {
  const t = useTranslations();
  const label = param.name ?? "";
  const common = { label, hint };
  const name = (property: keyof ScriptParam) => `${paramKey}.${property}`;
  switch (param.type) {
    case "STRING":
      return <InputField name={name("stringValue")} {...common} />;
    case "INTEGER":
      return (
        <NumberField name={name("intValue")} {...common} fractionDigits={0} />
      );
    case "DECIMAL":
      return <NumberField name={name("decimalValue")} {...common} />;
    case "BOOLEAN":
      return <CheckboxField name={name("booleanValue")} {...common} />;
    case "DATE":
      return <InputField name={name("dateValue")} {...common} type="date" />;
    case "TIME_PERIOD":
      return (
        <DatePeriodField
          {...common}
          begin={{ name: name("dateValue"), label: t("date.from") }}
          end={{ name: name("toDateValue"), label: t("until") }}
          paging
        />
      );
    case "TASK":
      return <TaskSelectField name={name("taskValue")} {...common} />;
    case "USER":
      return (
        <EntityAutocompleteField
          name={name("userValue")}
          {...common}
          entity="user"
        />
      );
    default:
      return null;
  }
}
