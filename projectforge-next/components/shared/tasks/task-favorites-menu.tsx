"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { FavoritesMenu } from "@/components/shared/favorites/favorites-menu";
import { fetchTaskInfo, type TaskFavorite, type TaskNode } from "@/lib/rs/task";
import { useTaskFavorites } from "./use-task-favorites";

export interface TaskFavoritesMenuProps {
  /** The task to save when creating a favorite, or null when nothing is picked. */
  taskId: number | null;
  disabled?: boolean;
  /** A favorite was applied — the loaded task, fed through the control's own pick funnel. */
  onSelect: (task: TaskNode) => void;
}

/**
 * The bookmark menu of the task picker: apply a saved structure element, or save the picked one under a
 * name (rename/delete the rest). Binds [useTaskFavorites] and the task load to the presentational
 * [FavoritesMenu], the same menu the calendar's filters and the timesheet's templates use.
 *
 * Applying a favorite resolves it to a task id and loads the whole task (its path and cost units), then
 * hands it to the control's pick funnel — which is what also records it as recently used.
 */
export function TaskFavoritesMenu({
  taskId,
  disabled,
  onSelect,
}: TaskFavoritesMenuProps) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const { favorites, create, select, rename, remove } = useTaskFavorites();

  if (disabled) return null;

  const handleSelect = async (favoriteId: number) => {
    const resolvedId = await select(favoriteId);
    if (resolvedId == null) return;
    // Seed the same cache entry the control reads its breadcrumb from (see TaskSelectControl).
    const task = await queryClient.fetchQuery({
      queryKey: ["taskInfo", resolvedId],
      queryFn: ({ signal }) => fetchTaskInfo(resolvedId, signal),
      staleTime: Infinity,
    });
    onSelect(task);
  };

  return (
    <FavoritesMenu<TaskFavorite>
      favorites={favorites}
      label={t("task.favorites.tooltip")}
      className="size-7 shrink-0 px-0"
      // The whole path behind a favorite's name, so its place in the structure reads on hover.
      tooltipFor={(favorite) => favorite.pathAsString}
      // Nothing to save without a picked task — mirror the legacy frontend, which only creates then.
      onCreate={(name) => {
        if (taskId != null) create(name, taskId);
      }}
      onSelect={handleSelect}
      onRename={rename}
      onDelete={remove}
    />
  );
}
