// Mirrors org.projectforge.rest.gantt.GanttDiagram and GanttObject (projectforge-rest). Keep field names
// in sync with the Spring DTOs. Settings and style are flattened into the diagram; the tree of the chart
// travels as `root`, which only the edit form loads (the list rows carry none).

/** `GanttAccess`: who may read or write the chart besides the owner. */
export type GanttAccess = "OWNER" | "PROJECT_MANAGER" | "ALL";

/** `GanttRelationType`, the dependency between an object and its predecessor. */
export type GanttRelationType =
  | "START_START"
  | "START_FINISH"
  | "FINISH_START"
  | "FINISH_FINISH";

/** `GanttObjectType`. */
export type GanttObjectType = "ACTIVITY" | "SUMMARY" | "MILESTONE";

/**
 * The values of the structure element (TaskDO) a node stands for — what "reject" restores and what a
 * differing node value is compared with. Read-only; absent for a Gantt-only node.
 */
export interface GanttTaskValues {
  title?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  duration?: number | null;
  progress?: number | null;
  predecessorId?: number | null;
  predecessorTitle?: string | null;
  predecessorOffset?: number | null;
  relationType?: GanttRelationType | null;
  type?: GanttObjectType | null;
  /** Whether the user may write values back to the structure element (save-to-task). */
  updateAccess?: boolean;
}

/**
 * One node of the chart. A positive id is the id of the structure element it stands for, a negative one
 * marks an object that lives in this chart only (see GanttChartDao.getNextId).
 */
export interface GanttObject {
  id: number;
  title?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  duration?: number | null;
  progress?: number | null;
  predecessorId?: number | null;
  predecessorOffset?: number | null;
  relationType?: GanttRelationType | null;
  type?: GanttObjectType | null;
  workpackageCode?: string | null;
  visible?: boolean;
  children?: GanttObject[] | null;
  /** Read-only, for display: the title of the predecessor, which may be outside this chart. */
  predecessorTitle?: string | null;
  task?: GanttTaskValues | null;
}

type Ref = {
  id: number;
  displayName?: string;
};

/** A task reference as the DTO carries it: the title for the list, the path to the root for its tooltip. */
export type GanttTaskRef = Ref & {
  title?: string | null;
  path?: string | null;
};

export interface GanttDiagramDetail {
  id: number | null;
  name?: string | null;
  task?: GanttTaskRef | null;
  owner?: Ref | null;
  readAccessType?: GanttAccess | null;
  writeAccessType?: GanttAccess | null;
  title?: string | null;
  fromDate?: string | null;
  toDate?: string | null;
  showOnlyVisibles?: boolean;
  openNodes?: number[] | null;
  width?: number | null;
  totalLabelWidth?: number | null;
  relativeTimeValues?: boolean;
  showToday?: boolean;
  showCompletion?: boolean;
  root?: GanttObject | null;
  created?: string | null;
  lastUpdate?: string | null;
  writeAccess?: boolean;
  deleteAccess?: boolean;
}

/** Projection the list page renders — the same DTO without the tree, keyed by its id. */
export interface GanttDiagramListRow extends GanttDiagramDetail {
  id: number;
}
