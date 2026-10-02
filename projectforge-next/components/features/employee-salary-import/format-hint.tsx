"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";

/**
 * The required file format of the employee-salary import, shown at the foot of the import page so it is
 * visible before and during an upload. It mirrors {@link EmployeeSalaryExcelImporter} on the backend: an
 * Excel file whose sheet is auto-detected by its header row, the three required columns (each with the
 * header aliases the parser accepts) and the `yyyy/MM` accounting month. The header strings are literal
 * spreadsheet identifiers, not UI language, and stay as they are; only the prose is translated.
 */
const COLUMN_HEADERS = {
  staffNumber: ["Pers.Nr.", "Personalnummer", "Personalnr."],
  month: ["Abrechnungsmonat", "Monat"],
  brutto: ["Gesamtkosten", "bruttoMitAgAnteil"],
} as const;

const MONTH_FORMAT = "yyyy/MM";
const MONTH_EXAMPLE = "2026/08";

function ColumnRow({
  headers,
  children,
}: {
  headers: readonly string[];
  children: ReactNode;
}) {
  const t = useTranslations();
  const [primary, ...aliases] = headers;
  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 text-xs">
        <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-foreground">
          {primary}
        </code>
        {aliases.length > 0 && (
          <span className="text-muted-foreground">
            {t("fibu.employee.salaries.import.format.also")}{" "}
            {aliases.map((alias) => (
              <code key={alias} className="mx-0.5 font-mono">
                {alias}
              </code>
            ))}
          </span>
        )}
      </div>
      <p className="text-xs text-muted-foreground">{children}</p>
    </div>
  );
}

export function EmployeeSalaryImportFormatHint() {
  const t = useTranslations();
  return (
    <SectionCard className="flex flex-col gap-3 bg-muted/40">
      <h3 className="text-sm font-semibold">
        {t("fibu.employee.salaries.import.format.title")}
      </h3>
      <p className="text-xs text-muted-foreground">
        {t("fibu.employee.salaries.import.format.intro")}
      </p>
      <div className="flex flex-col gap-2">
        <p className="text-xs font-medium">
          {t("fibu.employee.salaries.import.format.columns")}
        </p>
        <ColumnRow headers={COLUMN_HEADERS.staffNumber}>
          {t("fibu.employee.salaries.import.format.staffNumber")}
        </ColumnRow>
        <ColumnRow headers={COLUMN_HEADERS.month}>
          {t("fibu.employee.salaries.import.format.month", {
            arg0: MONTH_FORMAT,
            arg1: MONTH_EXAMPLE,
          })}
        </ColumnRow>
        <ColumnRow headers={COLUMN_HEADERS.brutto}>
          {t("fibu.employee.salaries.import.format.brutto")}
        </ColumnRow>
      </div>
    </SectionCard>
  );
}
