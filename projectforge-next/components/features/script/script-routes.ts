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
