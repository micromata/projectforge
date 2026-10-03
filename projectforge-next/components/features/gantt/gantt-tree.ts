import type { GanttObject } from "./types";

/**
 * The tree operations of the Gantt editor, pure and immutable: every change hands back a new root, which
 * the form stores as its `root` value (the counterpart of GanttTaskImpl's mutating methods the Wicket
 * page called).
 */

/** One row of the tree table: a node below the root, with its depth and whether it has children. */
export interface GanttRow {
  node: GanttObject;
  depth: number;
  parentId: number;
  hasChildren: boolean;
  open: boolean;
}

export function findNode(
  root: GanttObject | null | undefined,
  id: number
): GanttObject | undefined {
  if (!root) return undefined;
  if (root.id === id) return root;
  for (const child of root.children ?? []) {
    const found = findNode(child, id);
    if (found) return found;
  }
  return undefined;
}

export function findParent(
  root: GanttObject,
  id: number
): GanttObject | undefined {
  for (const child of root.children ?? []) {
    if (child.id === id) return root;
    const found = findParent(child, id);
    if (found) return found;
  }
  return undefined;
}

/** Applies `fn` to the node with the given id, copying the path down to it. */
export function mapNode(
  root: GanttObject,
  id: number,
  fn: (node: GanttObject) => GanttObject
): GanttObject {
  if (root.id === id) return fn(root);
  if (!root.children) return root;
  let changed = false;
  const children = root.children.map((child) => {
    const next = mapNode(child, id, fn);
    if (next !== child) changed = true;
    return next;
  });
  return changed ? { ...root, children } : root;
}

export function updateNode(
  root: GanttObject,
  id: number,
  patch: Partial<GanttObject>
): GanttObject {
  return mapNode(root, id, (node) => ({ ...node, ...patch }));
}

/** The id for a new Gantt-only object: one below the smallest id of the tree (GanttTaskImpl.getNextId). */
export function nextId(root: GanttObject): number {
  let min = 0;
  const walk = (node: GanttObject) => {
    if (node.id < min) min = node.id;
    node.children?.forEach(walk);
  };
  walk(root);
  return min - 1;
}

export function addChild(
  root: GanttObject,
  parentId: number,
  child: GanttObject
): GanttObject {
  return mapNode(root, parentId, (node) => ({
    ...node,
    children: [...(node.children ?? []), child],
  }));
}

export function removeNode(root: GanttObject, id: number): GanttObject {
  const parent = findParent(root, id);
  if (!parent) return root;
  return mapNode(root, parent.id, (node) => ({
    ...node,
    children: (node.children ?? []).filter((c) => c.id !== id),
  }));
}

/** Moves the node below a new parent, as its last child. A move into its own sub tree is ignored. */
export function moveNode(
  root: GanttObject,
  id: number,
  newParentId: number
): GanttObject {
  const node = findNode(root, id);
  if (!node || findNode(node, newParentId)) return root;
  return addChild(removeNode(root, id), newParentId, node);
}

/** "Make invisible": the node and its whole sub tree (GanttTaskImpl.setInvisible). */
export function setInvisible(root: GanttObject, id: number): GanttObject {
  const hide = (node: GanttObject): GanttObject => ({
    ...node,
    visible: false,
    children: node.children?.map(hide) ?? node.children,
  });
  return mapNode(root, id, hide);
}

/** "Visible incl. structure sub elements": the node and its direct children. */
export function setChildrenVisible(root: GanttObject, id: number): GanttObject {
  return mapNode(root, id, (node) => ({
    ...node,
    visible: true,
    children:
      node.children?.map((c) => ({ ...c, visible: true })) ?? node.children,
  }));
}

/**
 * The rows the table shows: the root's descendants in tree order, the children of a closed node left out.
 * With `onlyVisibles` a node is dropped as soon as it or an ancestor below the root is invisible, as the
 * Wicket table hides it.
 */
export function flattenRows(
  root: GanttObject | null | undefined,
  openNodes: ReadonlySet<number>,
  onlyVisibles: boolean
): GanttRow[] {
  const rows: GanttRow[] = [];
  const walk = (parent: GanttObject, depth: number) => {
    for (const node of parent.children ?? []) {
      if (onlyVisibles && !node.visible) continue;
      const hasChildren = (node.children?.length ?? 0) > 0;
      const open = hasChildren && openNodes.has(node.id);
      rows.push({ node, depth, parentId: parent.id, hasChildren, open });
      if (open) walk(node, depth + 1);
    }
  };
  if (root) walk(root, 0);
  return rows;
}
