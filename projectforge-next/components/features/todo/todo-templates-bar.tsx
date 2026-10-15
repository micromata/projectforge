"use client";

import { useTranslations } from "next-intl";
import { FavoritesMenu } from "@/components/shared/favorites/favorites-menu";
import { useToDoTemplates } from "./use-todo-templates";

/**
 * The user's to-do templates in a bar above the fields — the favorites of the Wicket edit form. Applying
 * one fills what it holds and leaves the rest; saving one stores the to-do on screen under a name. There
 * is no "current" template to overwrite, so the menu offers create, rename and delete.
 */
export function ToDoTemplatesBar() {
  const t = useTranslations();
  const { templates, apply, create, rename, remove } = useToDoTemplates();

  return (
    <div className="flex items-center gap-2 rounded-md border border-border bg-muted/40 p-2">
      <FavoritesMenu
        favorites={templates}
        label={t("plugins.todo.templates")}
        showLabel
        className="h-7 shrink-0"
        onSelect={apply}
        onCreate={create}
        onRename={rename}
        onDelete={remove}
      />
    </div>
  );
}
