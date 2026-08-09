"use client";

import { useState } from "react";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import { FilterField } from "./filter-field";
import { FilterPillShell } from "./filter-pill-shell";
import { describeFilterValue, isEmptyFilterValue } from "./filter-value";

interface FilterPillProps {
  element: FilterElement;
  value: MagicFilterEntryValue | undefined;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Default filters stay on the row, so they only offer emptying, not removing. */
  removable: boolean;
  onSave: (value: MagicFilterEntryValue | undefined) => void;
  onDelete: () => void;
}

/**
 * One filter field as a pill whose popover holds its input — the primary way to filter a
 * list, as in the legacy webapp. Editing happens on a draft so the list is only
 * refetched once, on save.
 */
export function FilterPill({
  element,
  value,
  open,
  onOpenChange,
  removable,
  onSave,
  onDelete,
}: FilterPillProps) {
  const [draft, setDraft] = useState(value);

  return (
    <FilterPillShell
      label={element.label ?? element.id}
      text={describeFilterValue(value, element)}
      tooltip={element.tooltip}
      active={!isEmptyFilterValue(value)}
      open={open}
      onOpenChange={(next) => {
        // Re-seed on open so an abandoned edit doesn't come back.
        if (next) setDraft(value);
        onOpenChange(next);
      }}
      removable={removable}
      onSave={() => save()}
      onDelete={onDelete}
    >
      <FilterField
        element={element}
        value={draft}
        onChange={setDraft}
        autoFocus
        onSubmit={(committed) => save(committed)}
      />
    </FilterPillShell>
  );

  /**
   * Saving an emptied field removes it, as in the legacy MagicInput.isEmpty check.
   *
   * A field submitting on Enter passes what it changed to, because its `onChange` has not come back
   * through `setDraft` yet at that point (see FilterField.onSubmit).
   */
  function save(value: MagicFilterEntryValue | undefined = draft) {
    onSave(isEmptyFilterValue(value) ? undefined : value);
  }
}
