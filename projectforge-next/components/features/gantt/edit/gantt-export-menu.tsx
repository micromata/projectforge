"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { toast } from "@/lib/toast";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ExportButton } from "@/components/shared/export-button";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { RsError } from "@/lib/rs/client";
import { downloadGanttExport, type GanttExportFormat } from "@/lib/rs/gantt";
import type { GanttValues } from "../gantt-schema";
import type { GanttDiagramDetail } from "../types";

/** The formats of Wicket's export choice, in its order, with their labels. */
const FORMATS: { format: GanttExportFormat; labelKey: string }[] = [
  { format: "PDF", labelKey: "gantt.export.pdf" },
  { format: "SVG", labelKey: "gantt.export.svg" },
  { format: "PNG", labelKey: "gantt.export.png" },
  { format: "JPG", labelKey: "gantt.export.jpg" },
  { format: "MS_PROJECT_MPX", labelKey: "gantt.export.msproject.mpx" },
  { format: "MS_PROJECT_XML", labelKey: "gantt.export.msproject.xml" },
  { format: "PROJECTFORGE", labelKey: "gantt.export.projectforge" },
];

interface FormState {
  values: GanttValues;
}

/** The exports of the chart. They take the form as it is, unsaved changes included, as Wicket's do. */
export function GanttExportMenu() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const hasRoot = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.root != null
  );
  const download = useMutation({
    mutationFn: (format: GanttExportFormat) =>
      downloadGanttExport(
        (form.state as FormState).values as GanttDiagramDetail,
        format
      ),
    onError: (error: unknown) => {
      // A 404 is no error: the chart has nothing to draw (no task or no visible object).
      if (error instanceof RsError && error.status === 404) {
        toast.info(t("datatable.no-records-found"));
        return;
      }
      toast.error(error instanceof Error ? error.message : String(error));
    },
  });

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild disabled={!hasRoot}>
        <ExportButton
          label={t("export")}
          isPending={download.isPending}
          disabled={!hasRoot}
        />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {FORMATS.map(({ format, labelKey }) => (
          <DropdownMenuItem
            key={format}
            onSelect={() => download.mutate(format)}
          >
            {t(labelKey)}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
