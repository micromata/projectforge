"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { HierarchyIcon, ListViewIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { TASK_ROUTE, TASK_TREE_ROUTE } from "./task-routes";

/**
 * The link from one perspective on the tasks to the other — the pair of buttons Wicket has as
 * `TaskListForm`'s "tree view" and `TaskTreeForm`'s "list view".
 *
 * Both directions in one component, because they are one button that differs in where it points: the
 * two pages sit in different tiers (the tree is shared chrome, the list is the task feature), and a
 * copy in each would be two places to change the wording.
 *
 * Shared rather than feature-local for the same reason the routes are (see task-routes.ts).
 */
export function TaskPerspectiveLink({ to }: { to: "tree" | "list" }) {
  const t = useTranslations();
  const tree = to === "tree";
  // A pair of keys of their own („Tree view" / „List view"), and each is the title of the page it
  // leads to as well: the two buttons then read as the two perspectives they switch between. Wicket's
  // list-view button reads the untranslated model `"listView"`, which is a bug there, not a text to copy.
  const label = t(tree ? "task.tree.perspective" : "task.list.perspective");
  return (
    <Button asChild variant="ghost" size="sm" className="gap-1.5">
      <Link href={tree ? TASK_TREE_ROUTE : TASK_ROUTE}>
        <HugeiconsIcon
          icon={tree ? HierarchyIcon : ListViewIcon}
          size={14}
          aria-hidden
        />
        {label}
      </Link>
    </Button>
  );
}
