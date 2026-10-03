"use client";

import { useTranslations } from "next-intl";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { useGanttPreview } from "./use-gantt-preview";

/**
 * The chart as the server's Batik renderer draws it (the same drawing as the SVG/PDF export), shown as an
 * image: the SVG is the backend's, so it is not mounted into the page's DOM.
 */
export function GanttChartPreview() {
  const t = useTranslations();
  const preview = useGanttPreview();
  const svg = preview.data?.svg;
  if (preview.isLoading) return <Skeleton className="h-48 w-full" />;
  if (!svg) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("datatable.no-records-found")}
      </p>
    );
  }
  return (
    <div className="overflow-x-auto">
      {/* eslint-disable-next-line @next/next/no-img-element -- a data URL, nothing to optimise */}
      <img
        src={`data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`}
        alt={t("gantt.title.heading")}
        className={cn("max-w-none", preview.isFetching && "opacity-60")}
      />
    </div>
  );
}
