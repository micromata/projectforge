"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { postEntityAction, type EntityWriteResult } from "@/lib/rs/entity";
import { invalidateEntity } from "@/hooks/use-entity-detail";
import type { BookDetail } from "../types";

/** REST category of a book — the entity name every shared hook is parameterised with. */
export const BOOK_ENTITY = "book";
/** Query key of the book list, so a write refreshes it. */
export const BOOKS_LIST_QUERY_KEY = ["books"] as const;

/**
 * Lends the book out to the logged-in user: the server sets `lendOutBy` from the session and
 * `lendOutDate` to today (BookServicesRest.lendOut), so neither is sent — and it saves the posted
 * book along the way, which is why the whole entity goes with it.
 *
 * Lending out a book that is already lent out silently reassigns it, as in the legacy page.
 */
export function useLendOutBook() {
  return useLoanAction("lendOut");
}

/** Clears `lendOutBy`, `lendOutDate` and `lendOutComment` (BookServicesRest.returnBook). */
export function useReturnBook() {
  return useLoanAction("returnBook");
}

/**
 * The two writes a book has beyond save and delete — which is why they stayed here while those moved
 * to hooks/use-entity-detail.ts: no other entity has a loan.
 */
function useLoanAction(action: "lendOut" | "returnBook") {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, BookDetail>({
    mutationFn: (book) => postEntityAction(BOOK_ENTITY, action, book),
    onSuccess: (result, book) => {
      if (result.kind !== "ok") return;
      // The answer carries no entity, so the loan fields are read back from the server.
      invalidateEntity(
        qc,
        BOOK_ENTITY,
        result.id ?? book.id,
        BOOKS_LIST_QUERY_KEY
      );
    },
  });
}
