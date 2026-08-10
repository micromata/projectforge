"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from "@tanstack/react-query";
import { fetchOne } from "@/lib/rs/client";
import {
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

/** id null = an entry being added, which has nothing to load yet. */
export function useEntityDetail<T>(entity: string, id: number | null) {
  return useQuery<T>({
    queryKey: [entity, id],
    queryFn: ({ signal }) => fetchOne<T>(entity, id!, signal),
    enabled: id != null && Number.isFinite(id) && id > 0,
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
