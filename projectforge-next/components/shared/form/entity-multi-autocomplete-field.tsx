"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { cn } from "@/lib/utils";
import {
  FieldShell,
  useFieldIds,
  type BaseFieldProps,
  type FieldMetaState,
} from "./field-shell";
import { useEntityEditForm, useFieldMetadata } from "./form-context";
import { useFieldErrors } from "./use-field-errors";

export interface EntityMultiAutocompleteFieldProps extends BaseFieldProps {
  /**
   * REST category to search in — `user`, `group`, `employee`. As in [EntityAutocompleteField] the
   * lookup url is that category's `autosearch`, built here rather than declared.
   */
  entity: string;
  /** Characters before the lookup fires; the backend defaults it to 2. */
  minChars?: number;
  /** Further request parameters of that search, see [EntityAutocomplete]. */
  params?: Record<string, unknown>;
  /**
   * The entity has no metadata for this field, and cannot have any: a collection is no
   * `@PropertyInfo` field of the DO, so a group's `assignedUsers` is absent from the generated
   * metadata however it is annotated (see [useFieldMetadata]).
   */
  metadataLess?: boolean;
}

/**
 * Picks any number of referenced entities — the members of a group, bound to a form value holding the
 * references themselves (`[{id, displayName}, …]`, what the DTO carries and what `copyTo` resolves back
 * by id).
 *
 * The picker is the shared [EntityAutocomplete] with no value of its own: picking means adding, so its
 * button always reads "choose", and what has been chosen stands below it as chips. That is the shape of
 * the legacy multi select too (`UISelect` with `multi = true`), minus its two-list transfer panel — a
 * search plus a list of what is picked says the same thing in one control.
 */
export function EntityMultiAutocompleteField({
  name,
  label,
  hint,
  className,
  entity,
  minChars,
  params,
  metadataLess,
}: EntityMultiAutocompleteFieldProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { required } = useFieldMetadata(name, metadataLess);
  return (
    <form.Field name={name as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = meta.isTouched && !meta.isValid;
        const picked = (field.state.value as EntityRef[] | null) ?? [];
        const change = (next: EntityRef[]) => {
          field.handleChange(next);
          // Blurring by hand: the picker is a popover, so nothing else ever marks the field touched
          // and its error would stay hidden (as in [EntityAutocompleteField]).
          field.handleBlur();
        };
        return (
          <FieldShell
            name={name}
            label={label}
            required={required}
            hint={hint}
            invalid={invalid}
            errors={fieldErrors(meta, label)}
            className={className}
            ids={ids}
          >
            <div className="flex min-w-0 flex-col gap-1.5">
              <EntityAutocomplete
                id={ids.controlId}
                // The trigger is a button, which a <label htmlFor> cannot name — same as SelectField.
                aria-label={label}
                url={`${entity}/autosearch?search=:search`}
                // Always empty: this control adds, it doesn't hold. Its own reset button therefore
                // never appears, and the chips below carry the removing.
                value={null}
                // Adding members is a series, not a single act: the search stays open with the cursor
                // in its term, so the next name is typed and not clicked open again.
                keepOpenOnSelect
                minChars={minChars}
                params={params}
                onChange={(value) => {
                  if (!value) return;
                  // Silently ignored rather than reported: the same entry twice is no error, the
                  // search simply answers what is already there.
                  if (picked.some((entry) => entry.id === value.id)) return;
                  change([...picked, value]);
                }}
                className="max-w-md"
              />
              {picked.length > 0 && (
                <ul className="flex flex-wrap items-center gap-1.5">
                  {picked.map((entry) => (
                    <li
                      key={entry.id}
                      className={cn(
                        "inline-flex h-6 items-center gap-1 rounded-full border px-2 text-xs font-semibold",
                        "border-primary/25 bg-primary/10 text-primary"
                      )}
                    >
                      {entry.displayName}
                      <button
                        type="button"
                        onClick={() =>
                          change(picked.filter((e) => e.id !== entry.id))
                        }
                        aria-label={`${t("delete")}: ${entry.displayName}`}
                        className="cursor-pointer opacity-60 hover:opacity-100"
                      >
                        <HugeiconsIcon icon={Cancel01Icon} size={12} />
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
