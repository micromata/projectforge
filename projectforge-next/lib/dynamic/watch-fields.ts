import { flattenPaths, type DataObject } from "./path";

/**
 * Determines which of the layout's watch fields a data patch touched.
 *
 * `UILayout.watchFields` lists the property paths the backend wants to be told about: whenever
 * one of them changes, the client posts the whole form to `{category}/watchFields` and the
 * backend answers with an UPDATE ResponseAction, e.g. to recalculate dependent fields or to
 * swap parts of the layout. Mirrors `setCurrentData` in
 * projectforge-webapp/src/actions/form.js.
 *
 * @param patch The dotted-path patch handed to `setData`.
 * @param watchFields `UILayout.watchFields`, undefined if the page has none.
 * @returns The touched watch fields, to be sent as `PostData.watchFieldsTriggered`.
 */
export function triggeredWatchFields(
  patch: DataObject,
  watchFields?: string[]
): string[] {
  if (!watchFields || watchFields.length === 0) return [];
  // The patch may use nested objects rather than dotted keys, so flatten first. The legacy
  // implementation only looked at the top level keys and missed nested changes.
  const changed = flattenPaths(patch);
  return watchFields.filter((field) =>
    changed.some(
      // A watch field on an object ("task") must also fire for "task.id".
      (path) => path === field || path.startsWith(`${field}.`)
    )
  );
}
