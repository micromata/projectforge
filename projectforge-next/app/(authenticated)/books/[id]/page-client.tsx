"use client";

import { notFound, useParams } from "next/navigation";
import { PageShell } from "@/components/shared/page-shell";
import { BookEditForm } from "@/components/features/books/edit/book-edit-form";

// Reads the book id from the URL at runtime rather than from a server-provided
// route param, so any id works under the static export (see page.tsx).
export function BookEditPageClient() {
  const { id: raw } = useParams<{ id: string }>();
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <BookEditForm bookId={id} />
    </PageShell>
  );
}
