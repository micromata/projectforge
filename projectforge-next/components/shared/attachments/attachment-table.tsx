"use client";

import { createContext, useContext } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef, Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete01Icon, Edit02Icon } from "@hugeicons/core-free-icons";
import { DataTable, DataTableColumnHeader } from "@/components/data-table";
import type { FilterKind } from "@/components/data-table";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import type { AttachmentSelection } from "@/hooks/use-attachment-selection";
import { attachmentDownloadUrl, type Attachment } from "@/lib/rs/attachments";
import { AttachmentEncryptedBadge } from "./attachment-encrypted-badge";

type Column = ColumnDef<Attachment, unknown>;

/**
 * What the cells need beyond their row and changes far more often than the columns: the selection on
 * every click. A context rather than column closures, because the selection is computed from the
 * table's displayed rows (see AttachmentFiles) — the columns have to exist before it does.
 */
interface CellContext {
  entity: string;
  id: number;
  selection?: AttachmentSelection;
  busy?: boolean;
}

const AttachmentCellContext = createContext<CellContext | null>(null);

function useCellContext(): CellContext {
  const context = useContext(AttachmentCellContext);
  if (!context) throw new Error("Attachment cell outside AttachmentTable");
  return context;
}

/** Whether the row offers no rename — then its click downloads instead (see AttachmentRow). */
function noRename(attachment: Attachment, readOnly?: boolean): boolean {
  return (
    readOnly === true ||
    attachment.readonly === true ||
    attachment.renameLocked === true
  );
}

/**
 * The files matching every word of `term` (case-insensitive) in name, description or one of the users —
 * the same words HighlightedText marks. All of them client-side: the backend sends the whole list.
 */
