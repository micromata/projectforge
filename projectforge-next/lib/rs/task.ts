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
 * The project a task's cost units come from, `TaskServicesRest.Projekt` — resolved through the
 * ancestors, so a task without a project of its own reports the one it inherits.
 */
export interface TaskProjekt {
  id?: number | null;
  name?: string | null;
  /** The cost number without the two Kost2Art digits, e.g. `5.123.45` — shown as `<kost>.*`. */
  kost?: string | null;
}

/**
 * A task's booked-versus-planned effort, `rest/task/Consumption.kt` — pre-rendered by the backend,
 * because the percentage, the wording and the colour all follow rules only it knows (the "finished"
 * flag, the person-day unit, the user's number format).
 *
 * Declared here rather than beside the cell that paints it: both perspectives of a task carry the
 * same value — the tree sends it per node, the list per row (`Task.copyFrom4ListRow`).
 */
export interface TaskConsumption {
  /** Already localised, e.g. "350PT/188PT (186%)" — the bar's tooltip. */
  title?: string;
  /** One of the `progress-*` names of `Consumption.Status`, which picks the bar's colour. */
  status?: string;
  percentage?: number;
  /** Capped at 100, unlike `percentage` — the width the bar is painted with. */
  barPercentage?: number;
}

/**
 * One order booked against a task, `TaskServicesRest.Task.Order`.
 *
 * The `url` is the backend's: the order's page may be Wicket's or this app's, which only
 * `NextMigration` knows.
 */
