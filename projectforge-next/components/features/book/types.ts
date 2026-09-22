// Mirrors org.projectforge.rest.dto.Book (projectforge-rest). Keep field names
// in sync with the Spring DTO so the mock routes can be swapped for the real
// backend via a Next.js rewrite without changing call sites.

import type { BOOK_METADATA } from "@/lib/metadata/book.generated";

// The constants of org.projectforge.business.book.BookStatus / BookType, taken from the generated
// metadata instead of copied: the generator reads the enum itself, so a new constant reaches the
// types (and the option lists of the edit form, see use-book-options) by regenerating.
export type BookStatus =
  (typeof BOOK_METADATA.fields.status.enumValues)[number]["value"];
export type BookType =
  (typeof BOOK_METADATA.fields.type.enumValues)[number]["value"];

export interface UserRef {
  id: number;
  displayName: string;
}

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. Whoever reads a book has to cope with `undefined` — toFormValues normalises it.
 */
export interface BookDetail {
  /** null for a book that has not been saved yet (Spring assigns the id). */
  id: number | null;
  title: string;
  authors?: string | null;
  signature?: string | null;
  yearOfPublishing?: string | null;
  publisher?: string | null;
  editor?: string | null;
  isbn?: string | null;
  keywords?: string | null;
  abstractText?: string | null;
  comment?: string | null;
  status?: BookStatus | null;
  type?: BookType | null;
  lendOutBy?: UserRef | null;
  lendOutDate?: string | null;
  lendOutComment?: string | null;
  // Audit metadata surfaced by Spring's BaseDTO.
  created?: string | null;
}

// Projection used by the list page — derived from BookDetail.
export interface BookListRow {
  id: number;
  title: string;
  authors?: string | null;
  signature?: string | null;
  yearOfPublishing?: string | null;
  keywords?: string | null;
  lendOutBy?: UserRef | null;
  created?: string | null;
  /** Number of attachments; absent for a book that never had one. */
  attachmentsCounter?: number | null;
  /** Their total size in bytes — sorted on, not displayed (see attachmentsSizeFormatted). */
  attachmentsSize?: number | null;
  /**
   * Size and count in one, formatted by the backend in the user's locale ("5,2MB (3)"), or "-"
   * without attachments. Taken as it is, per AttachmentsInfo.getAttachmentsSizeFormatted.
   */
  attachmentsSizeFormatted?: string | null;
}

// The change history is not book specific — its types live in lib/rs/history.ts.
