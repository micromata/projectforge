"use client";

import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { DateInput } from "@/components/shared/date-input";
import { ValueCombobox } from "@/components/shared/value-combobox";
import type {
  MassUpdateFieldMeta,
  MassUpdateParameter,
} from "@/lib/rs/multi-select";

/** The input the value is typed into — which one is decided by the field's data type. */
export function MassUpdateValueControl({
  meta,
  param,
  disabled,
  label,
  onChange,
}: {
  meta: MassUpdateFieldMeta;
  param: MassUpdateParameter;
  disabled: boolean;
  label: string;
  onChange: (values: Partial<MassUpdateParameter>) => void;
}) {
  // The property the value goes into is the backend's answer, never derived here (see
  // MassUpdateFieldMeta.valueProperty).
  const value = param[meta.valueProperty as keyof MassUpdateParameter];
  const setValue = (next: unknown) =>
    onChange({ [meta.valueProperty]: next } as Partial<MassUpdateParameter>);

  if (meta.values) {
    return (
      <ValueCombobox
        options={meta.values.map((option) => ({
          value: String(option.id),
          label: option.displayName,
        }))}
        selected={value == null || value === "" ? [] : [String(value)]}
        onChange={(values) => setValue(values[0] ?? undefined)}
        aria-label={label}
      />
    );
  }
  if (meta.dataType === "DATE") {
    return (
      <DateInput
        value={typeof value === "string" ? value : null}
        disabled={disabled}
        aria-label={label}
        onChange={(next) => setValue(next ?? undefined)}
      />
    );
  }
  const shared = {
    value: value == null ? "" : String(value),
    disabled,
    maxLength: meta.maxLength,
    "aria-label": label,
    onChange: (event: { target: { value: string } }) =>
      setValue(event.target.value || undefined),
  };
  return meta.rows ? (
    <Textarea rows={meta.rows} {...shared} />
  ) : (
    <Input {...shared} />
  );
}
