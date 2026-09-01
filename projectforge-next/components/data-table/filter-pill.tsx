"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import { FilterField } from "./filter-field";
import { FilterPillShell } from "./filter-pill-shell";
import { useFilterPeriodKinds } from "./filter-period-kinds";
import { pageablePeriodOf, steppedPeriodValue } from "./filter-period";
import { useDebouncedApply } from "./use-debounced-apply";
import { describeFilterValue, isEmptyFilterValue } from "./filter-value";

interface FilterPillProps {
  element: FilterElement;
  value: MagicFilterEntryValue | undefined;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Default filters stay on the row, so they only offer emptying, not removing. */
  removable: boolean;
  /** Applies a value to the list without closing the popover — the list refetches live. */
  onSave: (value: MagicFilterEntryValue | undefined) => void;
  onDelete: () => void;
}

/**
 * One filter field as a pill whose popover holds its input — the primary way to filter a
 * list, as in the legacy webapp. Editing a draft applies to the list automatically (debounced),
 * so the list follows along as the user steps a period; closing keeps what was applied and
 * "Abbrechen" restores the value the popover opened with.
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
  // The committed value the popover opened with, restored by "Abbrechen".
  const baseline = useRef(value);
  const ctx = useFormatContext();
  const t = useTranslations();

  useDebouncedApply(draft, value, save);

  // A period filter earns a wider popover: with room the two date boxes sit side by side (the `@2xs`
  // the default `w-72` stays just under, see [RangeBounds]) and the art is spelled out below them,
  // rather than stacked and abbreviated as they are in a form's narrow column.
  const isPeriod =
    element.filterType === "DATE" || element.filterType === "TIMESTAMP";

  // A period in effect: the pill offers arrows that page it without opening the popover, so the list —
  // and the statistics above it — step month by month while the popover stays closed.
  const kinds = useFilterPeriodKinds();
  const period = pageablePeriodOf(value, kinds, ctx);
  const onStep = period
    ? (steps: number) => {
        const next = steppedPeriodValue(value, steps, kinds, ctx);
        if (!next) return;
        // Keep the draft in step too, in case the popover is open while paging from the pill.
        setDraft(next);
        onSave(next);
      }
    : undefined;

  // The pill truncates its text, so the tooltip carries the full value — a long task path is only
  // recognizable that way. With no value it falls back to the field's description (element.tooltip).
  const label = element.label ?? element.id;
  const valueText = describeFilterValue(value, element, ctx);

  // A task filter's value names the whole path ("A | B | Task"); the pill shows only the task itself,
  // since the ancestors would truncate away the one segment that identifies it. The full path stays in
  // the tooltip and the popover. Its picker needs a wider popover for the breadcrumb and the controls.
  const isTask =
    element.filterType === "OBJECT" && element.autoCompletion?.type === "TASK";
  const pillText = isTask ? taskLeafOf(valueText) : valueText;

  return (
    <FilterPillShell
      label={label}
      text={pillText}
      tooltip={valueText ? `${label}: ${valueText}` : element.tooltip}
      active={!isEmptyFilterValue(value)}
      onStep={onStep}
      stepPreviousLabel={t(
        period?.kind.tooltipPreviousKey ?? "duration.previous"
      )}
      stepNextLabel={t(period?.kind.tooltipNextKey ?? "duration.next")}
      open={open}
      onOpenChange={(next) => {
        // Re-seed on open so a live-applied edit can still be taken back.
        if (next) {
          setDraft(value);
          baseline.current = value;
        }
        onOpenChange(next);
      }}
      removable={removable}
      onCancel={cancel}
      onDelete={onDelete}
      contentClassName={isPeriod ? "w-80" : isTask ? "w-96" : undefined}
    >
      <FilterField
        element={element}
        value={draft}
        onChange={setDraft}
        autoFocus
        // The pill's own popover already holds the field; a field opening a second one over it would
        // cover the buttons below.
        inline
        // Enter is "done": apply straight away, without waiting for the debounce, and close.
        onSubmit={(committed) => {
          save(committed);
          onOpenChange(false);
        }}
      />
    </FilterPillShell>
  );

  /**
   * Applying an emptied field removes it, as in the legacy MagicInput.isEmpty check.
   *
   * A field submitting on Enter passes what it changed to, because its `onChange` has not come back
   * through `setDraft` yet at that point (see FilterField.onSubmit).
   */
  function save(value: MagicFilterEntryValue | undefined = draft) {
    onSave(isEmptyFilterValue(value) ? undefined : value);
  }

  /** Restore the value the popover opened with and close. */
  function cancel() {
    setDraft(baseline.current);
    save(baseline.current);
    onOpenChange(false);
  }
}

/** The last segment of a " | "-joined task path — the task itself, without its ancestors. */
function taskLeafOf(path: string): string {
  const segments = path.split(" | ");
  return segments[segments.length - 1] || path;
}
