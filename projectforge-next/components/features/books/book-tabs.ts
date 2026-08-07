import type { EditPageTab } from "@/components/shared/edit-page-tabs";

export type BookTabId =
  | "general"
  | "loan"
  | "notes"
  | "attachments"
  | "history";

/**
 * The message key of every tab title.
 *
 * The four groupings of a book's own fields have no backend counterpart, so their titles are ours
 * (`books.edit.tabs.*`); "Anhänge" has one — it is the title BookPagesRest gives the attachment
 * fieldset — so that key is reused instead of writing the word a second time.
 */
const TAB_TITLES: Record<BookTabId, string> = {
  general: "books.edit.tabs.general",
  loan: "books.edit.tabs.loan",
  notes: "books.edit.tabs.notes",
  attachments: "attachment.list",
  history: "books.edit.tabs.history",
};

/**
 * The tabs of a book, shared by the edit form and the history page so both show the same bar.
 *
 * The first four tabs are anchors into the form's scroll column and must stay in step with its
 * `sections` array — EditPageShell couples them positionally. The history is a page of its own,
 * which keeps its (possibly long) list from loading with the form. Seen *from* the history page the
 * form tabs are links back to the book, which is what `onFormPage: false` turns them into.
 *
 * @param bookId null for a book that isn't saved yet: it has no history to link to.
 * @param t Translator without a namespace — the titles come from two of them (see TAB_TITLES).
 * @param onFormPage Whether the tabs are rendered on the form itself.
 */
export function bookTabs(
  bookId: number | null,
  t: (key: string) => string,
  onFormPage: boolean
): EditPageTab[] {
  // The form's own tabs scroll; from elsewhere they navigate back to it.
  const formHref =
    bookId != null && !onFormPage ? `/books/${bookId}` : undefined;
  const label = (id: BookTabId) => t(TAB_TITLES[id]);
  const formTab = (id: BookTabId) => ({ id, label: label(id), href: formHref });

  const formTabs = [
    formTab("general"),
    formTab("loan"),
    formTab("notes"),
    formTab("attachments"),
  ];
  // A book that isn't saved yet has no history — the tab would lead nowhere.
  if (bookId == null) {
    return formTabs;
  }
  return [
    ...formTabs,
    {
      id: "history",
      label: label("history"),
      href: `/books/${bookId}/history`,
    },
  ];
}
