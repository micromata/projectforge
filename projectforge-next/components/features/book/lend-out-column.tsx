"use client";

import { useTranslations } from "next-intl";
import type { ComputedColumn } from "@/lib/page-def/types";
import { StatusBadge } from "./status-badge";
import type { BookListRow, UserRef } from "./types";

/**
 * Who has the book, as the coloured badge of the legacy list — and "vorhanden" where nobody has it,
 * so the column reads as a state rather than as a mostly empty name column.
 *
 * A computed column: `lendOutBy` is a user, and what the cell shows is that user's display name,
 * which is also what the backend sorts and filters by.
 */
export const lendOutColumn: ComputedColumn<BookListRow> = {
  id: "lendOutBy",
  labelKey: "book.lendOutBy",
  accessor: (row) => row.lendOutBy?.displayName ?? "",
  size: 140,
  cell: ({ row }) => <LendOutCell borrower={row.original.lendOutBy} />,
};

function LendOutCell({ borrower }: { borrower?: UserRef | null }) {
  const t = useTranslations();
  return borrower ? (
    <StatusBadge lendOut label={borrower.displayName} />
  ) : (
    <StatusBadge lendOut={false} label={t("book.status.present")} />
  );
}
