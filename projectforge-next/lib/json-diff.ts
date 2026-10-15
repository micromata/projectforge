import {
  create,
  type ArrayDelta,
  type Delta,
  type ObjectDelta,
} from "jsondiffpatch";

/**
 * One changed place of a JSON value: where (the keys down to it, an array element named by its
 * `name`, else its `key` or `id`) and the values removed and added there. A changed scalar has one of each, a list of
 * scalars (customer numbers, texts) only the entries that came and went.
 */
export interface JsonChange {
  path: string[];
  removed: string[];
  added: string[];
}

/** The first of the given properties holding a string or number, as a string. */
function firstOf(item: object, props: string[]): string | undefined {
  const values = item as Record<string, unknown>;
  const found = props
    .map((prop) => values[prop])
    .find((value) => typeof value === "string" || typeof value === "number");
  return found === undefined ? undefined : String(found);
}

/** What identifies an array element across versions, so a renamed or reordered entry is not new. */
const identity = (item: object) => firstOf(item, ["key", "id", "name"]);

/** How an array element is named in a path: by what a reader recognizes, its name first. */
const labelOf = (item: object) => firstOf(item, ["name", "key", "id"]);

const differ = create({
  objectHash: (item, index) => identity(item) ?? `$$index:${index}`,
  // Moves are detected only to be left out (see walk): the order of a list carries no meaning in a
  // configuration, and without detection a reordered element would show as removed and added.
  arrays: { detectMove: true, includeValueOnMove: false },
});

/** The value of a JSON object or array, null for anything else (a plain text, a broken value). */
export function parseJsonContainer(raw: string | null | undefined): unknown {
  const text = raw?.trim();
  if (!text || !(text.startsWith("{") || text.startsWith("["))) return null;
  try {
    const value: unknown = JSON.parse(text);
    return value !== null && typeof value === "object" ? value : null;
  } catch {
    return null;
  }
}

/**
 * The changes between two JSON values, in document order. Changes of one list of scalars are merged into a
 * single entry, so a group's customers read "−4711 +4712" rather than as two lines.
 */
export function jsonChanges(left: unknown, right: unknown): JsonChange[] {
  const changes: JsonChange[] = [];
  walk(differ.diff(left, right), left, right, [], changes);
  const merged = new Map<string, JsonChange>();
  for (const change of changes) {
    const id = JSON.stringify(change.path);
    const known = merged.get(id);
    if (known) {
      known.removed.push(...change.removed);
      known.added.push(...change.added);
    } else merged.set(id, change);
  }
  return [...merged.values()];
}

function walk(
  delta: Delta,
  left: unknown,
  right: unknown,
  path: string[],
  out: JsonChange[]
): void {
  if (delta === undefined) return;
  if (Array.isArray(delta)) {
    // [new] added, [old, new] modified, [old, 0, 0] deleted, [_, to, 3] moved (skipped); text diffs
    // ([_, 0, 2]) are not configured.
    if (delta.length === 1) out.push(change(path, [], [delta[0]]));
    else if (delta.length === 2) out.push(change(path, [delta[0]], [delta[1]]));
    else if (delta[2] === 0) out.push(change(path, [delta[0]], []));
    return;
  }
  if ((delta as ArrayDelta)._t === "a") {
    walkArray(delta as ArrayDelta, left, right, path, out);
    return;
  }
  for (const [prop, sub] of Object.entries(delta as ObjectDelta)) {
    walk(sub, member(left, prop), member(right, prop), [...path, prop], out);
  }
}

function walkArray(
  delta: ArrayDelta,
  left: unknown,
  right: unknown,
  path: string[],
  out: JsonChange[]
): void {
  const entries = Object.entries(delta).filter(([index]) => index !== "_t");
  for (const [index, sub] of entries) {
    const removedAt = index.startsWith("_");
    const position = Number(removedAt ? index.slice(1) : index);
    const element = member(removedAt ? left : right, position);
    // A scalar element added or removed belongs to the list, an object element is a place of its own.
    if (!isContainer(element)) {
      walk(sub, undefined, undefined, path, out);
      continue;
    }
    const label = labelOf(element) ?? `#${position + 1}`;
    // Only a modified element (an object delta at its new index) has both versions to descend into.
    const before = removedAt
      ? element
      : Array.isArray(sub)
        ? undefined
        : counterpart(left, element, position);
    walk(sub, before, removedAt ? undefined : element, [...path, label], out);
  }
}

/** The element of the old array a new one was matched with: by identity, else at the same position. */
function counterpart(left: unknown, element: object, position: number) {
  const id = identity(element);
  const items = Array.isArray(left) ? left : [];
  return (
    (id !== undefined
      ? items.find((item) => isContainer(item) && identity(item) === id)
      : undefined) ?? items[position]
  );
}

function change(path: string[], removed: unknown[], added: unknown[]) {
  return { path, removed: removed.map(show), added: added.map(show) };
}

function member(value: unknown, prop: string | number): unknown {
  return isContainer(value)
    ? (value as Record<string | number, unknown>)[prop]
    : undefined;
}

function isContainer(value: unknown): value is object {
  return value !== null && typeof value === "object";
}

/** A string as it is, anything else as compact JSON. */
function show(value: unknown): string {
  return typeof value === "string" ? value : JSON.stringify(value);
}
