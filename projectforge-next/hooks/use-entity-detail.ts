"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { fetchNew, fetchOne } from "@/lib/rs/client";
import {
  cancelEntityEdit,
  markEntityAsDeleted,
  postEntityAction,
  saveOrUpdateEntity,
  type EntityWriteResult,
} from "@/lib/rs/entity";
import { historyQueryKey } from "@/hooks/use-history";

/** An entity the edit form can write: the id is all the shared hooks need to know about it. */
export interface EntityWithId {
  id: number | null;
}

interface WriteOptions {
  /** Query key of the list page, so a write refreshes it. E.g. `["book"]`. */
  listQueryKey: readonly unknown[];
}

/**
 * The entity being edited, or — for id null — the preset one an "add" starts from.
 *
 * A new entry is loaded too, rather than starting from the form's own defaults: what a fresh entity
 * looks like is the backend's decision (`newBaseDTO`), and an order in particular cannot be saved
 * without the status it presets there. The two cases share the query so the form's reset path is the
 * same for both.
 */
export function useEntityDetail<T>(entity: string, id: number | null) {
  const isNew = id == null;
  return useQuery<T>({
    queryKey: [entity, id],
    queryFn: ({ signal }) =>
      isNew ? fetchNew<T>(entity, signal) : fetchOne<T>(entity, id, signal),
    enabled: isNew || (Number.isFinite(id) && id > 0),
    // A preset is a starting point, not shared state: refetching it would overwrite a form the user
    // has already begun to fill in.
    staleTime: isNew ? Infinity : undefined,
    refetchOnMount: isNew ? false : undefined,
    refetchOnWindowFocus: isNew ? false : undefined,
  });
}

/**
 * Saves an entity — insert and update alike, the backend tells them apart by `data.id`.
 *
 * The answer carries no entity, only the id (see lib/rs/entity.ts), so the caches are invalidated
 * instead of written: the saved entry comes back from the server on the next read, including the
 * fields it computed itself.
 */
export function useSaveEntity<T extends EntityWithId>(
  entity: string,
  { listQueryKey }: WriteOptions
) {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, T>({
    mutationFn: (data) => saveOrUpdateEntity(entity, data),
    onSuccess: (result, data) => {
      // A rejected entity is a regular answer here (HTTP 406), not an error - nothing changed.
      if (result.kind !== "ok") return;
      invalidateEntity(qc, entity, result.id ?? data.id, listQueryKey);
    },
  });
}

/**
 * A write the entity's backend offers besides save — `book/lendOut`, `book/returnBook`
 * (BookServicesRest).
 *
 * One mutation for all of them, with the action as a variable rather than a hook per name: which
 * actions an entity has is known to its own buttons, not to this hook, and a hook per name could not
 * be called from a component that renders a variable number of them.
 *
 * These endpoints save the whole posted entity, so they invalidate exactly what a save does.
 */
export function useEntityAction<T extends EntityWithId>(
  entity: string,
  { listQueryKey }: WriteOptions
) {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, { action: string; data: T }>({
    mutationFn: ({ action, data }) => postEntityAction(entity, action, data),
    onSuccess: (result, { data }) => {
      if (result.kind !== "ok") return;
      invalidateEntity(qc, entity, result.id ?? data.id, listQueryKey);
    },
  });
}

/**
 * Tells the backend the edit was cancelled, so the list can mark the entry the user came from.
 *
 * The entity is not written (see `cancelEntityEdit`), so only the list is invalidated — and it has
 * to be: the id the list marks travels with its response, and a cached one still names the entry
 * before this.
 */
export function useCancelEntityEdit<T extends EntityWithId>(
  entity: string,
  { listQueryKey }: WriteOptions
) {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, T>({
    mutationFn: (data) => cancelEntityEdit(entity, data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: listQueryKey });
    },
  });
}

/**
 * Marks an entity as deleted. This is the delete a historized entity offers: the row survives and
 * `RestPaths.UNDELETE` can bring it back.
 */
export function useDeleteEntity<T extends EntityWithId>(
  entity: string,
  { listQueryKey }: WriteOptions
) {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, T>({
    mutationFn: (data) => markEntityAsDeleted(entity, data),
    onSuccess: (result, data) => {
      if (result.kind !== "ok") return;
      invalidateEntity(qc, entity, data.id, listQueryKey);
    },
  });
}

/**
 * Drops every cached view of the entity a write touched: the list, the entry itself and its change
 * history — every save writes a history entry, so that timeline is stale too.
 */
export function invalidateEntity(
  qc: QueryClient,
  entity: string,
  id: number | null,
  listQueryKey?: readonly unknown[]
): void {
  if (listQueryKey) void qc.invalidateQueries({ queryKey: listQueryKey });
  if (id == null) return;
  void qc.invalidateQueries({ queryKey: [entity, id] });
  void qc.invalidateQueries({ queryKey: historyQueryKey(entity, id) });
}
