"use client";

import { useEffect, useState } from "react";
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
import { bookEditSchema } from "./book-edit-schema";
import { emptyBookValues, toFormValues } from "./book-edit-values";
import { useBookDetail, useSaveBook } from "./use-book-detail";
import type { BookDetail } from "../types";

interface Props {
  /** null adds a new book: nothing is fetched and the form starts out blank. */
  bookId: number | null;
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
  // A new book has nothing to load — the hook stays disabled for id null.
  const { data: book, isLoading, isError } = useBookDetail(bookId);
  const saveMutation = useSaveBook(bookId);

  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);

  const form = useForm({
    defaultValues: book ? toFormValues(book) : emptyBookValues(),
    validators: { onSubmit: bookEditSchema },
    onSubmit: async ({ value }) => {
      const saved = await saveMutation.mutateAsync(value as BookDetail);
      form.reset(toFormValues(saved));
      setLastSavedAt(new Date());
      toast.success(t("saved"));
      // The id only exists after the first save, so the url has to follow it.
      if (bookId == null && saved.id != null) {
        router.replace(`/books/${saved.id}`);
      }
    },
  });

  useEffect(() => {
    if (book) form.reset(toFormValues(book));
  }, [book, form]);

  const isDirty = useStore(form.store, (s) => s.isDirty);
  const isSubmitting = useStore(form.store, (s) => s.isSubmitting);

  if (isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
        {t("loading")}
      </div>
    );
  }
  if (bookId != null && (isError || !book)) {
    return (
      <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
        {t("notFound")}
      </div>
    );
  }

  // A book that doesn't exist yet has neither a loan nor a change history.
  const tabs = [
    { id: "general", label: t("tabs.general") },
    ...(book
      ? [
          { id: "loan", label: t("tabs.loan") },
          { id: "notes", label: t("tabs.notes") },
          { id: "history", label: t("tabs.history") },
        ]
      : [{ id: "notes", label: t("tabs.notes") }]),
  ];

  const sections = book
    ? [
        <AllgemeinSection key="general" />,
        <AusleiheSection key="loan" book={book} />,
        <NotizenSection key="notes" />,
        <VerlaufSection key="history" bookId={book.id!} />,
      ]
    : [<AllgemeinSection key="general" />, <NotizenSection key="notes" />];

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
              title={book?.title ?? t("newTitle")}
              lendOut={book?.lendOutBy != null}
            />
          }
          tabs={tabs}
          sections={sections}
          actions={
            <BookEditActions
              onCancel={() => router.push("/books")}
              // Nothing to delete before the first save.
              onDelete={
                book ? () => toast.info(t("actions.deleteTodo")) : undefined
              }
              isSaving={isSubmitting}
              isDirty={isDirty}
              lastSavedLabel={
                lastSavedAt ? formatTime(lastSavedAt) : (book?.created ?? null)
              }
            />
          }
        />
      </form>
    </BookEditFormProvider>
  );
}
