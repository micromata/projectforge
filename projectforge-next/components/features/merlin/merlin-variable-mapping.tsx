"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import {
  isMerlinCsvWritable,
  parseMerlinCsv,
  writeMerlinCsv,
} from "./merlin-csv";

/**
 * The mapping of a dependent variable: one row per value of its master variable, with the value the
 * variable gets for it. Stored as csv by position (`mappingValues`), which is written here — so a
 * value may hold a comma. Values beyond the master's (from a master changed meanwhile) are kept.
 */
export function MerlinVariableMapping({
  masterName,
  name,
  masterValues,
  csv,
  onChange,
}: {
  masterName: string;
  /** The name of the dependent variable, the heading of its column. */
  name: string;
  masterValues: readonly string[];
  csv: string | null | undefined;
  /** The new csv, and whether every value can be written into it. */
  onChange: (csv: string | null, valid: boolean) => void;
}) {
  const t = useTranslations();
  const ids = useFieldIds();
  // Held as typed: the csv is trimmed, so reading it back while typing would swallow a trailing blank.
  const [values, setValues] = useState(() => parseMerlinCsv(csv));
  const update = (index: number, value: string) => {
    const next = Array.from(
      { length: Math.max(values.length, index + 1) },
      (_, i) => (i === index ? value : (values[i] ?? ""))
    );
    setValues(next);
    // Trailing empty values are the same as none to the backend.
    let length = next.length;
    while (length > 0 && next[length - 1].trim() === "") length--;
    const kept = next.slice(0, length);
    onChange(
      kept.length > 0 ? writeMerlinCsv(kept) : null,
      kept.every(isMerlinCsvWritable)
    );
  };
  const invalid = values.some((it) => !isMerlinCsvWritable(it));

  return (
    <FieldShell
      label={t("plugins.merlin.variable.mapping._")}
      hint={t("plugins.merlin.variable.mapping.info")}
      invalid={invalid}
      errors={invalid ? [t("plugins.merlin.variable.mapping.notWritable")] : []}
      ids={ids}
      className="min-w-0 sm:col-span-2"
    >
      <Table aria-labelledby={ids.labelId}>
        <TableHeader>
          <TableRow>
            <TableHead className="w-1/3">{masterName}</TableHead>
            <TableHead>{name || t("value")}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {masterValues.map((masterValue, index) => (
            <TableRow key={index}>
              <TableCell className="break-words whitespace-normal">
                {masterValue}
              </TableCell>
              <TableCell>
                <Input
                  id={index === 0 ? ids.controlId : undefined}
                  aria-label={`${name || t("value")}: ${masterValue}`}
                  aria-invalid={!isMerlinCsvWritable(values[index] ?? "")}
                  value={values[index] ?? ""}
                  onChange={(e) => update(index, e.target.value)}
                />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </FieldShell>
  );
}
