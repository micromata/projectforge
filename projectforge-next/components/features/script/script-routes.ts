/** The routes of the script pages, for the links between them. */
export const SCRIPT_ROUTE = "/script";
export const MY_SCRIPT_ROUTE = "/myscript";

/** Ad-hoc code, not stored; `?example=n` starts with an example script. */
export const SCRIPT_EXECUTE_ROUTE = `${SCRIPT_ROUTE}/execute`;

/** The execution of a stored script — what a row of either list opens. */
export const scriptExecuteRoute = (route: string, id: number) =>
  `${route}/${id}`;

/** The form of a stored script (administration only). */
export const scriptEditRoute = (id: number) => `${SCRIPT_ROUTE}/${id}/edit`;

/** The search parameter of the execution page naming the page the script was started from. */
export const SCRIPT_FROM_PARAM = "from";

/**
 * The execution of a script started by its button on the page of `target` (see ScriptPageButtons), whose
 * current filter the script gets.
 */
export const scriptPageButtonRoute = (id: number, target: string) =>
  `${MY_SCRIPT_ROUTE}/${id}?${SCRIPT_FROM_PARAM}=${encodeURIComponent(target)}`;
