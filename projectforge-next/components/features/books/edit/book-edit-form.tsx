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
import { BookDeleteButton } from "./book-delete-button";
import { AllgemeinSection } from "./sections/allgemein-section";
import { AusleiheSection } from "./sections/ausleihe-section";
import { NotizenSection } from "./sections/notizen-section";
import { bookTabs } from "../book-tabs";
import { bookEditSchema, BOOK_EDIT_FIELDS } from "./book-edit-schema";
import { emptyBookValues, toFormValues } from "./book-edit-values";
import { useBookDetail, useSaveBook } from "./use-book-detail";
import { applyServerValidationErrors } from "@/lib/validation/server-errors";
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
  const tCommon = useTranslations();
  // A new book has nothing to load — the hook stays disabled for id null.
  const { data: book, isLoading, isError } = useBookDetail(bookId);
  const saveMutation = useSaveBook();

  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);

  const form = useForm({
    defaultValues: book ? toFormValues(book) : emptyBookValues(),
    validators: { onSubmit: bookEditSchema },
    onSubmit: async ({ value }) => {
      const result = await saveMutation.mutateAsync(value as BookDetail);
      if (result.kind === "validationErrors") {
        // The server rejected the entity: its rules are the authority, ours only anticipate them.
        const { unassigned } = applyServerValidationErrors(
          form,
          result.validationErrors,
          BOOK_EDIT_FIELDS
        );
        // Anything the form can't show next to a field would be invisible otherwise.
        unassigned.forEach((message) => toast.error(message));
        if (unassigned.length === 0)
          toast.error(tCommon("validation.error.generic"));
        return;
      }
      setLastSavedAt(new Date());
      toast.success(t("saved"));
      // The id only exists after the first save, so the url has to follow it. The reloaded book
      // then resets the form (see the effect below) — with the values the server actually stored.
      if (bookId == null && result.id != null) {
        router.replace(`/books/${result.id}`);
      } else {
        form.reset(value);
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

  // The history has a page of its own (see bookTabs), so it is not among the sections here.
  const tabs = bookTabs(book?.id ?? null, (key) => t(`tabs.${key}`), true);

  const sections = book
    ? [
        <AllgemeinSection key="general" />,
        <AusleiheSection key="loan" book={book} />,
        <NotizenSection key="notes" />,
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
              deleteAction={
                book ? (
                  <BookDeleteButton book={book} disabled={isSubmitting} />
                ) : undefined
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
