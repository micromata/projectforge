import type { TaskNode, TaskTreeFilter } from "@/lib/rs/task";

export type { TaskNode, TaskTreeFilter };

/** Props of the tree panel, shared by the page and the select field around it. */
export interface TaskTreePanelProps {
  /**
   * The task the tree opens at and marks. The backend then answers with its neighbourhood instead
   * of the whole tree, which is what makes the panel usable as the body of a select field.
   */
  highlightTaskId?: number | null;
  /** Called when a row is picked — a click outside the first column, or on a leaf's title. */
  onSelect?: (task: TaskNode) => void;
  /**
   * Show the root node, for admins and financial staff. The tree page does; a select field doesn't,
   * since the root is nothing anyone books against.
   */
  showRootForAdmins?: boolean;
  className?: string;
}
