import { notFound } from "next/navigation";
import { PageShell } from "@/components/shared/page-shell";
import { BookEditForm } from "@/components/features/books/edit/book-edit-form";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function BookEditPage({ params }: Props) {
  const { id: raw } = await params;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <BookEditForm bookId={id} />
    </PageShell>
  );
}
