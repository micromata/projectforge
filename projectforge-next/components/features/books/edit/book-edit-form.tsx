"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useStore, useForm } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { BookEditFormProvider } from "./book-edit-context";
import { BookEditHeader } from "./book-edit-header";
import { BookEditActions } from "./book-edit-actions";
import { BookDeleteButton } from "./book-delete-button";
import { GeneralSection } from "./sections/general-section";
import { LoanSection } from "./sections/loan-section";
import { NotesSection } from "./sections/notes-section";
import { AttachmentSection } from "./sections/attachment-section";
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

export function BookEditForm({ bookId }: Props) {
  const router = useRouter();
  const t = useTranslations("books.edit");
  const tCommon = useTranslations();
  // A new book has nothing to load — the hook stays disabled for id null.
  const { data: book, isLoading, isError } = useBookDetail(bookId);
  const saveMutation = useSaveBook();

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
      toast.success(t("saved"));
      // Back to the list, which is where the backend points too (its ResponseAction is a REDIRECT
      // to /next/books) and what deleting does. The form is reset first so leaving it doesn't look
      // like unsaved changes; the list refetches on its own, the caches having been invalidated.
      form.reset(value);
      router.push("/books");
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
  const tabs = bookTabs(book?.id ?? null, tCommon, true);

  // All sections render for a new book too: they are the book's own fields, and the legacy page
  // hid the loan block only because its lend-out action needs a saved entity (BookPagesRest), not
  // because the fields don't exist. Attachments are the exception — they need a persisted id to
  // hang off, so the section says so itself instead of disappearing.
  // The order must match `tabs`: EditPageShell couples the anchor tabs to it positionally.
  const sections = [
    <GeneralSection key="general" />,
    <LoanSection key="loan" />,
    <NotesSection key="notes" />,
    <AttachmentSection key="attachments" bookId={book?.id ?? null} />,
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
              // Saving leaves the page, so there is never a "just saved" moment to show here —
              // what remains is when the book was created.
              lastSavedLabel={book?.created ?? null}
            />
          }
        />
      </form>
    </BookEditFormProvider>
  );
}
