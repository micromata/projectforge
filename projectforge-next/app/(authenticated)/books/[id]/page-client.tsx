"use client";

import { notFound, useParams } from "next/navigation";
import { PageShell } from "@/components/shared/page-shell";
import { BookEditForm } from "@/components/features/books/edit/book-edit-form";

// Reads the book id from the URL at runtime rather than from a server-provided
// route param, so any id works under the static export (see page.tsx).
export function BookEditPageClient() {
  const { id: raw } = useParams<{ id: string }>();
  // "new" adds a book — the same form, just without an id to load.
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <BookEditForm bookId={isNew ? null : id} />
    </PageShell>
  );
}
