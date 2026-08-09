"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { BookHistoryPage } from "@/components/features/books/history/book-history-page";

// Reads the book id from the URL at runtime rather than from a server-provided route param, so any
// id works under the static export (see page.tsx and use-route-params.ts).
export function BookHistoryPageClient() {
  const raw = useRouteParams<{ id: string }>("/books/[id]/history")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  // A book that isn't saved yet ("new") has no history to show.
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <BookHistoryPage bookId={id} />
    </PageShell>
  );
}
