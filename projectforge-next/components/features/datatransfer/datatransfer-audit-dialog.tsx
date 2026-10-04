"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/shared/spinner";
import {
  fetchDataTransferAudit,
  type DataTransferAuditEntry,
} from "@/lib/rs/datatransfer";

type Column = ColumnDef<DataTransferAuditEntry, unknown>;

/**
 * The activities of an area over the last 30 days (`DataTransferAuditRest`): what was uploaded, changed
 * and deleted, and — a table of its own, as in the legacy modal — who downloaded what.
 *
 * Read when opened, not with the file view: the activities are looked at rarely, and they grow with
 * every download.
 */
export function DataTransferAuditDialog({
  id,
  onClose,
}: {
  id: number;
  onClose: () => void;
}) {
  const t = useTranslations();
  const query = useQuery({
    queryKey: ["datatransfer", "audit", id],
    queryFn: ({ signal }) => fetchDataTransferAudit(id, signal),
  });
  const columns = useMemo(() => {
    const column = (
      id: keyof DataTransferAuditEntry,
      header: string,
      size: number
    ): Column => ({
      id,
      header,
      size,
      accessorFn: (row) => row[id] ?? "",
      meta: { label: header },
    });
    // The time as "3 hours ago", which is what the activities are read by; sorted by the timestamp.
    const time: Column = {
      ...column("timeAgo", t("timestamp"), 130),
      sortingFn: (a, b) =>
        (a.original.timestamp ?? "").localeCompare(b.original.timestamp ?? ""),
    };
    const file = column("filenameAsString", t("attachment.fileName"), 220);
    return {
      events: [
        time,
        column("byUserAsString", t("modifiedBy"), 160),
        file,
        column("eventAsString", t("plugins.datatransfer.audit.action"), 120),
        column("description", t("description"), 220),
      ],
      downloads: [
        time,
        column(
          "byUserAsString",
          t("plugins.datatransfer.audit.downloadedBy"),
          160
        ),
        file,
      ],
    };
  }, [t]);
  const audit = query.data;

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle>
            {t("plugins.datatransfer.audit._")}
            {audit?.areaName ? `: ${audit.areaName}` : ""}
          </DialogTitle>
        </DialogHeader>
        {query.isLoading ? (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : (
          <div className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto">
            <AuditTable
              title={t("plugins.datatransfer.audit.events")}
              columns={columns.events}
              rows={audit?.events ?? []}
            />
            <AuditTable
              title={t("plugins.datatransfer.audit.downloadEvents")}
              columns={columns.downloads}
              rows={audit?.downloadEvents ?? []}
            />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

function AuditTable({
  title,
  columns,
  rows,
}: {
  title: string;
  columns: Column[];
  rows: DataTransferAuditEntry[];
}) {
  return (
    <section className="flex flex-col gap-2">
      <h3 className="text-sm font-semibold">{title}</h3>
      <div className="flex max-h-80 flex-col">
        <DataTable<DataTransferAuditEntry>
          columns={columns}
          data={rows}
          enableColumnFilters={false}
          manualSorting={false}
          showPagination={false}
          getRowId={(row) => String(row.id)}
        />
      </div>
    </section>
  );
}
