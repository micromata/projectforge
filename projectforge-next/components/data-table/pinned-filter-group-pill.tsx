"use client";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { FilterElement } from "@/lib/rs/types";
import { FilterPillFace, filterPillFrameClass } from "./filter-pill-face";
import { foldedPinnedFiltersLabel } from "./pinned-filter-group";

/**
 * The empty pinned checklists as one pill, "Business Unit / Kunde / Projekt" (see [foldedPinnedFilters]).
 * It opens a menu of its fields; picking one hands it to [onSelect], which puts that field's own pill on
 * the row with its checklist open — the same way the "+" chip adds a field.
 */
export function PinnedFilterGroupPill({
  folded,
  onSelect,
}: {
  folded: FilterElement[];
  onSelect: (id: string) => void;
}) {
  const label = foldedPinnedFiltersLabel(folded);
  return (
    <span className={filterPillFrameClass(false)}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            type="button"
            aria-label={label}
            className="min-w-0 cursor-pointer rounded-full px-2.5 py-0.5"
          >
            <FilterPillFace label={label} />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          align="start"
          // The focus belongs to the popover of the picked field, which opens as this menu closes;
          // handed back to this trigger, it would close that popover again at once.
          onCloseAutoFocus={(event) => event.preventDefault()}
        >
          {folded.map((element) => (
            <HintTooltip key={element.id} text={element.tooltip}>
              <DropdownMenuItem onSelect={() => onSelect(element.id)}>
                {element.label ?? element.id}
              </DropdownMenuItem>
            </HintTooltip>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </span>
  );
}
