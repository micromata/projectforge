"use client";

import { useCallback, useMemo, useState } from "react";
import type { Attachment } from "@/lib/rs/attachments";

/** Which attachments of a list the user picked, for the actions that work on several at once. */
export interface AttachmentSelection {
  /** The picked files in list order — that is the order they end up in the ZIP. */
  selected: Attachment[];
  has: (fileId: string) => boolean;
  toggle: (fileId: string, selected: boolean) => void;
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
 */
export function useAttachmentSelection(
  attachments: Attachment[]
): AttachmentSelection {
  const [ids, setIds] = useState<ReadonlySet<string>>(() => new Set());

  const selected = useMemo(
    () => attachments.filter((attachment) => ids.has(attachment.fileId)),
    [attachments, ids]
  );

  const has = useCallback((fileId: string) => ids.has(fileId), [ids]);

  const toggle = useCallback((fileId: string, on: boolean) => {
    setIds((current) => {
      const next = new Set(current);
      if (on) next.add(fileId);
      else next.delete(fileId);
      return next;
    });
  }, []);

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

  const clear = useCallback(() => setIds(new Set()), []);

  return { selected, has, toggle, setAll, clear };
}
