import type { EditPageTab } from "@/components/shared/edit-page-tabs";

/** Tab titles of a book, from `books.edit.tabs.*`. */
export type BookTabId = "general" | "loan" | "notes" | "history";

/**
 * The tabs of a book, shared by the edit form and the history page so both show the same bar.
 *
 * The form's three tabs are anchors into its scroll column, the history is a page of its own —
 * that keeps its (possibly long) history from loading with the form. Seen *from* the history page
 * the form tabs are links back to the book, which is what `onFormPage: false` turns them into.
 *
 * @param bookId null for a book that isn't saved yet: it has neither a loan nor a history.
 * @param label Translator for `books.edit.tabs.*`.
 * @param onFormPage Whether the tabs are rendered on the form itself.
 */
export function bookTabs(
  bookId: number | null,
  label: (key: BookTabId) => string,
  onFormPage: boolean
): EditPageTab[] {
  // The form's own tabs scroll; from elsewhere they navigate back to it.
  const formHref =
    bookId != null && !onFormPage ? `/books/${bookId}` : undefined;
  const formTab = (id: BookTabId) => ({ id, label: label(id), href: formHref });

  if (bookId == null) {
    return [formTab("general"), formTab("notes")];
  }
  return [
    formTab("general"),
    formTab("loan"),
    formTab("notes"),
    {
      id: "history",
      label: label("history"),
      href: `/books/${bookId}/history`,
    },
  ];
}
