import type { TaskNode, TaskTreeFilter } from "@/lib/rs/task";
import type { TaskTreeState } from "./use-task-tree";

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
   * Show a breadcrumb above the tree that re-roots it at a subtree (see TaskRootBreadcrumb). Set by the
   * select field, where a task is picked from its neighbourhood rather than by scrolling the whole tree;
   * the tree page leaves it off.
   */
  rootNavigable?: boolean;
  /**
   * Root the tree at this node from the start (see useTaskTree). Set when a click on an ancestor drills
   * the select field into that node: the tree opens rooted there, the node itself sitting in the
   * breadcrumb rather than as a row. Only meaningful together with [rootNavigable].
   */
  initialRootTaskId?: number | null;
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
  /**
   * The tree's state, when the caller owns it: the value of [useTaskTree]. Handed in by the tree page,
   * whose header holds the buttons that act on the filter — the same reason [DataTable] takes a table
   * instance. Left out everywhere else, and the panel keeps its own.
   */
  tree?: TaskTreeState;
  className?: string;
}
