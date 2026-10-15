"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  CheckmarkCircle02Icon,
  MinusSignCircleIcon,
} from "@hugeicons/core-free-icons";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { AccessMatrixCell } from "./access-matrix-cell";
import {
  ACCESS_TYPES,
  ACCESS_TYPE_LABEL_KEY,
  OPERATIONS,
  type AccessEntryDto,
} from "../types";

/**
 * The permission matrix as one line of icons — the four access types as four blocks of four
 * accept/deny icons (`●●●●|●●●●|●●●●|●●●●`), a green check where granted and a red minus where denied,
 * the same icons the full [AccessMatrixCell] uses. No row or column labels: identity is carried per
 * icon by its accessible name, and the labelled matrix is one click away in [AccessMatrixPopover].
 *
 * Every cell is present because the DTO normalizes the matrix to the four ordered types
 * (`GroupTaskAccess.copyFrom`); a missing type reads as all-denied.
 */
export function AccessMatrixSummary({
  entries,
  className,
}: {
  entries?: AccessEntryDto[] | null;
  className?: string;
}) {
  const t = useTranslations();
  return (
    <div className={cn("flex items-center gap-1 leading-none", className)}>
      {ACCESS_TYPES.map((type, typeIndex) => {
        const typeLabel = t(ACCESS_TYPE_LABEL_KEY[type]);
        const entry = entries?.find((e) => e.accessType === type);
        return (
          <div
            key={type}
            className={cn(
              "flex items-center",
              // A thin divider between the blocks, standing in for the "|" of ●●●●|●●●●.
              typeIndex > 0 && "border-l border-border pl-1"
            )}
          >
            {OPERATIONS.map((op) => {
              const granted = entry?.[op.key] === true;
              return (
                <HugeiconsIcon
                  key={op.key}
                  icon={granted ? CheckmarkCircle02Icon : MinusSignCircleIcon}
                  size={10}
                  className={cn(
                    "block shrink-0",
                    granted ? "text-emerald-600" : "text-destructive"
                  )}
                  aria-label={`${typeLabel} – ${t(op.labelKey)}: ${t(
                    granted ? "yes" : "no"
                  )}`}
                />
              );
            })}
          </div>
        );
      })}
    </div>
  );
}

/**
 * The compact summary line as the list's default matrix cell, with the full labelled matrix
 * ([AccessMatrixCell]) one click away in a popover — the per-row path to the detail, next to the
 * page-wide pill that expands every row at once (see AccessListActions).
 */
export function AccessMatrixPopover({
  entries,
}: {
  entries?: AccessEntryDto[] | null;
}) {
  const t = useTranslations();
  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label={t("access.type._")}
          // The row opens the edit page on click; stop the trigger's click there so opening the
          // popover does not also navigate away (see DataTableRow.onRowClick).
          onClick={(e) => e.stopPropagation()}
          className="-mx-1 rounded-sm px-1 py-0.5 hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
        >
          <AccessMatrixSummary entries={entries} />
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto">
        <AccessMatrixCell entries={entries} />
      </PopoverContent>
    </Popover>
  );
}
