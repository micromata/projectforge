/** The routes of the Merlin pages, for the links between them. */
export const MERLIN_ROUTE = "/merlin";

/** The execution of a template — what a row of the list opens. */
export const merlinExecuteRoute = (id: number) => `${MERLIN_ROUTE}/${id}`;

/** The admin form of a template. */
export const merlinEditRoute = (id: number) => `${MERLIN_ROUTE}/${id}/edit`;

/** The documentation of the Word® templates (variables, conditionals, serial execution). */
export const MERLIN_DOCUMENTATION_URL =
  "https://github.com/micromata/Merlin/blob/master/docs/templates.adoc";
