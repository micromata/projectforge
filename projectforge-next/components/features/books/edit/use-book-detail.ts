"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchOne, save } from "@/lib/rs/client";
import type { BookDetail } from "../types";

const ENTITY = "book";

export function useBookDetail(id: number) {
  return useQuery<BookDetail>({
    queryKey: [ENTITY, id],
    queryFn: ({ signal }) => fetchOne<BookDetail>(ENTITY, id, signal),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useSaveBook(id: number) {
  const qc = useQueryClient();
  return useMutation<BookDetail, Error, BookDetail>({
    mutationFn: (body) => save<BookDetail, BookDetail>(ENTITY, id, body),
    onSuccess: (saved) => {
      qc.setQueryData([ENTITY, id], saved);
      qc.invalidateQueries({ queryKey: ["books"] });
      qc.invalidateQueries({ queryKey: [ENTITY, "history", id] });
    },
  });
}
