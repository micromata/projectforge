"use client";

import { createContext, useContext } from "react";
import { BOOK_METADATA } from "@/lib/metadata/book.generated";
import type { FieldMetadata } from "@/lib/metadata/types";
import type { BookEditValues } from "./book-edit-schema";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type BookForm = any;

const Ctx = createContext<BookForm | null>(null);

export const BookEditFormProvider = Ctx.Provider;

export function useBookEditForm(): BookForm {
  const form = useContext(Ctx);
  if (!form)
    throw new Error("useBookEditForm must be used inside BookEditFormProvider");
  return form;
}

/**
 * The backend's rules for one field of the form — `required`, `maxLength`, the enum constants — as
 * generated from BookDO (see lib/metadata/book.generated.ts).
 *
 * The field components read them from here instead of taking them as props, so the schema
 * (lib/validation/from-metadata.ts) and what the input actually allows cannot drift apart: both read
 * the same object. Alongside `useBookEditForm` because it answers the same question — "what does this
 * form know about the field I am rendering".
 *
 * `id` is the only form value with no counterpart in the metadata — it is the DTO's identity, not an
 * editable field — hence the neutral fallback instead of a throw.
 */
export function useFieldMetadata(name: keyof BookEditValues): FieldMetadata {
  const fields: Readonly<Record<string, FieldMetadata>> = BOOK_METADATA.fields;
  return fields[name] ?? { dataType: "STRING", required: false };
}
