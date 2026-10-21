"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import type { MerlinInputVariable } from "@/lib/rs/merlin";
import { inputFieldName } from "./merlin-execute-metadata";

/**
 * The input of one variable, by its type: a select of its allowed values, a date, a number (an INT without
 * fraction digits, a FLOAT with the variable's scale) or a text — labelled with the variable's name and
 * explained by its description.
 */
function MerlinVariableInput({ variable }: { variable: MerlinInputVariable }) {
  const common = {
    name: inputFieldName(variable),
    label: variable.name,
    hint: variable.description ?? undefined,
  };
  if (variable.allowedValues?.length) {
    return (
      <SelectField
        {...common}
        options={variable.allowedValues.map((value) => ({
          value,
          label: value,
        }))}
        clearable={!variable.required}
      />
    );
  }
  switch (variable.type) {
    case "DATE":
      return <InputField {...common} type="date" />;
    case "INT":
      return <NumberField {...common} fractionDigits={0} />;
    case "FLOAT":
      return (
        <NumberField {...common} fractionDigits={variable.scale ?? undefined} />
      );
    default:
      return <InputField {...common} />;
  }
}

/** The input variables of the template, in two columns, and whether the document is wanted as PDF. */
export function MerlinExecuteInputs({
  variables,
}: {
  variables: readonly MerlinInputVariable[];
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        {variables.map((variable) => (
          <MerlinVariableInput key={variable.name} variable={variable} />
        ))}
      </div>
      <CheckboxField
        name="pdfFormat"
        label={t("plugins.merlin.format.pdf._")}
        hint={t("plugins.merlin.format.pdf.info")}
      />
    </div>
  );
}
