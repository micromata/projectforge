"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchOne, save } from "@/lib/rs/client";
import type { BookDetail } from "../types";

const ENTITY = "book";

/** id null = a book being added, which has nothing to load yet. */
export function useBookDetail(id: number | null) {
  return useQuery<BookDetail>({
    queryKey: [ENTITY, id],
    queryFn: ({ signal }) => fetchOne<BookDetail>(ENTITY, id!, signal),
    enabled: id != null && Number.isFinite(id) && id > 0,
  });
}

export function useSaveBook(id: number | null) {
  const qc = useQueryClient();
  return useMutation<BookDetail, Error, BookDetail>({
    mutationFn: (body) => save<BookDetail, BookDetail>(ENTITY, id, body),
    onSuccess: (saved) => {
      // A new book is cached under the id the backend assigned, not under null.
      qc.setQueryData([ENTITY, saved.id ?? id], saved);
      qc.invalidateQueries({ queryKey: ["books"] });
      if (saved.id != null) {
        qc.invalidateQueries({ queryKey: [ENTITY, "history", saved.id] });
      }
    },
  });
}
