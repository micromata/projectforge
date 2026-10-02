"use client";

import type { KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { FreeTextBadge } from "@/components/shared/free-text-badge";
import { HighlightedText } from "@/components/shared/highlighted-text";
import type { ChecklistOption } from "./checklist";
import { toggled } from "./checklist-selection";

/**
 * More rows than this are not rendered: a list of thousands would stall the popover, and nobody
 * scrolls through them — the hint below asks for a narrower search instead.
 */
const MAX_RENDERED = 200;

/** The matching entries of a [Checklist], one checkbox each. */
export function ChecklistRows({
  options,
  selected,
  onChange,
  term,
  describedBy,
}: {
  options: ChecklistOption[];
  selected: string[];
  onChange: (values: string[]) => void;
  /** The search, highlighted in the labels. */
  term: string;
  describedBy?: string;
}) {
  const t = useTranslations();
  const shown = options.slice(0, MAX_RENDERED);
  return (
    <>
      <ul
        className="max-h-72 overflow-y-auto py-1"
        onKeyDown={moveFocus}
        aria-describedby={describedBy}
      >
        {shown.map((option) => (
          <li key={option.value}>
            <label className="flex cursor-pointer items-center gap-2 px-2 py-1 text-sm hover:bg-muted">
              <Checkbox
                checked={selected.includes(option.value)}
                onCheckedChange={() =>
                  onChange(toggled(selected, option.value))
                }
              />
              <span className="min-w-0 flex-1 truncate">
                <HighlightedText text={option.label} query={term} />
              </span>
              {option.freeText && <FreeTextBadge />}
            </label>
          </li>
        ))}
      </ul>
      {options.length > shown.length && (
        <p className="border-t px-2 py-1 text-xs text-muted-foreground">
          {t("select.checklist.truncated", {
            arg0: shown.length,
            arg1: options.length,
          })}
        </p>
      )}
    </>
  );
}

/** Arrow up/down step between the checkboxes of the list, as in a menu. */
function moveFocus(event: KeyboardEvent<HTMLUListElement>) {
  if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
  const boxes = Array.from(
    event.currentTarget.querySelectorAll<HTMLButtonElement>(
      "button[role=checkbox]"
    )
  );
  const index = boxes.indexOf(document.activeElement as HTMLButtonElement);
  const next = boxes[index + (event.key === "ArrowDown" ? 1 : -1)];
  if (!next) return;
  event.preventDefault();
  next.focus();
}
