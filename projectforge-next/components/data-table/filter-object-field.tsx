"use client";

import { Label } from "@/components/ui/label";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { useSelectMeRef } from "@/hooks/use-current-user-ref";
import type { FilterElement } from "@/lib/rs/types";
import { TextField, type FilterInputProps } from "./filter-field-inputs";
import { FilterTaskField } from "./filter-task-field";

/**
 * An OBJECT filter (org.projectforge.ui.filter.UIFilterObjectElement): picks the entity to filter
 * by, e.g. the user of "geändert durch".
 *
 * The value carries `id` — which is what `MagicFilterProcessor` reads
 * (`value.id ?: value.value?.toLongOrNull()`) — plus `displayName`, so the pill and a restored
 * favorite can name the entity without looking it up again.
 */
export function FilterObjectField({
  element,
  value,
  onChange,
  label,
  id,
  autoFocus,
  autoOpen = autoFocus,
  onSubmit,
}: FilterInputProps & {
  element: FilterElement;
  /**
   * Opens the picker along with the focus. Defaults to [autoFocus]: a pill of its own has nothing
   * else to cover. A field among others (the history group) turns it off.
   */
  autoOpen?: boolean;
}) {
  // Only a user or employee filter offers the „select me" smiley: it picks the logged-in user (or their
  // employee) with one click, as the form's [EntityAutocompleteField] does. A filter on a project or
  // customer takes a different entity's id, for which the current user means nothing.
  const selectMe = useSelectMeRef(element.autoCompletion?.type?.toLowerCase());

  // A task filter (AutoCompletion.Type.TASK) picks from the structure tree, not a flat combobox — a
  // task title only means something in its place in the structure (see [FilterTaskField]).
  if (element.autoCompletion?.type === "TASK") {
    return (
      <FilterTaskField
        value={value}
        onChange={onChange}
        label={label}
        id={id}
        autoFocus={autoFocus}
        onSubmit={onSubmit}
      />
    );
  }

  const url = element.autoCompletion?.url;

  // No lookup url means there is nothing to search: fall back to the plain text input, whose value
  // the backend still parses as an id. Not expected — every UIFilterObjectElement carries one — but
  // an empty, unusable field would be worse than a typable one.
  if (!url) {
    return (
      <TextField
        value={value}
        onChange={onChange}
        label={label}
        id={id}
        autoFocus={autoFocus}
        onSubmit={onSubmit}
      />
    );
  }

  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <EntityAutocomplete
        id={`filter-${id}`}
        url={url}
        minChars={element.autoCompletion?.minChars}
        autoFocus={autoFocus}
        // A freshly added filter (user, group, …) should open its picker at once, with the search
        // input focused, so the user types the entry to look for without a further click.
        autoOpen={autoOpen}
        aria-label={label}
        selectMe={selectMe}
        value={entityRefOf(value)}
        onChange={(entity) =>
          onChange(
            entity
              ? { id: entity.id, displayName: entity.displayName }
              : undefined
          )
        }
      />
    </div>
  );
}

/** The stored value as the autocomplete's, or null when no entity is picked. */
function entityRefOf(value: FilterInputProps["value"]): EntityRef | null {
  if (value?.id == null) return null;
  // A filter restored from the backend has its name resolved by MagicFilter.init; `label` is the
  // same text under the name the legacy frontend used.
  return {
    id: value.id,
    displayName: value.displayName ?? value.label ?? String(value.id),
  };
}
