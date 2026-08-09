import { z } from "zod";
import { BOOK_METADATA } from "@/lib/metadata/book.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length, the constants of an enum — comes from BookDO through
 * `lib/metadata/book.generated.ts`. Changing a column length in the entity and regenerating changes
 * this form; nothing here restates a rule (see lib/validation/from-metadata.ts).
 */
const m = fromMetadata(BOOK_METADATA);

const userRefSchema = z
  .object({
    id: z.number(),
    displayName: z.string(),
  })
  .nullable();

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Book — that is a hand-written decision,
 * because the DTO has neither the field set nor the names of the DO. What each field *allows* is not.
 *
 * The server validates too and has the last word (BookPagesRest.validate, HTTP 406 → see
 * lib/validation/server-errors.ts); this only anticipates the rules for immediate feedback. Rules
 * the client can't know — "signature already exists" — are deliberately absent.
 */
export const bookEditSchema = z.object({
  // null while the book is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  title: m.requiredString("title"),
  // Optional, as BookDO says (`@Column(length = 1000)`, nullable, no `required = true`): the server
  // accepts a book without an author, so demanding one here would only be a rule of this one form.
  authors: m.nullableString("authors"),
  signature: m.nullableString("signature"),
  yearOfPublishing: m.nullableString("yearOfPublishing"),
  publisher: m.nullableString("publisher"),
  editor: m.nullableString("editor"),
  isbn: m.nullableString("isbn"),
  keywords: m.nullableString("keywords"),
  abstractText: m.nullableString("abstractText"),
  comment: m.nullableString("comment"),
  status: m.enumField("status"),
  type: m.enumField("type"),
  lendOutBy: userRefSchema,
  // A LocalDate as "yyyy-MM-dd", not a column of BookDO's own length.
  lendOutDate: m.nullableString("lendOutDate"),
  lendOutComment: m.nullableString("lendOutComment"),
  created: m.nullableString("created"),
});

export type BookEditValues = z.infer<typeof bookEditSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually
 * renders (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const BOOK_EDIT_FIELDS = Object.keys(
  bookEditSchema.shape
) as readonly (keyof BookEditValues)[];
