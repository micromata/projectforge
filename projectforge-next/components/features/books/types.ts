// Mirrors org.projectforge.rest.dto.Book (projectforge-rest). Keep field names
// in sync with the Spring DTO so the mock routes can be swapped for the real
// backend via a Next.js rewrite without changing call sites.

// Mirrors org.projectforge.business.book.BookStatus / BookType, in their order — the edit form
// derives both its Zod enum and its option lists from these, so a value exists exactly once.
export const BOOK_STATUS_VALUES = [
  "PRESENT",
  "MISSED",
  "DISPOSED",
  "UNKNOWN",
] as const;

export const BOOK_TYPE_VALUES = [
  "AUDIO_BOOK",
  "BOOK",
  "EBOOK",
  "MAGAZINE",
  "ARTICLE",
  "NEWSPAPER",
  "PERIODICAL",
  "FILM",
  "SOFTWARE",
  "THESIS",
  "MISC",
] as const;

export type BookStatus = (typeof BOOK_STATUS_VALUES)[number];
export type BookType = (typeof BOOK_TYPE_VALUES)[number];

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
}

// The change history is not book specific — its types live in lib/rs/history.ts.
