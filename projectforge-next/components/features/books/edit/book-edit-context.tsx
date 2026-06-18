"use client";

import { createContext, useContext } from "react";

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
