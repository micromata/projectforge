import { z } from "zod";
import { BOOK_STATUS_VALUES, BOOK_TYPE_VALUES } from "../types";

/**
 * `nullable`, not `nullish`: this schema validates the *form's* values, and those never hold
 * `undefined` — a field Spring omitted from the JSON (`JsonInclude.Include.NON_NULL`) is normalised
 * to null by toFormValues before it ever reaches the form. Widening it here instead would make the
 * schema's input type wider than the form's values, which @tanstack/react-form rejects.
 */
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
 * Mandatory text field: never null, an emptied input holds "" (see InputField, which keeps the empty
 * string for required fields). `refine` rather than `min(1)` so a missing value yields the [REQUIRED]
 * marker instead of one of Zod's untranslated English defaults.
 */
const requiredString = z.string().refine((v) => v.trim().length > 0, REQUIRED);

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
  title: requiredString,
  authors: requiredString,
  signature: nullableString,
  yearOfPublishing: nullableString,
  publisher: nullableString,
  editor: nullableString,
  isbn: nullableString,
  keywords: nullableString,
  abstractText: nullableString,
  comment: nullableString,
  // Required, like the column (BookDO: `nullable = false`) — the server rejects a book without one.
  // Still `nullable`, so a book that has no status can be represented and reported as missing rather
  // than silently given one; `refine` yields the [REQUIRED] marker, as for requiredString.
  // The `: boolean` matters: an inferred type guard would narrow `status` to non-null in the
  // schema's own type, and the form's values (which do allow null) would no longer match it.
  status: z
    .enum(BOOK_STATUS_VALUES)
    .nullable()
    .refine((v): boolean => v != null, REQUIRED),
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
