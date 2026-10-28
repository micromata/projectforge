"use client";

import { useQuery } from "@tanstack/react-query";
import {
  ValueCombobox,
  type ValueOption,
} from "@/components/shared/value-combobox";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { CONTRACT_METADATA } from "@/lib/metadata/contract.generated";
import {
  CONTRACT_TYPES_QUERY_KEY,
  fetchContractTypes,
  type ContractTypeOption,
} from "@/lib/rs/contract";

/** The configured contract types; they change with the configuration only. */
export function useContractTypes(): ContractTypeOption[] {
  const { data } = useQuery({
    queryKey: CONTRACT_TYPES_QUERY_KEY,
    queryFn: ({ signal }) => fetchContractTypes(signal),
    staleTime: Infinity,
  });
  return data ?? [];
}

/**
 * The type of a contract, one of the configuration parameter `orga.contractTypes` (`/rs/contract/types`). A value
 * stored before its type was removed from the configuration is shown as it is (see ValueCombobox).
 */
export function ContractTypeField({ className }: { className?: string }) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const label = useFieldLabels(CONTRACT_METADATA)("type");
  const options: ValueOption[] = useContractTypes();

  return (
    <form.Field name={"type" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const value = field.state.value as string | null;
        return (
          <FieldShell
            name="type"
            label={label}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <ValueCombobox
              id={ids.controlId}
              aria-label={label}
              options={options}
              selected={value ? [value] : []}
              clearable
              onChange={(values) => {
                field.handleChange(values[0] ?? null);
                field.handleBlur();
              }}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
