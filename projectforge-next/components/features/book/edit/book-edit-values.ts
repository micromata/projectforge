import type { BookEditValues } from "./book-edit-schema";
import type { BookDetail } from "../types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised to null here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(book: BookDetail): BookEditValues {
  return {
    id: book.id ?? null,
    title: book.title ?? "",
    authors: book.authors ?? "",
    signature: book.signature ?? null,
    yearOfPublishing: book.yearOfPublishing ?? null,
    publisher: book.publisher ?? null,
    editor: book.editor ?? null,
    isbn: book.isbn ?? null,
    keywords: book.keywords ?? null,
    abstractText: book.abstractText ?? null,
    comment: book.comment ?? null,
    status: book.status ?? null,
    type: book.type ?? null,
    lendOutBy: book.lendOutBy ?? null,
    lendOutDate: book.lendOutDate ?? null,
    lendOutComment: book.lendOutComment ?? null,
    created: book.created ?? null,
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
