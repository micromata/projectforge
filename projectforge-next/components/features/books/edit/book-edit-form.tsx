"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useStore, useForm } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { BookEditFormProvider } from "./book-edit-context";
import { BookEditHeader } from "./book-edit-header";
import { BookEditActions } from "./book-edit-actions";
import { AllgemeinSection } from "./sections/allgemein-section";
import { AusleiheSection } from "./sections/ausleihe-section";
import { NotizenSection } from "./sections/notizen-section";
import { VerlaufSection } from "./sections/verlauf-section";
import { bookEditSchema, type BookEditValues } from "./book-edit-schema";
import { useBookDetail, useSaveBook } from "./use-book-detail";
import type { BookDetail } from "../types";

interface Props {
  bookId: number;
}

function toFormValues(book: BookDetail): BookEditValues {
  return {
    id: book.id,
    title: book.title,
    authors: book.authors ?? "",
    signature: book.signature,
    yearOfPublishing: book.yearOfPublishing,
    publisher: book.publisher,
    editor: book.editor,
    isbn: book.isbn,
    keywords: book.keywords,
    abstractText: book.abstractText,
    comment: book.comment,
    status: book.status,
    type: book.type,
    lendOutBy: book.lendOutBy,
    lendOutDate: book.lendOutDate,
    lendOutComment: book.lendOutComment,
    created: book.created,
  };
}

function formatTime(date: Date): string {
  return date.toLocaleTimeString(undefined, {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function BookEditForm({ bookId }: Props) {
  const router = useRouter();
  const t = useTranslations("books.edit");
  const { data: book, isLoading, isError } = useBookDetail(bookId);
  const saveMutation = useSaveBook(bookId);

  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);

  const form = useForm({
    defaultValues: book
      ? toFormValues(book)
      : ({
          id: bookId,
          title: "",
          authors: "",
          signature: null,
          yearOfPublishing: null,
          publisher: null,
          editor: null,
          isbn: null,
          keywords: null,
          abstractText: null,
          comment: null,
          status: null,
          type: null,
          lendOutBy: null,
          lendOutDate: null,
          lendOutComment: null,
          created: null,
        } satisfies BookEditValues),
    validators: { onSubmit: bookEditSchema },
    onSubmit: async ({ value }) => {
      const payload: BookDetail = {
        ...value,
        authors: value.authors,
      };
      await saveMutation.mutateAsync(payload);
      form.reset(value);
      setLastSavedAt(new Date());
      toast.success("Buch gespeichert");
    },
  });

  useEffect(() => {
    if (book) form.reset(toFormValues(book));
  }, [book, form]);

  const isDirty = useStore(form.store, (s) => s.isDirty);
  const isSubmitting = useStore(form.store, (s) => s.isSubmitting);

  const lastSavedLabel = useMemo(() => {
    if (!lastSavedAt) return null;
    return formatTime(lastSavedAt);
  }, [lastSavedAt]);

  if (isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
        Lade Buch…
      </div>
    );
  }
  if (isError || !book) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
        Buch nicht gefunden.
      </div>
    );
  }

  const tabs = [
    { id: "general", label: t("tabs.general") },
    { id: "loan", label: t("tabs.loan") },
    { id: "notes", label: t("tabs.notes") },
    { id: "history", label: t("tabs.history") },
  ];

  return (
    <BookEditFormProvider value={form}>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void form.handleSubmit();
        }}
        className="flex min-w-0 flex-1 flex-col overflow-hidden"
      >
        <EditPageShell
          header={
            <BookEditHeader
              title={book.title}
              lendOut={book.lendOutBy != null}
            />
          }
          tabs={tabs}
          sections={[
            <AllgemeinSection key="general" />,
            <AusleiheSection key="loan" book={book} />,
            <NotizenSection key="notes" />,
            <VerlaufSection key="history" bookId={bookId} />,
          ]}
          actions={
            <BookEditActions
              onCancel={() => router.push("/books")}
              onDelete={() =>
                toast.info("Löschen ist noch nicht implementiert")
              }
              isSaving={isSubmitting}
              isDirty={isDirty}
              lastSavedLabel={lastSavedLabel ?? book.created}
            />
          }
        />
      </form>
    </BookEditFormProvider>
  );
}
