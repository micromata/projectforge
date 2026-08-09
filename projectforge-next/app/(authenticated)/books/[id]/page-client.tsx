"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { BookEditForm } from "@/components/features/books/edit/book-edit-form";

// Reads the book id from the URL at runtime rather than from a server-provided
// route param, so any id works under the static export (see page.tsx and use-route-params.ts).
export function BookEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/books/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
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
