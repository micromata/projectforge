"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ValueCombobox } from "@/components/shared/value-combobox";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { fetchMenuBadgeOptions } from "@/lib/rs/notification";

const NAME = "menuBadge";

/**
 * The menu entry counting the open notifications of the rule, chosen from the entries of the menu.
 *
 * A searchable combobox rather than a plain select: the options are the whole menu, far too many to
 * scroll through, so typing a few letters narrows them down.
 */
export function MenuBadgeField({ className }: { className?: string }) {
  const t = useTranslations("notification.rule");
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { data } = useQuery({
    queryKey: ["notificationRule", "menuBadges"],
    queryFn: ({ signal }) => fetchMenuBadgeOptions(signal),
    staleTime: Infinity,
  });
  const options = (data ?? []).map((option) => ({
    value: option.id,
    label: option.label,
  }));
  const label = t("menuBadge");
  return (
    <form.Field name={NAME as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const value = field.state.value as string | null;
        return (
          <FieldShell
            name={NAME}
            label={label}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <ValueCombobox
              id={ids.controlId}
              aria-label={label}
              clearable
              options={options}
              selected={value ? [value] : []}
              onChange={(next) => {
                field.handleChange(next[0] ?? null);
                field.handleBlur();
              }}
            />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
