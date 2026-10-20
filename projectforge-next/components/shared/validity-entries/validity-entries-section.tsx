"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatNumber, formatNumberUpTo } from "@/lib/format";
import { todayOf } from "@/lib/user-zone";
import { cn } from "@/lib/utils";
import {
  currentEntry,
  sortByValidity,
  validityOf,
} from "./validity-entries-model";
import {
  toNumber,
  ValidityEntryDialog,
  type ValidityEntryDialogProps,
} from "./validity-entry-dialog";
import type {
  ValidityEntriesAdapter,
  ValidityEntry,
  ValidityValueField,
} from "./types";

export interface ValidityEntriesSectionProps<T extends ValidityEntry> {
  entries: readonly T[];
  /** Property of the entry holding its start date (`validSince`, `validFrom`). */
  dateField: keyof T & string;
  /** Header of the date column and label of its input. */
  dateLabelKey?: string;
  valueFields: readonly ValidityValueField[];
  /** Title of the dialog, usually the section's title. */
  dialogTitle: string;
  /** A fresh entry for the add dialog, carrying whatever the backend needs to file it (the owner, the kind). */
  newEntry: () => T;
  adapter: ValidityEntriesAdapter<T>;
  /** Without write access: no add button, rows don't open. */
  readOnly?: boolean;
  /** After every successful write, with the fresh entries. */
  onChanged: (entries: T[]) => void;
  /** Values proposed for a new entry, see [ValidityEntryDialogProps.prefill]. */
  prefill?: ValidityEntryDialogProps<T>["prefill"];
}

/**
 * The entries of something that changes over time, each valid from its date on until the next one starts
 * (see validity-entries-model): an employee's weekly hours or annual leave, a currency pair's rates.
 *
 * Newest first; the entry in force today is marked, the ones still ahead are muted. A row opens the
 * [ValidityEntryDialog], which writes the entry right away through its own endpoint ([adapter]) — not with
 * the form around it — and the section shows what the backend answered.
 */
export function ValidityEntriesSection<T extends ValidityEntry>({
  entries,
  dateField,
  dateLabelKey = "attr.validSince",
  valueFields,
  dialogTitle,
  newEntry,
  adapter,
  readOnly,
  onChanged,
  prefill,
}: ValidityEntriesSectionProps<T>) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const [editing, setEditing] = useState<T | null>(null);
  const dateOf = (entry: T) => entry[dateField] as string | null | undefined;
  const today = todayOf(ctx);
  const sorted = useMemo(
    () =>
      sortByValidity(entries, (e) => e[dateField] as string | null | undefined),
    [entries, dateField]
  );
  const current = currentEntry(sorted, dateOf, today);

  // A number with a unit reads as one (`0,92 USD`), below a header like `1 EUR =`.
  const withSuffix = (field: ValidityValueField, text: string) =>
    field.suffix && text ? `${text} ${field.suffix}` : text;

  const formatValue = (field: ValidityValueField, value: unknown) => {
    switch (field.kind) {
      case "int":
        return withSuffix(field, formatNumber(toNumber(value), ctx, 0));
      case "decimal":
        return withSuffix(
          field,
          field.maxFractionDigits != null
            ? formatNumberUpTo(toNumber(value), ctx, field.maxFractionDigits)
            : formatNumber(toNumber(value), ctx, field.fractionDigits)
        );
      case "select": {
        const option = field.options?.find((o) => o.value === value);
        return option ? t(option.labelKey) : value == null ? "" : String(value);
      }
      default:
        return value == null ? "" : String(value);
    }
  };

  return (
    <div className="flex flex-col gap-2">
      {sorted.length > 0 && (
        <div className="overflow-x-auto rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-40">{t(dateLabelKey)}</TableHead>
                {valueFields.map((field) => (
                  <TableHead
                    key={field.name}
                    className={cn(
                      field.kind !== "select" &&
                        field.kind !== "text" &&
                        "text-right"
                    )}
                  >
                    {field.label ?? t(field.labelKey)}
                  </TableHead>
                ))}
                <TableHead>{t("comment")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sorted.map((entry, index) => {
                const validity = validityOf(entry, current, dateOf, today);
                return (
                  <TableRow
                    key={entry.id ?? `new-${index}`}
                    data-validity={validity}
                    className={cn(
                      !readOnly && "cursor-pointer",
                      validity === "future" && "text-muted-foreground"
                    )}
                    onClick={readOnly ? undefined : () => setEditing(entry)}
                  >
                    <TableCell className="whitespace-nowrap">
                      {formatDate(dateOf(entry), ctx)}
                      {validity === "current" && (
                        <Badge variant="secondary" className="ml-2">
                          {t("attr.currentlyValid")}
                        </Badge>
                      )}
                    </TableCell>
                    {valueFields.map((field) => (
                      <TableCell
                        key={field.name}
                        className={cn(
                          field.kind !== "select" &&
                            field.kind !== "text" &&
                            "text-right tabular-nums"
                        )}
                      >
                        {formatValue(
                          field,
                          (entry as Record<string, unknown>)[field.name]
                        )}
                      </TableCell>
                    ))}
                    <TableCell className="whitespace-normal">
                      {entry.comment}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
      {!readOnly && (
        <div>
          <Button
            type="button"
            size="sm"
            onClick={() => setEditing(newEntry())}
          >
            <HugeiconsIcon icon={PlusSignIcon} strokeWidth={2.5} />
            {t("add")}
          </Button>
        </div>
      )}
      {editing && (
        <ValidityEntryDialog<T>
          entry={editing}
          title={dialogTitle}
          dateField={dateField}
          dateLabelKey={dateLabelKey}
          valueFields={valueFields}
          onSave={(entry) =>
            entry.id == null ? adapter.insert(entry) : adapter.update(entry)
          }
          onDelete={editing.id == null ? undefined : adapter.remove}
          onDone={(fresh) => {
            setEditing(null);
            onChanged(fresh);
          }}
          onClose={() => setEditing(null)}
          prefill={prefill}
        />
      )}
    </div>
  );
}