export interface TaskOrder {
  number: string;
  title?: string;
  text?: string;
  url?: string;
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
  /** Only sent by `info/{id}`: the project the cost units come from (see FinanceSection). */
  projekt?: TaskProjekt | null;
  /** Only sent by `info/{id}`: whether cost units are configured at all (`Configuration`). */
  costConfigured?: boolean;
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
   * The task to bring into view. On the initial call the backend opens its ancestors so the row is on
   * screen in the *full* tree; the client marks and scrolls to it (see TaskTreeTable's `highlightRowId`,
   * Wicket's `PARAMETER_HIGHLIGHTED_ROW`). The select field passes the current value here, so its popover
   * opens on the whole tree expanded to that task rather than on a narrowed neighbourhood of it.
   */
  highlightedTaskId?: number;
  /**
   * Re-root the answer at this node instead of the real root: only its subtree is returned, its direct
   * children starting at indent 0. The breadcrumb of the select panel sets it to climb the structure
   * without scrolling the whole tree (see TaskServicesRest.getTree). A node the user may not select is
   * ignored server-side, falling back to the full tree.
   */
  rootTaskId?: number;
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

/**
 * The root of the structure tree, `{id, displayName}` — the parent every top level element hangs below.
 *
 * Asked for rather than assumed: a task without a parent is refused
 * (`TaskDao.checkConstraintVioloation`), and which task is the root is the server's knowledge. The
 * caller is the wizard's "create structure element" link, which presets it as the parent exactly as
 * Wicket does (see TaskServicesRest.getRoot).
 */
export function fetchRootTask(
  signal?: AbortSignal
): Promise<TaskDisplayObject> {
  return request<TaskDisplayObject>(
    `/rs/task/tree/root`,
    { method: "GET" },
    signal
  );
}

/** A task as `AbstractEntityRest.DisplayObject` — id plus the label the backend built for it. */
export interface TaskDisplayObject {
  id: number;
  displayName?: string;
}

/** What the client asks a preview for, `TaskServicesRest.Kost2PreviewRequest`. */
export interface Kost2PreviewRequest {
  /** Id of the task being edited, null while it is being added. */
  id?: number | null;
  /** The only way to resolve the project of a task that has no id yet. */
  parentTaskId?: number | null;
  kost2BlackWhiteList?: string | null;
  kost2IsBlackList: boolean;
  /** A cost unit just picked, appended by the backend before the preview is computed. */
  addKost2Id?: number | null;
}

/** `TaskServicesRest.Kost2Preview`. */
export interface Kost2Preview {
  /** The list, normalized and sorted — what the form's field is set to after a pick. */
  kost2BlackWhiteList?: string | null;
  /** The cost number of the resolved project, or null if the task has none. */
  projektKost?: string | null;
  /** The resulting cost units in wild card form, e.g. `5.123.45.*`. */
  kost2WildCard?: string | null;
  /** The resulting cost units, one formatted number per line — the Wicket tooltip's content. */
  kost2ListAsLines?: string | null;
}

/**
 * What the kost2 block of the task form resolves to for a black/white list the user has typed but not
 * saved — and, with `addKost2Id`, for the list with a picked cost unit appended.
 *
 * Asked rather than computed here: the three calls behind it need the cost cache, the project of the
 * task and the number format, and `TaskHelper.addKost2` has a branch a TypeScript copy would get wrong
 * (see the backend's own note on `Kost2Preview`).
 *
 * A POST although nothing is written: the list is form content, and a GET would carry it in the url and
 * into every log.
 */
export function postKost2Preview(
  body: Kost2PreviewRequest,
  signal?: AbortSignal
): Promise<Kost2Preview> {
  return request<Kost2Preview>(
    "/rs/task/kost2Preview",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}

/** `TaskWizardRest.ExecuteRequest`. All three groups are optional; the task is not. */
export interface TaskWizardRequest {
  taskId: number;
  managerGroupId?: number | null;
  teamGroupId?: number | null;
  externalGroupId?: number | null;
}

/** What became of one access entry, `TaskWizardService.AccessStatus`. */
export type TaskWizardAccessStatus = "CREATED" | "UPDATED" | "UNCHANGED";

/** The role a group was granted, `TaskWizardService.GroupType`. */
export type TaskWizardGroupType = "MANAGER" | "TEAM" | "EXTERNAL";

/** The access types of a `GroupTaskAccessDO`, in the order the access management lists them. */
export type AccessTypeName =
  | "TASK_ACCESS_MANAGEMENT"
  | "TASKS"
  | "TIMESHEETS"
  | "OWN_TIMESHEETS";

/** `TaskWizardRest.AccessRight`: the four permissions of one access type of an access entry. */
export interface AccessRight {
  accessType: AccessTypeName;
  select: boolean;
  insert: boolean;
  update: boolean;
  delete: boolean;
}

/** `TaskWizardRest.AccessEntry`: one access entry the wizard looked at. */
export interface TaskWizardAccessEntry {
  groupName?: string | null;
  groupType: TaskWizardGroupType;
  taskId: number;
  taskTitle?: string | null;
  /** True for the element the user picked, false for an ancestor that only got read access. */
  pickedElement: boolean;
  status: TaskWizardAccessStatus;
  /** Whether the rights hold for the sub elements as well — only on the picked element. */
  recursive: boolean;
  /** Which rights the entry would carry, for the matrix the preview shows. */
  rights: AccessRight[];
}

/** `TaskWizardRest.ExecuteResponse`. */
export interface TaskWizardResult {
  taskTitle?: string | null;
  /** Access entries the wizard touched over all groups, the ancestors' and the unchanged included. */
  accessEntries: number;
  created: number;
  updated: number;
  unchanged: number;
  /**
   * The single entries behind those numbers, the picked element's first per group and then its
   * ancestors upwards — that order is the hierarchy, since the rows of a group are the one path from
   * the picked element to the root (see previewRows, which indents them by it).
   */
  entries: TaskWizardAccessEntry[];
}

/** Grants the chosen groups their rights on the structure element and read access on its ancestors. */
export function executeTaskWizard(
  body: TaskWizardRequest,
  signal?: AbortSignal
): Promise<TaskWizardResult> {
  return request<TaskWizardResult>(
    "/rs/taskWizard/execute",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}

/**
 * What [executeTaskWizard] with the same body would do, without doing any of it — the wizard's preview
 * table, asked for again with every pick.
 *
 * The same shape as the execution's answer, because it is the same walk over the same rows
 * (`TaskWizardService.previewAccess`): the statuses are the ones the write would report.
 */
export function previewTaskWizard(
  body: TaskWizardRequest,
  signal?: AbortSignal
): Promise<TaskWizardResult> {
  return request<TaskWizardResult>(
    "/rs/taskWizard/preview",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}
