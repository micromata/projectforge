import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { request } from "./client";

/**
 * The structure tree (Strukturbaum), served by `TaskServicesRest` rather than by a list layout.
 *
 * Its own endpoint, not the generic `<category>/list`: the tree is not a page of rows. Which nodes
 * are visible follows from the *server's* expansion state — a set of open task ids in the user's
 * prefs (`TaskTree.USER_PREFS_KEY_OPEN_TASKS`) — so opening a node is a request, not a client-side
 * state change, and the answer is the whole visible tree again.
 */

/** Whether a node can be expanded, as `TaskServicesRest.TreeStatus`. */
export type TaskTreeStatus = "LEAF" | "OPENED" | "CLOSED";

/** A cost unit 2 of a task, `TaskServicesRest.Kost2`. */
export interface TaskKost2 {
  id: number;
  title: string;
}

/**
 * One node, `TaskServicesRest.Task`. Only the fields the backend actually sends, and only for the
 * two shapes this app asks for: the flat tree (`table=true`) and a single task (`info/{id}`).
 *
 * `consumption` stays `unknown` on purpose: the cell that paints it (ConsumptionCell) owns that
 * shape, and nothing here reads into it.
 */
export interface TaskNode {
  id: number;
  /** Depth in the flat (`table=true`) answer, 0 for a child of the root. */
  indent?: number;
  treeStatus?: TaskTreeStatus;
  title?: string;
  shortDescription?: string;
  /** Already translated by the backend, unlike the raw `status`. */
  statusAsString?: string;
  /** e.g. `6.000.00.*` — the shared prefix of the task's cost units. */
  kost2WildCard?: string;
  /** All cost 2 numbers, one per line, for the column's tooltip. */
  kost2ListAsLines?: string;
  /** Only sent by `info/{id}`; the tree omits it to save bandwidth. */
  kost2List?: TaskKost2[];
  /** Only sent by `info/{id}`: the ancestors, root first, *excluding* the task itself. */
  path?: TaskNode[];
  /**
   * The tree's root node, sent last and only to admins and financial staff (`showRootForAdmins`).
   *
   * Flagged by the backend rather than derived here: the tree's answer carries no `parentTask`, and
   * the root's id is 1 only by convention. It may be shown and expanded, but not selected as the task
   * of anything else — see [isSelectableTask].
   */
  root?: boolean;
  /**
   * Whether the task is marked as deleted — the tree's filter can include such tasks. The row is then
   * tinted and struck through (see deletedRowClass); `statusAsString` reads "gelöscht".
   */
  deleted?: boolean;
  consumption?: unknown;
  /** Index signature so the node can be read as a table row (see DataObject). */
  [field: string]: unknown;
}

/**
 * Whether a node may be picked as the task of *another* entity — a timesheet, an order position, an
 * access entry.
 *
 * Only the root is not: it is the tree's anchor, not a task anyone books against (Wicket leaves it out
 * of the select panel's path for the same reason, and never offers it for selection). The structure
 * tree page itself is exempt — there a click opens the task's own edit page, which the root has.
 */
export function isSelectableTask(task: TaskNode): boolean {
  return task.root !== true;
}

/** The status filter of the tree, `TaskFilter`. Names are the request's parameter names. */
export interface TaskTreeFilter {
  searchString: string;
  opened: boolean;
  notOpened: boolean;
  closed: boolean;
  deleted: boolean;
}

/**
 * The filter the backend starts from. It lives in the session, so the first answer says what is in
 * effect rather than the client deciding — a filter set on the Wicket page still applies here.
 */
export const DEFAULT_TASK_TREE_FILTER: TaskTreeFilter = {
  searchString: "",
  opened: true,
  notOpened: true,
  closed: false,
  deleted: false,
};

/**
 * The five fields of the tree's filter, picked out of the serialized one.
 *
 * `initFilter` is the whole `TaskFilter` — `maxRows`, `pageSize`, `useModificationFilter` and half a
 * dozen more that belong to the list pages. Passing those on would put them into the query string of
 * every following call (the backend reads a non-initial call's parameters as the new filter) and into
 * the query key, where each of them would be a reason to refetch.
 */
export function taskTreeFilterOf(
  initFilter: Partial<TaskTreeFilter> | undefined
): TaskTreeFilter {
  const filter = { ...DEFAULT_TASK_TREE_FILTER };
  if (!initFilter) return filter;
  for (const key of Object.keys(
    DEFAULT_TASK_TREE_FILTER
  ) as (keyof TaskTreeFilter)[]) {
    const value = initFilter[key];
    if (value !== undefined && value !== null) {
      // Each key's type is its own, so the assignment needs the union widened once.
      (filter as Record<string, unknown>)[key] = value;
    }
  }
  return filter;
}

/**
 * `TaskServicesRest.Result`.
 *
 * It carries `columnDefs`/`sortModel` in the same shape a `UIAgGrid` node does, which is why it
 * extends [AgGridNode]: `adaptColumnDefs` and `initialStateFrom` then take the response itself, and
 * the tree gets the columns, their widths and the user's restored column state from the one place
 * that already knows how to read them.
 *
 * Everything but `nodes` is only sent for `initial: true`.
 */
export interface TaskTreeResult extends AgGridNode {
  nodes: TaskNode[];
  initFilter?: Partial<TaskTreeFilter>;
}

export interface TaskTreeParams extends Partial<TaskTreeFilter> {
  /** Ask for the column defs, the restored column state and the session's filter. */
  initial?: boolean;
  /** Open this node and all its ancestors; persisted server-side. */
  open?: number;
  /** Collapse this node; persisted server-side. */
  close?: number;
  /**
   * Narrow the answer to this node, its descendants and (for a leaf) its siblings — how the select
   * field shows the neighbourhood of the current value instead of the whole tree.
   */
  highlightedTaskId?: number;
  /** Prepend the root node, for admins and financial staff. */
  showRootForAdmins?: boolean;
  /**
   * Ask for the select popover's narrow set of columns instead of the page's full one, and for its
   * own stored column state — hiding a column while picking a task must not change the tree page.
   */
  select?: boolean;
}

/**
 * The visible tree, flat: every node carries its `indent` and `treeStatus`.
 *
 * Always `table=true`. The nested variant (`children`) exists but would have to be flattened for a
 * table anyway, and the backend's highlight handling only runs in the flat one.
 */
export function fetchTaskTree(
  params: TaskTreeParams,
  signal?: AbortSignal
): Promise<TaskTreeResult> {
  const query = new URLSearchParams({ table: "true" });
  for (const [key, value] of Object.entries(params)) {
    // An empty search string is meaningful (it clears the session's filter), false and 0 are values;
    // only "not asked for" may be left out.
    if (value !== undefined && value !== null) query.set(key, String(value));
  }
  return request<TaskTreeResult>(
    `/rs/task/tree?${query}`,
    { method: "GET" },
    signal
  );
}

/**
 * One task with its cost units and its ancestor path — what a select field needs to show a stored
 * id as a breadcrumb.
 */
export function fetchTaskInfo(
  id: number,
  signal?: AbortSignal
): Promise<TaskNode> {
  return request<TaskNode>(`/rs/task/info/${id}`, { method: "GET" }, signal);
}
