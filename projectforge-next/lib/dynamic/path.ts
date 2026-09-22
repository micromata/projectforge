/**
 * Dotted path access for the data object of a dynamic (UILayout) page.
 *
 * The backend addresses fields by their property path relative to the DTO, e.g. `title` or
 * `task.id`, and uses the very same string as the element's `id`, as the `fieldId` of a
 * validation error and as an entry of `UILayout.watchFields`. So the whole protocol is built
 * on dotted paths, and reading/writing them is the single most used operation of the renderer.
 *
 * (The legacy React app did this by monkey patching `Object.getByString` onto the prototype -
 * see projectforge-webapp/src/utilities/global.js. These are plain functions instead.)
 */

/** A plain JSON object. Values may be nested objects, arrays or primitives. */
export type DataObject = Record<string, unknown>;

/**
 * Reads the value at a dotted path. Returns undefined if any segment along the way is missing
 * or not an object, so a missing intermediate never throws.
 */
export function getByPath(data: DataObject | undefined, path: string): unknown {
  if (!data || !path) return undefined;
  let current: unknown = data;
  for (const segment of path.split(".")) {
    if (current === null || typeof current !== "object") return undefined;
    current = (current as DataObject)[segment];
  }
  return current;
}

/**
 * Returns a copy of `data` with the value at the dotted path replaced. Only the objects along
 * the path are cloned (structural sharing), so React state comparisons on untouched branches
 * still see the same references.
 *
 * Missing intermediate objects are created. An intermediate that exists but is not an object
 * (or is an array) is replaced by an object - the backend never asks us to write into an array
 * element, so there is no index handling.
 */
export function setByPath(
  data: DataObject,
  path: string,
  value: unknown
): DataObject {
  const segments = path.split(".");
  const result = { ...data };
  let cursor: DataObject = result;
  for (let i = 0; i < segments.length - 1; i++) {
    const segment = segments[i];
    const child = cursor[segment];
    cursor[segment] =
      child !== null && typeof child === "object" && !Array.isArray(child)
        ? { ...(child as DataObject) }
        : {};
    cursor = cursor[segment] as DataObject;
  }
  cursor[segments[segments.length - 1]] = value;
  return result;
}

/**
 * Applies a patch of dotted paths to `data`, returning a new object.
 *
 * This is what the renderer's `setData` receives: `{ "task.id": 42 }` must reach the backend as
 * a nested `task: { id: 42 }`, not as a literal key `"task.id"` - the Kotlin DTO cannot
 * deserialize the latter.
 */
export function applyPatch(data: DataObject, patch: DataObject): DataObject {
  return Object.entries(patch).reduce(
    (acc, [path, value]) => setByPath(acc, path, value),
    data
  );
}

/**
 * Flattens an object into dotted paths, e.g. `{ task: { id: 1 } }` -> `["task.id"]`.
 * Arrays and null are treated as leaves, matching how the backend names watch fields.
 */
export function flattenPaths(data: DataObject, prefix = ""): string[] {
  return Object.entries(data).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value !== null && typeof value === "object" && !Array.isArray(value)) {
      const nested = flattenPaths(value as DataObject, path);
      // An empty object is a leaf itself (e.g. clearing a reference to {}).
      return nested.length > 0 ? nested : [path];
    }
    return [path];
  });
}
