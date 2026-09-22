"use client";

import { useTranslations } from "next-intl";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { SectionCard } from "@/components/shared/section-card";
import type { ImportStorageInfo } from "./import-types";

interface Props {
  info?: ImportStorageInfo;
}

/**
 * The column reference of an upload, shown at the end of the preview: which target field each recognised
 * CSV column was mapped to (the "Erkannte Spalten" table of the classic import page), then the columns the
 * parser did not recognise. Read-only and identical before and after reconcile — it describes the file, not
 * the reconciliation. Renders nothing until an upload has reported its columns.
 */
export function ImportColumnInfo({ info }: Props) {
  const t = useTranslations();
  const mappings = info?.detectedColumnMappings ?? [];
  const unknown = info?.unknownColumns ?? [];
  if (mappings.length === 0 && unknown.length === 0) return null;

  return (
    <SectionCard className="flex flex-col gap-4 bg-muted/40">
      {mappings.length > 0 && (
        <div className="flex flex-col gap-2">
          <h3 className="text-sm font-semibold">
            {t("import.info.detectedColumns")}
          </h3>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("import.field.name")}</TableHead>
                <TableHead>{t("import.field.mapping")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {mappings.map((mapping, index) => (
                <TableRow key={`${mapping.field}-${mapping.header}-${index}`}>
                  <TableCell className="font-medium">{mapping.field}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {mapping.header}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      {unknown.length > 0 && (
        <div className="flex flex-col gap-1">
          <h3 className="text-sm font-semibold">
            {t("import.info.unknownColumns")}
          </h3>
          <p className="text-xs text-muted-foreground">{unknown.join(", ")}</p>
        </div>
      )}
    </SectionCard>
  );
}
