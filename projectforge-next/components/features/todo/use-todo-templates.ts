"use client";

import { useCallback } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import {
  createToDoTemplate,
  deleteToDoTemplate,
  fetchToDoTemplates,
  renameToDoTemplate,
  selectToDoTemplate,
} from "@/lib/rs/todo";
import type { FavoriteIdTitle } from "@/lib/rs/types";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import type { ToDoValues } from "./todo-schema";
import { templateFieldsOf } from "./todo-values";
import type { ToDoDetail } from "./types";

/** Cache key — module-level so a write elsewhere could invalidate it by the same key. */
const TEMPLATES_KEY = ["todo", "templates"] as const;

/**
 * The saved templates of the to-do form and everything that fills the form from one of them — the
 * favorites of the Wicket edit form, kept as favorites of their own (`ToDoFavoritesService`).
 */
export function useToDoTemplates() {
  const form = useEntityEditForm();
  const queryClient = useQueryClient();

  const templates = useQuery<FavoriteIdTitle[]>({
    queryKey: TEMPLATES_KEY,
    queryFn: ({ signal }) => fetchToDoTemplates(signal),
    staleTime: Infinity,
  });

  const onError = (err: unknown) =>
    toast.error(err instanceof Error ? err.message : String(err));

  /** Fills the form with what the template holds, leaving the rest as it is. */
  const apply = useCallback(
    async (id: number) => {
      try {
        const fields = templateFieldsOf(await selectToDoTemplate(id));
        for (const [name, value] of Object.entries(fields)) {
          form.setFieldValue(name, value);
        }
      } catch (err) {
        onError(err);
      }
    },
    [form]
  );

  /** Runs a write that answers with the new list and refreshes the cache from it. */
  const write = useCallback(
    async (op: () => Promise<FavoriteIdTitle[]>) => {
      try {
        queryClient.setQueryData(TEMPLATES_KEY, await op());
      } catch (err) {
        onError(err);
      }
    },
    [queryClient]
  );

  return {
    templates: templates.data ?? [],
    apply,
    // The form values are the DTO's shape, so they are posted as they stand.
    create: (name: string) =>
      write(() =>
        createToDoTemplate(
          name,
          form.state.values as ToDoValues as unknown as ToDoDetail
        )
      ),
    rename: (id: number, newName: string) =>
      write(() => renameToDoTemplate(id, newName)),
    remove: (id: number) => write(() => deleteToDoTemplate(id)),
  };
}
