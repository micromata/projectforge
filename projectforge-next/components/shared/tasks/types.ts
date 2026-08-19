import type { TaskNode, TaskTreeFilter } from "@/lib/rs/task";

export type { TaskNode, TaskTreeFilter };

/** Props of the tree panel, shared by the page and the select field around it. */
export interface TaskTreePanelProps {
  /**
   * The task the tree opens at and marks. The backend then answers with its neighbourhood instead
   * of the whole tree, which is what makes the panel usable as the body of a select field.
   */
  highlightTaskId?: number | null;
  /**
   * Called when a row is picked — a click outside the first column, or on a leaf's title.
   *
   * Never called for the root node unless [rootSelectable] says so: the root is the tree's anchor, not
   * a task another entity can be booked against (see isSelectableTask).
   */
  onSelect?: (task: TaskNode) => void;
  /**
   * Show the root node, for admins and financial staff. The tree page does; a select field doesn't,
   * since the root is nothing anyone books against.
   */
  showRootForAdmins?: boolean;
  /**
   * Let the root be picked. Only the structure tree page does, where picking means "open this task's
   * edit page" — and the root is a task with a page of its own. For every other entity the root is not
   * a valid task, so the default is false.
   */
  rootSelectable?: boolean;
  /**
   * Show the narrow set of columns a select popover has room for, and keep its column state apart
   * from the tree page's. Set by the select field; the tree page leaves it off.
   */
  selectMode?: boolean;
  /**
   * Render the tree as its own *page*: the action bar above it, "add a subtask" per row, the handbook
   * link beside the search field, and the page's hint (`task.tree.info`) instead of the select
   * panel's.
   *
   * Only `/next/taskTree` sets it. Everywhere else the tree is the body of a select field, where those
   * actions would either leave the form the user is in or explain a click that means something else
   * there (see TaskTreeActionBar).
   */
  pageMode?: boolean;
  className?: string;
}
