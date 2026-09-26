"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  CheckmarkCircle02Icon,
  MinusSignCircleIcon,
} from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";
import {
  ACCESS_TYPES,
  ACCESS_TYPE_LABEL_KEY,
  OPERATIONS,
  type AccessEntryDto,
} from "../types";

/**
 * The permission matrix as the list shows it — four access types (rows) by the four operations
 * (columns) of accept / deny icons, the read-only counterpart of the edit page's [AccessMatrix] and the
 * Wicket `AccessTablePanel` the list drew per row. Green check = granted, red minus = denied, exactly as
 * the classic list reads.
 *
 * Every cell is present because the DTO normalizes the matrix to the four ordered rows
 * (`GroupTaskAccess.copyFrom`); a row missing a type still reads as all-denied. Operation identity is on
 * the column-header icons (named on hover), access-type identity on the row labels, so the status icons
 * only need an accessible name and no tooltip of their own.
 */
export function AccessMatrixCell({
  entries,
  className,
}: {
  entries?: AccessEntryDto[] | null;
  className?: string;
}) {
  const t = useTranslations();
  return (
    <table
      className={cn(
        "border-separate border-spacing-0 text-xs leading-none",
        className
      )}
    >
      <thead>
        <tr>
          <th className="h-auto! p-0!" />
          {OPERATIONS.map((op) => (
            <th
              key={op.key}
              className="h-auto! px-px! py-0! text-center font-normal"
            >
              <HintTooltip text={t(op.labelKey)} openOnTap>
                <HugeiconsIcon
                  icon={op.icon}
                  size={12}
                  className="mx-auto block text-muted-foreground"
                  aria-label={t(op.labelKey)}
                />
              </HintTooltip>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {ACCESS_TYPES.map((type) => {
          const rowLabel = t(ACCESS_TYPE_LABEL_KEY[type]);
          const entry = entries?.find((e) => e.accessType === type);
          return (
            <tr key={type}>
              <th
                scope="row"
                className="h-auto! py-0! pr-1! pl-0! text-left font-normal whitespace-nowrap text-muted-foreground"
              >
                {rowLabel}
              </th>
              {OPERATIONS.map((op) => {
                const granted = entry?.[op.key] === true;
                const opLabel = t(op.labelKey);
                return (
                  <td key={op.key} className="px-px! py-0! text-center">
                    <HugeiconsIcon
                      icon={
                        granted ? CheckmarkCircle02Icon : MinusSignCircleIcon
                      }
                      size={13}
                      className={cn(
                        "mx-auto block",
                        granted ? "text-emerald-600" : "text-destructive"
                      )}
                      aria-label={`${rowLabel} – ${opLabel}: ${t(
                        granted ? "yes" : "no"
                      )}`}
                    />
                  </td>
                );
              })}
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
