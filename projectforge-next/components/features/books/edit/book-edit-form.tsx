"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { EditPageShell } from "@/components/shared/edit-page-shell";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { EntityEditActions } from "@/components/shared/edit/entity-edit-actions";
import { EntityDeleteButton } from "@/components/shared/edit/entity-delete-button";
import {
  useDeleteEntity,
  useEntityDetail,
  useSaveEntity,
} from "@/hooks/use-entity-detail";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useLegacyEditUrl } from "@/hooks/use-legacy-edit-url";
import { BOOK_METADATA } from "@/lib/metadata/book.generated";
import { BookEditHeader } from "./book-edit-header";
import { GeneralSection } from "./sections/general-section";
import { LoanSection } from "./sections/loan-section";
import { NotesSection } from "./sections/notes-section";
import { AttachmentSection } from "./sections/attachment-section";
import { bookTabs } from "../book-tabs";
import {
  bookEditSchema,
  BOOK_EDIT_FIELDS,
  type BookEditValues,
} from "./book-edit-schema";
import { emptyBookValues, toFormValues } from "./book-edit-values";
import {
  BOOKS_LIST_QUERY_KEY,
  BOOK_ENTITY,
  useLendOutBook,
  useReturnBook,
} from "./use-book-detail";
import type { BookDetail } from "../types";

const LIST_ROUTE = "/books";
const WRITE_OPTIONS = { listQueryKey: BOOKS_LIST_QUERY_KEY };

interface Props {
  /** null adds a new book: nothing is fetched and the form starts out blank. */
  bookId: number | null;
}

export function BookEditForm({ bookId }: Props) {
  const router = useRouter();
  const t = useTranslations("books.edit");
  const tCommon = useTranslations();
  // A new book has nothing to load — the hook stays disabled for id null.
  const {
    data: book,
    isLoading,
    isError,
  } = useEntityDetail<BookDetail>(BOOK_ENTITY, bookId);
  const saveMutation = useSaveEntity<BookDetail>(BOOK_ENTITY, WRITE_OPTIONS);
  const deleteMutation = useDeleteEntity<BookDetail>(
    BOOK_ENTITY,
    WRITE_OPTIONS
  );
  const lendOutMutation = useLendOutBook();
  const returnMutation = useReturnBook();
  const legacyUrl = useLegacyEditUrl(BOOK_ENTITY, bookId);

  // All three writes go through the form's submit — same validation, same values, same 406 handling
  // — because the loan endpoints save the whole book too (see lib/rs/submit-meta.ts).
  const save = useCallback(
    (values: BookEditValues, meta: { action: string }) => {
      const mutation =
        meta.action === "lendOut"
          ? lendOutMutation
          : meta.action === "returnBook"
            ? returnMutation
            : saveMutation;
      // The form's values are the DTO the backend expects: every field of BookEditValues is one of
      // BookDetail (see book-edit-schema.ts), the type only differs in what it makes optional.
      return mutation.mutateAsync(values as BookDetail);
    },
    [lendOutMutation, returnMutation, saveMutation]
  );

  const { form, isDirty, isSubmitting } = useEntityEditForm<
    BookEditValues,
    BookDetail
  >({
    data: book,
    toFormValues,
    defaultValues: emptyBookValues(),
    schema: bookEditSchema,
    fieldNames: BOOK_EDIT_FIELDS,
    listRoute: LIST_ROUTE,
    savedMessage: t("saved"),
    save,
  });

  async function runDelete(): Promise<void> {
    if (!book) return;
    const result = await deleteMutation.mutateAsync(book);
    if (result.kind === "validationErrors") {
      // Nothing was deleted; the server explains why (e.g. the book is still lent out).
      result.validationErrors.forEach((error) => toast.error(error.message));
      return;
    }
    toast.success(tCommon("message.successfullChanged"));
    router.push(LIST_ROUTE);
  }

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
    <EntityEditFormProvider value={{ form, metadata: BOOK_METADATA }}>
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
              legacyUrl={legacyUrl}
            />
          }
          tabs={tabs}
          sections={sections}
          actions={
            <EntityEditActions
              onCancel={() => router.push(LIST_ROUTE)}
              // Nothing to delete before the first save.
              deleteAction={
                book ? (
                  <EntityDeleteButton
                    onDelete={runDelete}
                    disabled={isSubmitting || deleteMutation.isPending}
                  />
                ) : undefined
              }
              isSaving={isSubmitting}
              isDirty={isDirty}
              // Saving leaves the page, so there is never a "just saved" moment to show here —
              // what remains is when the book was created.
              lastSaved={book?.created ?? null}
            />
          }
        />
      </form>
    </EntityEditFormProvider>
  );
}
