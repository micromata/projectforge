/**
 * Dotted path access for row and data objects coming from the backend.
 *
 * The backend addresses fields by their property path relative to the DTO, e.g. `title` or
 * `task.id` (column `field`s of a grid, value paths of an import), so cells read them by path.
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
