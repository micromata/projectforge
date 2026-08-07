"use client";

import { notFound, useParams } from "next/navigation";
import { PageShell } from "@/components/shared/page-shell";
import { BookHistoryPage } from "@/components/features/books/history/book-history-page";

// Reads the book id from the URL at runtime rather than from a server-provided route param, so any
// id works under the static export (see page.tsx).
export function BookHistoryPageClient() {
  const { id: raw } = useParams<{ id: string }>();
  const id = Number(raw);
  // A book that isn't saved yet ("new") has no history to show.
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <BookHistoryPage bookId={id} />
    </PageShell>
  );
}
