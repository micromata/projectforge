"use client";

import { useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import {
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { SearchInput } from "@/components/shared/list/search-input";
import { useAttachmentMutations } from "@/hooks/use-attachments";
import { useAttachmentSelection } from "@/hooks/use-attachment-selection";
import { useAttachmentEncryption } from "@/hooks/use-attachment-encryption";
import type { Attachment, AttachmentWriteResult } from "@/lib/rs/attachments";
import { AttachmentEditDialog } from "./attachment-edit-dialog";
import { AttachmentRow } from "./attachment-row";
import {
  AttachmentTable,
  attachmentColumns,
  searchAttachments,
} from "./attachment-table";
import { AttachmentToolbar } from "./attachment-toolbar";

interface Props {
  /** The stored attachments, in the order the backend returned them. */
  attachments: Attachment[];
  entity: string;
  id: number;
  /** Only downloads are offered — no selection, no rename, no delete. */
  readOnly?: boolean;
  /**
   * Adds the "add files" button to the toolbar — the embedded variant, where no permanent drop area
   * offers the click (see AttachmentList).
   */
  onFiles?: (files: File[]) => void;
  /** Called after a rename or a delete went through — see AttachmentList, which passes it on. */
  onChanged?: () => void;
  /** See AttachmentList's `layout`. */
  layout?: "list" | "table";
  /** See AttachmentList's `columnStates`. */
  columnStates?: string;
}

/**
 * The stored attachments of an entity: rename, delete, encrypt, and the actions on a whole selection
 * (download as one ZIP, delete at once — see AttachmentToolbar).
 *
 * Split from AttachmentList so that one keeps to the uploads and the query while this one holds the
 * selection and both dialogs.
 *
 * The table layout with `columnStates` is rendered once the stored column state has arrived (or failed), so
 * the columns don't jump from the default layout to the user's one.
 */
export function AttachmentFiles(props: Props) {
  const prefs = props.layout === "table" ? props.columnStates : undefined;
  const stored = useStoredColumnState(prefs);
  // Without prefs the query is disabled — and a disabled query stays pending.
  if (prefs && stored.isPending) return null;
  return <LoadedAttachmentFiles {...props} storedState={stored.data ?? {}} />;
}

function LoadedAttachmentFiles({
  attachments,
  entity,
  id,
  readOnly,
  onFiles,
  onChanged,
  layout = "list",
  columnStates,
  storedState,
}: Props & { storedState: ColumnState }) {
  const t = useTranslations();
  const { rename, remove, removeMany, encrypt, testDecryption } =
    useAttachmentMutations(entity, id);
  const asTable = layout === "table";
  const [search, setSearch] = useState("");
  const searched = useMemo(
    () => (asTable ? searchAttachments(attachments, search) : []),
    [asTable, attachments, search]
  );
  const columns = useMemo(() => attachmentColumns(t, readOnly), [t, readOnly]);
  // Created here rather than in AttachmentTable: the selection works on the rows the table shows —
  // searched, filtered by column and sorted — so a Shift range follows the order on screen, "select all"
  // picks what is visible, and a file a filter hides drops out of the selection instead of being deleted
  // unseen. Unpaged (manualPagination), so the row model is every displayed row.
  const state = useTableState({ restoredState: storedState });
  const table = useDataTable<Attachment>({
    columns,
    data: searched,
    sorting: state.sorting,
    onSortingChange: state.setSorting,
    columnFilters: state.columnFilters,
    onColumnFiltersChange: state.setColumnFilters,
    columnVisibility: state.columnVisibility,
    onColumnVisibilityChange: state.setColumnVisibility,
    columnPinning: state.columnPinning,
    onColumnPinningChange: state.setColumnPinning,
    columnSizing: state.columnSizing,
    onColumnSizingChange: state.setColumnSizing,
    columnOrder: state.columnOrder,
    onColumnOrderChange: state.setColumnOrder,
    // The checkbox leads the table whatever the stored layout says.
    lockedColumnIds: ["select"],
    enableColumnFilters: true,
    enableColumnResizing: asTable,
    manualPagination: true,
    getRowId: (attachment) => attachment.fileId,
    highlight: search,
  });
  const rows = table.getRowModel().rows;
  const displayed = useMemo(
    () => (asTable ? rows.map((row) => row.original) : attachments),
    [asTable, rows, attachments]
  );
  useColumnStatePersistence(asTable ? columnStates : undefined, {
    sorting: state.sorting,
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  });
  // Back to the columns as declared; the persistence then stores the empty state.
  const resetColumns = () => {
    state.setSorting([]);
    state.setColumnVisibility({});
    state.setColumnPinning({});
    state.setColumnSizing({});
    state.setColumnOrder([]);
    state.setColumnFilters([]);
  };
  const searchField = asTable && (
    <div className="flex items-center gap-2">
      <div className="relative w-64">
        <SearchInput value={search} onChange={setSearch} />
      </div>
      <DataTableColumnPanel table={table} onReset={resetColumns} />
    </div>
  );
  const selection = useAttachmentSelection(displayed);
  const [editing, setEditing] = useState<Attachment | null>(null);
  /** The files the open confirmation would delete — one row's, or a whole selection's. */
  const [deleting, setDeleting] = useState<Attachment[]>([]);

  const busy =
    rename.isPending ||
    remove.isPending ||
    removeMany.isPending ||
    encrypt.isPending;

  function report(result: AttachmentWriteResult): void {
    if (result.kind === "rejected") {
      toast.error(result.message || t("validation.error.generic"));
    } else {
      onChanged?.();
    }
  }

  async function saveEdit(name: string, description: string) {
    if (!editing) return;
    try {
      report(
        await rename.mutateAsync({ fileId: editing.fileId, name, description })
      );
      setEditing(null);
    } catch {
      toast.error(t("validation.error.generic"));
    }
  }

  const encryption = useAttachmentEncryption(
    { encrypt, testDecryption },
    (result) => {
      report(result);
      if (result.kind === "ok") setEditing(null);
    }
  );

  async function confirmDelete() {
    const files = deleting;
    setDeleting([]);
    if (files.length === 0) return;
    // Deleted from its own details: those show a file that no longer exists.
    if (editing && files.some((file) => file.fileId === editing.fileId)) {
      setEditing(null);
    }
    try {
      // One call for a selection, so the cache is written once (see deleteAttachments).
      report(
        files.length === 1
          ? await remove.mutateAsync(files[0].fileId)
          : await removeMany.mutateAsync(files.map((file) => file.fileId))
      );
      // Whatever was deleted can no longer be picked; the rest of the selection stays.
      selection.clear();
    } catch {
      toast.error(t("validation.error.generic"));
    }
  }

  return (
    <>
      {!readOnly && (
        <AttachmentToolbar
          attachments={attachments}
          selection={selection}
          entity={entity}
          id={id}
          busy={busy}
          onDeleteSelected={setDeleting}
          onFiles={onFiles}
          visible={displayed}
          search={searchField}
        />
      )}
      {/* Without a toolbar (read-only) the search stands on its own. */}
      {readOnly && searchField}
      {asTable ? (
        <AttachmentTable
          table={table}
          entity={entity}
          id={id}
          readOnly={readOnly}
          busy={busy}
          selection={readOnly ? undefined : selection}
          onEdit={setEditing}
          onDelete={(attachment) => setDeleting([attachment])}
        />
      ) : (
        <ul className="flex flex-col">
          {attachments.map((attachment) => (
            <AttachmentRow
              key={attachment.fileId}
              attachment={attachment}
              entity={entity}
              id={id}
              busy={busy}
              readOnly={readOnly}
              selected={selection.has(attachment.fileId)}
              onSelectedChange={
                readOnly
                  ? undefined
                  : (on, range) =>
                      selection.toggle(attachment.fileId, on, range)
              }
              onEdit={setEditing}
              onDelete={(attachment) => setDeleting([attachment])}
            />
          ))}
        </ul>
      )}

      {editing && (
        <AttachmentEditDialog
          attachment={editing}
          entity={entity}
          id={id}
          saving={rename.isPending}
          busy={busy}
          encrypting={encryption.pending}
          onSave={(name, description) => void saveEdit(name, description)}
          onDelete={() => setDeleting([editing])}
          onEncrypt={(password, mode) =>
            encryption.encryptFile(editing.fileId, password, mode)
          }
          onTestDecryption={(password) =>
            encryption.testFile(editing.fileId, password)
          }
          onClose={() => setEditing(null)}
        />
      )}
      {deleting.length > 0 && (
        <ConfirmDialog
          open
          onOpenChange={(open) => !open && setDeleting([])}
          title={t("delete")}
          description={
            // The final question in both cases, not markAsDeletedQuestion: the JCR keeps no history
            // of removed files, so this cannot be undone.
            deleting.length === 1
              ? t("question.deleteQuestion")
              : t("file.upload.deleteSelected.confirm")
          }
          confirmLabel={t("delete")}
          destructive
          onConfirm={() => void confirmDelete()}
        />
      )}
    </>
  );
}