export function searchAttachments(
  attachments: Attachment[],
  term: string
): Attachment[] {
  const words = term.trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (words.length === 0) return attachments;
  return attachments.filter((attachment) => {
    const text = [
      attachment.name,
      attachment.description,
      attachment.createdByUser,
      attachment.lastUpdateByUser,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return words.every((word) => text.includes(word));
  });
}

/**
 * The columns of the legacy grid (`UIAttachmentList`), with the description under the file name rather
 * than in a column of its own. Times are shown as the backend formatted them and sorted by their
 * timestamps; the size by its bytes.
 */
export function attachmentColumns(
  t: (key: string) => string,
  readOnly?: boolean
): Column[] {
  const header = (label: string, filterKind?: FilterKind): Column["header"] =>
    function AttachmentColumnHeader({ column, table }) {
      return (
        <DataTableColumnHeader
          column={column}
          table={table}
          filterKind={filterKind}
        >
          {label}
        </DataTableColumnHeader>
      );
    };
  const byTimestamp =
    (key: "created" | "lastUpdate"): Column["sortingFn"] =>
    (a, b) =>
      (a.original[key] ?? "").localeCompare(b.original[key] ?? "");
  const user = (
    key: "createdByUser" | "lastUpdateByUser",
    label: string
  ): Column => ({
    id: key,
    accessorFn: (row) => row[key] ?? "",
    header: header(label, "text"),
    size: 140,
    meta: { label },
    cell: ({ getValue, table }) => (
      <HighlightedText
        text={String(getValue())}
        query={table.options.meta?.highlight}
      />
    ),
  });

  const columns: Column[] = [
    {
      id: "name",
      accessorFn: (row) => row.name,
      header: header(t("attachment.fileName"), "text"),
      size: 320,
      meta: { label: t("attachment.fileName"), wrap: true },
      cell: ({ row, table }) => (
        <NameCell
          attachment={row.original}
          highlight={table.options.meta?.highlight}
        />
      ),
    },
    {
      id: "size",
      accessorFn: (row) => row.size ?? 0,
      header: header(t("attachment.size")),
      size: 90,
      enableColumnFilter: false,
      meta: { label: t("attachment.size"), align: "right" },
      cell: ({ row }) => row.original.sizeHumanReadable ?? "",
    },
    {
      id: "created",
      accessorFn: (row) => row.created ?? "",
      header: header(t("created")),
      size: 150,
      enableColumnFilter: false,
      sortingFn: byTimestamp("created"),
      meta: { label: t("created") },
      cell: ({ row }) => row.original.createdFormatted ?? "",
    },
    user("createdByUser", t("createdBy")),
    {
      id: "lastUpdate",
      accessorFn: (row) => row.lastUpdate ?? "",
      header: header(t("modified")),
      size: 130,
      enableColumnFilter: false,
      sortingFn: byTimestamp("lastUpdate"),
      meta: { label: t("modified") },
      cell: ({ row }) => (
        <span title={row.original.lastUpdateFormatted ?? undefined}>
          {row.original.lastUpdateTimeAgo ?? ""}
        </span>
      ),
    },
    user("lastUpdateByUser", t("modifiedBy")),
    {
      id: "expiryInfo",
      accessorFn: (row) => row.info?.expiryInfo ?? "",
      header: header(t("attachment.expires")),
      size: 130,
      // A translated "in 2 months" doesn't sort; the dates it follows from are the columns above.
      enableSorting: false,
      enableColumnFilter: false,
      meta: { label: t("attachment.expires") },
    },
  ];
  return readOnly
    ? columns
    : [
        {
          id: "select",
          size: 36,
          enableSorting: false,
          enableColumnFilter: false,
          enableResizing: false,
          enableHiding: false,
          header: () => null,
          cell: ({ row }) => <SelectCell attachment={row.original} />,
        },
        ...columns,
      ];
}

/** The checkbox of one file, with Shift ranges over the displayed order (see AttachmentRow). */
function SelectCell({ attachment }: { attachment: Attachment }) {
  const t = useTranslations();
  const { selection, busy } = useCellContext();
  if (!selection) return null;
  const selected = selection.has(attachment.fileId);
  return (
    <div
      className="flex items-center justify-center"
      // The row's click would otherwise open the file as well.
      onClick={(event) => event.stopPropagation()}
    >
      <Checkbox
        checked={selected}
        disabled={busy}
        aria-label={`${t("select._")}: ${attachment.name}`}
        // The click rather than onCheckedChange, which doesn't tell whether Shift was held.
        onClick={(event) => {
          event.preventDefault();
          selection.toggle(attachment.fileId, !selected, event.shiftKey);
        }}
        // Shift+click would otherwise also select the text between the two rows.
        onMouseDown={(event) => event.shiftKey && event.preventDefault()}
      />
    </div>
  );
}

/** The name as the download link, the encrypted badge, and the description below. */
function NameCell({
  attachment,
  highlight,
}: {
  attachment: Attachment;
  highlight?: string;
}) {
  const t = useTranslations();
  const { entity, id } = useCellContext();
  return (
    <div className="min-w-0">
      <div className="flex items-center gap-1.5">
        <a
          href={attachmentDownloadUrl({
            entity,
            id,
            fileId: attachment.fileId,
          })}
          aria-label={`${t("download._")}: ${attachment.name}`}
          // min-w-0: a flex item doesn't shrink below its content, so a name without spaces
          // (Foo_Bar_Baz.pdf) would run into the next columns instead of wrapping (break-words of the cell).
          className="min-w-0 font-medium hover:underline"
          // A download, not the row's own click (which opens the details).
          onClick={(event) => event.stopPropagation()}
        >
          <HighlightedText text={attachment.name} query={highlight} />
        </a>
        {attachment.encrypted && (
          <AttachmentEncryptedBadge attachment={attachment} />
        )}
      </div>
      {attachment.description && (
        <p className="text-[11px] text-muted-foreground">
          <HighlightedText text={attachment.description} query={highlight} />
        </p>
      )}
    </div>
  );
}

interface Props {
  /** Created by AttachmentFiles, which needs its displayed rows for the selection. */
  table: Table<Attachment>;
  entity: string;
  id: number;
  readOnly?: boolean;
  busy?: boolean;
  selection?: AttachmentSelection;
  onEdit: (attachment: Attachment) => void;
  onDelete: (attachment: Attachment) => void;
}

/**
 * The attachments as a table — sortable and filterable per column like the legacy grid
 * (`DynamicAttachmentList.jsx`), for a page whose content *is* the files (a data transfer area). The
 * compact list (AttachmentRow) stays for the attachment sections inside a form.
 *
 * A row click opens the details, or downloads where nothing could be changed in them — as AttachmentRow.
 */
export function AttachmentTable({
  table,
  entity,
  id,
  readOnly,
  busy,
  selection,
  onEdit,
  onDelete,
}: Props) {
  const t = useTranslations();
  return (
    <AttachmentCellContext.Provider value={{ entity, id, selection, busy }}>
      <DataTable<Attachment>
        table={table}
        columns={[]}
        data={[]}
        showPagination={false}
        autoHeight
        emptyState={t("nothingFound")}
        onRowClick={(attachment) => {
          if (busy) return;
          if (noRename(attachment, readOnly)) {
            window.location.assign(
              attachmentDownloadUrl({ entity, id, fileId: attachment.fileId })
            );
          } else {
            onEdit(attachment);
          }
        }}
        rowActions={
          readOnly
            ? undefined
            : (attachment) =>
                attachment.readonly === true ? null : (
                  <>
                    {attachment.renameLocked !== true && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-7 text-muted-foreground"
                        disabled={busy}
                        aria-label={`${t("edit")}: ${attachment.name}`}
                        onClick={() => onEdit(attachment)}
                      >
                        <HugeiconsIcon icon={Edit02Icon} size={13} />
                      </Button>
                    )}
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="size-7 text-muted-foreground hover:text-destructive"
                      disabled={busy}
                      aria-label={`${t("delete")}: ${attachment.name}`}
                      onClick={() => onDelete(attachment)}
                    >
                      <HugeiconsIcon icon={Delete01Icon} size={13} />
                    </Button>
                  </>
                )
        }
      />
    </AttachmentCellContext.Provider>
  );
}
