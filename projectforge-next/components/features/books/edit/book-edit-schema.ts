import { z } from "zod";

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

export const bookEditSchema = z.object({
  id: z.number(),
  title: z.string().min(1, "Titel ist erforderlich"),
  authors: z.string().min(1, "Autor:innen sind erforderlich"),
  signature: nullableString,
  yearOfPublishing: nullableString,
  publisher: nullableString,
  editor: nullableString,
  isbn: nullableString,
  keywords: nullableString,
  abstractText: nullableString,
  comment: nullableString,
  status: z.enum(["PRESENT", "MISSED", "DISPOSED", "UNKNOWN"]).nullable(),
  type: z.enum(["BOOK", "MAGAZINE", "EBOOK", "OTHER"]).nullable(),
  lendOutBy: userRefSchema,
  lendOutDate: nullableString,
  lendOutComment: nullableString,
  created: nullableString,
});

export type BookEditValues = z.infer<typeof bookEditSchema>;
