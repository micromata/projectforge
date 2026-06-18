// Mirrors org.projectforge.rest.dto.Book (projectforge-rest). Keep field names
// in sync with the Spring DTO so the mock routes can be swapped for the real
// backend via a Next.js rewrite without changing call sites.

export type BookStatus = "PRESENT" | "MISSED" | "DISPOSED" | "UNKNOWN";
export type BookType = "BOOK" | "MAGAZINE" | "EBOOK" | "OTHER";

export interface UserRef {
  id: number;
  displayName: string;
}

export interface BookDetail {
  id: number;
  title: string;
  authors: string | null;
  signature: string | null;
  yearOfPublishing: string | null;
  publisher: string | null;
  editor: string | null;
  isbn: string | null;
  keywords: string | null;
  abstractText: string | null;
  comment: string | null;
  status: BookStatus | null;
  type: BookType | null;
  lendOutBy: UserRef | null;
  lendOutDate: string | null;
  lendOutComment: string | null;
  // Audit metadata surfaced by Spring's BaseDTO.
  created: string | null;
}

// Projection used by the list page — derived from BookDetail.
export interface BookListRow {
  id: number;
  title: string;
  authors: string | null;
  signature: string | null;
  yearOfPublishing: string | null;
  keywords: string | null;
  lendOutBy: UserRef | null;
  created: string | null;
}

// Mirrors DisplayHistoryEntry / DisplayHistoryEntryAttr in
// projectforge-business: framework.persistence.history.
export type EntityOpType = "Insert" | "Update" | "Delete";
export type PropertyOpType = "Insert" | "Update" | "Delete";

export interface HistoryEntryAttr {
  propertyName: string | null;
  displayPropertyName: string | null;
  operationType: PropertyOpType | null;
  oldValue: string | null;
  newValue: string | null;
}

export interface HistoryEntry {
  id: number;
  modifiedAt: string;
  timeAgo: string;
  modifiedByUserId: number | null;
  modifiedByUser: string | null;
  operationType: EntityOpType;
  operation: string;
  userComment: string | null;
  attributes: HistoryEntryAttr[];
}
