"use client";

import { useCallback, useMemo, useRef, useState } from "react";
import type { Attachment } from "@/lib/rs/attachments";

/** Which attachments of a list the user picked, for the actions that work on several at once. */
export interface AttachmentSelection {
  /** The picked files in list order — that is the order they end up in the ZIP. */
  selected: Attachment[];
  has: (fileId: string) => boolean;
  /**
   * Picks or drops one file. With `range` (Shift held), every file from the one toggled last up to this
   * one follows it — see [toggledRange].
   */
  toggle: (fileId: string, selected: boolean, range?: boolean) => void;
  /** Picks or drops every file currently in the list. */
  setAll: (selected: boolean) => void;
  clear: () => void;
}

/**
 * Multi-selection over an attachment list (download several as a ZIP, delete several at once).
 *
 * The state is a set of `fileId`s, not of row indices: a delete or a parallel upload reorders the
 * list, and an index-keyed selection would then follow whichever file moved into that position —
 * the bug the legacy grid needed `resetRowSelection()` for. Ids the list no longer contains are
 * filtered out here rather than removed on change, so nothing has to notice a file disappearing.
 *
 * Shift+click picks a range, as in the list's selection mode (useRowSelection) — but adding to the
 * picked files rather than replacing them: these are checkboxes, which only ever change what they
 * are clicked on.
 */
export function useAttachmentSelection(
  attachments: Attachment[]
): AttachmentSelection {
  const [ids, setIds] = useState<ReadonlySet<string>>(() => new Set());
  /** Where a Shift range starts: the file toggled last. */
  const anchor = useRef<string | null>(null);

  const selected = useMemo(
    () => attachments.filter((attachment) => ids.has(attachment.fileId)),
    [attachments, ids]
  );

  const has = useCallback((fileId: string) => ids.has(fileId), [ids]);

  const toggle = useCallback(
    (fileId: string, on: boolean, range?: boolean) => {
      const order = attachments.map((attachment) => attachment.fileId);
      const from = range ? anchor.current : null;
      anchor.current = fileId;
      setIds((current) => toggledRange(current, order, from, fileId, on));
    },
    [attachments]
  );

  const setAll = useCallback(
    (on: boolean) => {
      setIds(
        on
          ? new Set(attachments.map((attachment) => attachment.fileId))
          : new Set()
      );
    },
    [attachments]
  );

  const clear = useCallback(() => {
    setIds(new Set());
    anchor.current = null;
  }, []);

  return { selected, has, toggle, setAll, clear };
}

/**
 * The selection after toggling `target` to `on`, together with every file between `anchor` and it
 * (both included, in list order). Without an anchor, or with one no longer in the list (deleted
 * meanwhile), only `target` changes.
 */
export function toggledRange(
  current: ReadonlySet<string>,
  order: string[],
  anchor: string | null,
  target: string,
  on: boolean
): ReadonlySet<string> {
  const from = anchor === null ? -1 : order.indexOf(anchor);
  const to = order.indexOf(target);
  const span =
    from < 0 || to < 0
      ? [target]
      : order.slice(Math.min(from, to), Math.max(from, to) + 1);
  const next = new Set(current);
  for (const fileId of span) {
    if (on) next.add(fileId);
    else next.delete(fileId);
  }
  return next;
}
