"use client";

import { useTranslations } from "next-intl";
import { StatusBadge } from "./status-badge";

/**
 * Whether the book is lent out, as a badge — beside the heading of the edit page and its history.
 *
 * Its own component because the texts are ours: BookDO has a `status` ("vorhanden", "vermisst"), but
 * no word for "currently with someone" — the legacy page said it with a coloured pill too.
 */
export function LoanStatus({ lendOut }: { lendOut: boolean }) {
  const t = useTranslations("books.edit.status");
  return (
    <StatusBadge
      lendOut={lendOut}
      label={t(lendOut ? "loaned" : "available")}
      variant="pill"
    />
  );
}
