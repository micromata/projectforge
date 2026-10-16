"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
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
import { fetchAddressLanguages } from "@/lib/rs/address";

const LANGUAGES_QUERY_KEY = ["address", "languages"] as const;

/**
 * The language to write to the person in, a locale (`de_DE`). Offered first are the languages already used in
 * the address book, as the former form offered them for a quick pick, then every other one; the list is
 * searchable.
 */
export function AddressLanguageField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const label = t("address.communicationLanguage");
  const { data } = useQuery({
    queryKey: LANGUAGES_QUERY_KEY,
    queryFn: ({ signal }) => fetchAddressLanguages(signal),
    staleTime: Infinity,
  });
  const options = useMemo<ValueOption[]>(() => {
    if (!data) return [];
    const used = new Set(data.used.map((lang) => lang.value));
    return [...data.used, ...data.all.filter((lang) => !used.has(lang.value))];
  }, [data]);

  return (
    <form.Field name={"communicationLanguage" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const value = field.state.value as string | null;
        return (
          <FieldShell
            name="communicationLanguage"
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
