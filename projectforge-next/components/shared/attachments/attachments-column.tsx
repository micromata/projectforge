"use client";

import { useTranslations } from "next-intl";
import type { ComputedColumn } from "@/lib/page-def/types";
import { AttachmentsSummary } from "./attachments-summary";

/** What a list row of an entity with attachments carries (AttachmentsInfo). */
export interface RowWithAttachments {
  attachmentsCounter?: number | null;
  attachmentsSizeFormatted?: string | null;
}

/**
 * The attachments column, declarable by every entity that has them: how many there are and how much
 * they weigh.
 *
 * Not a field of the entity — `attachmentsSize` is the property the backend sorts by, while what the
 * cell shows is the string it formatted ("5,2MB (3)"); sorting on that would put 900KB after 1,1MB.
 * Hence a computed column with the sort property as its id.
 *
 * No filter of its own: "has attachments" is a filter the backend offers on the entity
 * (AttachmentsFilterSupport), and the column header would only offer a second, weaker one.
 */
export function attachmentsColumn<
  Row extends RowWithAttachments,
>(): ComputedColumn<Row> {
  return {
    id: "attachmentsSize",
    labelKey: "attachments._",
    // "Anh." — the column is as wide as its icon and a size.
    headerLabelKey: "attachments.short",
    accessor: (row) => row.attachmentsSizeFormatted ?? "",
    size: 90,
    filterKind: null,
    cell: ({ row }) => <AttachmentsCell row={row.original} />,
  };
}

function AttachmentsCell({ row }: { row: RowWithAttachments }) {
  const t = useTranslations();
  return (
    <AttachmentsSummary
      count={row.attachmentsCounter}
      formatted={row.attachmentsSizeFormatted}
      label={t("attachments._")}
    />
  );
}
