import type { BookEditValues } from "./book-edit-schema";
import type { BookDetail } from "../types";

export function toFormValues(book: BookDetail): BookEditValues {
  return {
    id: book.id,
    title: book.title,
    authors: book.authors ?? "",
    signature: book.signature,
    yearOfPublishing: book.yearOfPublishing,
    publisher: book.publisher,
    editor: book.editor,
    isbn: book.isbn,
    keywords: book.keywords,
    abstractText: book.abstractText,
    comment: book.comment,
    status: book.status,
    type: book.type,
    lendOutBy: book.lendOutBy,
    lendOutDate: book.lendOutDate,
    lendOutComment: book.lendOutComment,
    created: book.created,
  };
}

/** Blank form for a book that doesn't exist yet; `id` stays null until saved. */
export function emptyBookValues(): BookEditValues {
  return {
    id: null,
    title: "",
    authors: "",
    signature: null,
    yearOfPublishing: null,
    publisher: null,
    editor: null,
    isbn: null,
    keywords: null,
    abstractText: null,
    comment: null,
    status: "PRESENT",
    type: "BOOK",
    lendOutBy: null,
    lendOutDate: null,
    lendOutComment: null,
    created: null,
  };
}
