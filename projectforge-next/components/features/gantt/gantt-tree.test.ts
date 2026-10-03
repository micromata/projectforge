import { describe, expect, it } from "vitest";
import {
  addChild,
  findParent,
  flattenRows,
  moveNode,
  nextId,
  removeNode,
  setChildrenVisible,
  setInvisible,
  updateNode,
} from "./gantt-tree";
import type { GanttObject } from "./types";

/** root(1) → a(2) → [a1(3), a2(-1)], b(4) */
function tree(): GanttObject {
  return {
    id: 1,
    visible: true,
    children: [
      {
        id: 2,
        title: "a",
        visible: true,
        children: [
          { id: 3, title: "a1", visible: true },
          { id: -1, title: "a2", visible: false },
        ],
      },
      { id: 4, title: "b", visible: true },
    ],
  };
}

const ids = (rows: { node: GanttObject }[]) => rows.map((r) => r.node.id);

describe("gantt-tree", () => {
  it("flattens the open nodes in tree order", () => {
    expect(ids(flattenRows(tree(), new Set(), false))).toEqual([2, 4]);
    const rows = flattenRows(tree(), new Set([2]), false);
    expect(ids(rows)).toEqual([2, 3, -1, 4]);
    expect(rows[1]).toMatchObject({ depth: 1, parentId: 2 });
    expect(rows[0]).toMatchObject({ hasChildren: true, open: true });
  });

  it("drops invisible nodes and their sub trees with onlyVisibles", () => {
    expect(ids(flattenRows(tree(), new Set([2]), true))).toEqual([2, 3, 4]);
    const hidden = setInvisible(tree(), 2);
    expect(ids(flattenRows(hidden, new Set([2]), true))).toEqual([4]);
  });

  it("copies only the path to a changed node", () => {
    const before = tree();
    const after = updateNode(before, 3, { title: "x" });
    expect(after.children![0].children![0].title).toBe("x");
    expect(after.children![1]).toBe(before.children![1]);
    expect(before.children![0].children![0].title).toBe("a1");
  });

  it("hands out ids below the smallest one", () => {
    expect(nextId(tree())).toBe(-2);
    expect(nextId({ id: 5 })).toBe(-1);
  });

  it("adds, removes and moves nodes", () => {
    const added = addChild(tree(), 4, { id: -2, title: "new" });
    expect(findParent(added, -2)?.id).toBe(4);
    expect(findParent(removeNode(added, -2), -2)).toBeUndefined();
    const moved = moveNode(tree(), 3, 4);
    expect(findParent(moved, 3)?.id).toBe(4);
    // Never into its own sub tree.
    expect(moveNode(tree(), 2, 3)).toEqual(tree());
  });

  it("makes a node and its direct children visible", () => {
    const result = setChildrenVisible(setInvisible(tree(), 2), 2);
    expect(result.children![0].visible).toBe(true);
    expect(result.children![0].children!.map((c) => c.visible)).toEqual([
      true,
      true,
    ]);
  });
});
