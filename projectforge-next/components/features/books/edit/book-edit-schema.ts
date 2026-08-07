import { z } from "zod";
import { BOOK_STATUS_VALUES, BOOK_TYPE_VALUES } from "../types";

const nullableString = z
  .string()
  .nullable()
  .transform((v) => (v && v.trim().length > 0 ? v : null));

const userRefSchema = z
  .object({
    id: z.number(),
    displayName: z.string(),
  })
  .nullable();

/**
 * Marker instead of a text: the message is `validation.error.fieldRequired`, which needs the
 * field's label as its argument — and only the rendering field knows that (see book-edit-fields).
 */
export const REQUIRED = "@required";

/**
 * Mirrors org.projectforge.rest.dto.Book.
 *
 * The server validates too and has the last word (BookPagesRest.validate, HTTP 406 → see
 * lib/validation/server-errors.ts); this only anticipates the rules for immediate feedback. Rules
 * the client can't know — "signature already exists" — are deliberately absent.
 */
export const bookEditSchema = z.object({
  // null while the book is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  title: z.string().min(1, REQUIRED),
  authors: z.string().min(1, REQUIRED),
  signature: nullableString,
  yearOfPublishing: nullableString,
  publisher: nullableString,
  editor: nullableString,
  isbn: nullableString,
  keywords: nullableString,
  abstractText: nullableString,
  comment: nullableString,
  status: z.enum(BOOK_STATUS_VALUES).nullable(),
  type: z.enum(BOOK_TYPE_VALUES).nullable(),
  lendOutBy: userRefSchema,
  lendOutDate: nullableString,
  lendOutComment: nullableString,
  created: nullableString,
});

export type BookEditValues = z.infer<typeof bookEditSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually
 * renders (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const BOOK_EDIT_FIELDS = Object.keys(
  bookEditSchema.shape
) as readonly (keyof BookEditValues)[];
