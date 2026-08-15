"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { cn } from "@/lib/utils";
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
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/**
 * One collapsible section of the "all filters" dialog: a heading with what it holds, and its fields
 * in as many columns as the width allows.
 *
 * Fields are laid out from the flattest input to the tallest ([controlRankOf]) instead of in the
 * backend's order: a one-line checkbox next to a list of ten leaves a hole the size of the list, and
 * the ragged edge is invisible once it is in the last row. Their labels drop the group prefix the
 * heading now carries.
 */
export function FilterFieldGroup({
  group,
  label,
  values,
  onChange,
  open,
  onOpenChange,
}: FilterFieldGroupProps) {
  const active = group.elements.filter(
    (element) => !isEmptyFilterValue(values[element.id])
  ).length;
  const fields = [...group.elements].sort(
    (a, b) => controlRankOf(a) - controlRankOf(b)
  );

  return (
    <Collapsible open={open} onOpenChange={onOpenChange}>
      <CollapsibleTrigger className="flex w-full cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-xs font-semibold hover:bg-accent">
        <HugeiconsIcon
          icon={ArrowRight01Icon}
          size={14}
          className={cn("transition-transform", open && "rotate-90")}
        />
        <span className="truncate">{label}</span>
        {active > 0 && (
          <Badge variant="secondary" className="px-1.5 py-0 text-[10px]">
            {active}
          </Badge>
        )}
        <span className="ml-auto font-normal text-muted-foreground">
          {group.elements.length}
        </span>
      </CollapsibleTrigger>
      <CollapsibleContent className="grid auto-rows-min items-start gap-x-6 gap-y-3 px-2 py-2 sm:grid-cols-2 lg:grid-cols-3">
        {fields.map((element) => (
          <div key={element.id} title={element.tooltip}>
            <FilterField
              element={element}
              label={fieldLabelInGroup(element)}
              value={values[element.id]}
              onChange={(value) =>
                onChange(withFilterValue(values, element.id, value))
              }
            />
          </div>
        ))}
      </CollapsibleContent>
    </Collapsible>
  );
}
