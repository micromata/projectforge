"use client";

import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { FilterField } from "./filter-field";
import {
  controlRankOf,
  fieldLabelInGroup,
  type FilterFieldGroup as Group,
} from "./filter-groups";
import {
  isEmptyFilterValue,
  withFilterValue,
  type FilterValues,
} from "./filter-value";

interface FilterFieldGroupProps {
  group: Group;
  /** The heading; the caller translates the groups the client makes up itself. */
  label: string;
  values: FilterValues;
  onChange: (values: FilterValues) => void;
}

/**
 * One section of the "all filters" dialog: a heading with what it holds, and its fields in as many
 * columns as the width allows.
 *
 * Always open. The dialog is the place to see everything a list can be filtered by, and a section
 * that has to be clicked open hides exactly that — with the fields down to one line each
 * ([controlRankOf]), the whole set fits without collapsing any of it.
 *
 * Fields are laid out from the flattest input to the tallest instead of in the backend's order: a
 * one-line checkbox next to a two-line range leaves a hole the size of the range, and the ragged
 * edge is invisible once it is in the last row. Their labels drop the group prefix the heading now
 * carries.
 */
export function FilterFieldGroup({
  group,
  label,
  values,
  onChange,
}: FilterFieldGroupProps) {
  const active = group.elements.filter(
    (element) => !isEmptyFilterValue(values[element.id])
  ).length;
  const fields = [...group.elements].sort(
    (a, b) => controlRankOf(a) - controlRankOf(b)
  );

  return (
    <section>
      <h3 className="flex items-center gap-2 px-2 py-1.5 text-xs font-semibold">
        <span className="truncate">{label}</span>
        {active > 0 && (
          <Badge variant="secondary" className="px-1.5 py-0 text-[10px]">
            {active}
          </Badge>
        )}
        <span className="ml-auto font-normal text-muted-foreground">
          {group.elements.length}
        </span>
      </h3>
      <div className="grid auto-rows-min items-start gap-x-6 gap-y-3 px-2 py-2 sm:grid-cols-2 lg:grid-cols-3">
        {fields.map((element) => (
          <HintTooltip key={element.id} text={element.tooltip}>
            <div>
              <FilterField
                element={element}
                label={fieldLabelInGroup(element)}
                value={values[element.id]}
                onChange={(value) =>
                  onChange(withFilterValue(values, element.id, value))
                }
              />
            </div>
          </HintTooltip>
        ))}
      </div>
    </section>
  );
}
