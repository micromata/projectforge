import { BOOK_METADATA } from "@/lib/metadata/book.generated";
import { definePage } from "@/lib/page-def/define-page";
import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import { AttachmentSection } from "./edit/sections/attachment-section";
import { KeywordsField } from "./edit/sections/keywords-field";
import { LoanSection } from "./edit/sections/loan-section";
import { lendOutColumn } from "./lend-out-column";
import { LoanStatus } from "./loan-status";
import {
  bookEditSchema,
  BOOK_EDIT_FIELDS,
  type BookEditValues,
} from "./edit/book-edit-schema";
import { emptyBookValues, toFormValues } from "./edit/book-edit-values";
import type { BookDetail, BookListRow } from "./types";

/** REST category of a book — the entity name every shared hook is parameterised with. */
export const BOOK_ENTITY = "book";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const BOOK_LIST_QUERY_KEY = ["book"] as const;

/**
 * The whole book page — list and edit — as data (see lib/page-def/types.ts).
 *
 * Every label, every rule and the constants of both enums come from BookDO through the generated
 * metadata; the history tab too (`historizable`). What is declared here is order, grouping and width,
 * plus the four things a book really has of its own: the loan — a section with the two writes it
 * triggers —, the attachments, the keyword picker and the loan badge beside the heading.
 */
export const BOOK_PAGE = definePage<
  BookListRow,
  BookEditValues,
  BookDetail,
  typeof BOOK_METADATA
>({
  entity: BOOK_ENTITY,
  metadata: BOOK_METADATA,
  route: "/book",
  queryKey: BOOK_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: General > Books (MenuItemDefId.BOOK_LIST).
  categoryKey: "menu.common",
  titleKey: "book.title.list",
  // The columns of BookEntityRest.createListLayout, in its order.
  columns: [
    { name: "created", size: 130 },
    {
      name: "yearOfPublishing",
      size: 56,
      // "Jahr" — the full label ("Jahr der Veröffentlichung") is four times the column's width.
      headerLabelKey: "book.yearOfPublishing.short",
      // A year, although BookDO stores it as a string: what a reader wants is "greater than 2015",
      // not "contains 2015".
      filterKind: "number",
      className: "font-medium",
    },
    {
      name: "signature",
      size: 76,
      className:
        "inline-flex items-center rounded-sm border border-border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground/80",
    },
    { name: "authors", size: 140, className: "font-medium" },
    // No link in the cell: the whole row navigates to the edit page.
    {
      name: "title",
      size: 280,
      minSize: 200,
      className: "font-semibold text-primary",
    },
    { name: "keywords", size: 132, className: "text-muted-foreground" },
    attachmentsColumn<BookListRow>(),
    lendOutColumn,
  ],
  edit: {
    schema: bookEditSchema,
    fieldNames: BOOK_EDIT_FIELDS,
    defaultValues: emptyBookValues,
    toFormValues,
    title: (book) => book.title,
    newTitleKey: "books.edit.newTitle",
    savedMessageKey: "books.edit.saved",
    // Lending out and returning are writes of their own (BookServicesRest), but they save the whole
    // posted book, so they run through the form's submit — see BookLoanActions and SubmitMeta.
    actions: ["lendOut", "returnBook"],
    headerTrailing: (book) => <LoanStatus lendOut={book?.lendOutBy != null} />,
    // Every section renders for a new book too: they are the book's own fields, and the legacy page
    // hid the loan block only because lending out needs a saved entity. Attachments are the
    // exception — they need a persisted id to hang off, so the section says so itself.
    sections: [
      {
        id: "general",
        titleKey: "books.edit.sections.general",
        tabTitleKey: "books.edit.tabs.general",
        fields: [
          { name: "title", span: 3 },
          { name: "authors", span: 3 },
          { name: "type" },
          // The one value a reader looks for first — whether the book is there at all.
          { name: "status", emphasized: true },
          { name: "isbn" },
          { name: "yearOfPublishing" },
          { name: "publisher", span: 2 },
          { name: "signature" },
          { name: "editor", span: 2 },
          { custom: KeywordsField, span: 3 },
        ],
      },
      {
        id: "loan",
        titleKey: "book.lending",
        tabTitleKey: "books.edit.tabs.loan",
        render: () => <LoanSection />,
      },
      {
        id: "notes",
        titleKey: "books.edit.sections.notes",
        tabTitleKey: "books.edit.tabs.notes",
        fields: [
          { name: "abstractText", rows: 4, span: 3 },
          { name: "comment", rows: 3, span: 3 },
        ],
      },
      {
        id: "attachments",
        // The title BookEntityRest gives the attachment fieldset, reused rather than written again.
        titleKey: "attachment.list",
        render: ({ id }) => <AttachmentSection bookId={id} />,
      },
    ],
  },
});
