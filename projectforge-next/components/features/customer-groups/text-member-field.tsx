"use client";

import { TagInput } from "@/components/shared/tag-input";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { TextMatches } from "./text-matches";

/**
 * The free-text members of a group or business unit: exact customer names or patterns (`ACME*`), for
 * the rows that name their customer as text instead of referring to an entity.
 *
 * A comma does not confirm an entry here, as it does in the shared [TagInput] by default: a customer's
 * name may contain one ("ACME, Inc.").
 */
export function TextMemberField({
  name,
  label,
  hint,
  className,
}: BaseFieldProps) {
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const value = (field.state.value as string[] | null) ?? [];
        return (
          <FieldShell
            name={name}
            label={label}
            hint={hint}
            invalid={meta.isTouched && !meta.isValid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <TagInput
              value={value}
              onChange={(next) => {
                field.handleChange(next);
                field.handleBlur();
              }}
              variant="neutral"
              commitOnComma={false}
              sorted
              inputAriaLabel={label}
            />
            <TextMatches texts={value} />
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
