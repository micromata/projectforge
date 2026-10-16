/** The routes of the address pages. A row opens the view; the form is a sub route of it. */
export const ADDRESS_ROUTE = "/address";

/** The VCF import, whose entries are taken over one by one in the form. */
export const ADDRESS_IMPORT_ROUTE = "/address/import";

/** React Query key of the list, so a write or a toggled favorite refreshes it. */
export const ADDRESS_LIST_QUERY_KEY = ["address"] as const;

export function addressViewRoute(id: number): string {
  return `${ADDRESS_ROUTE}/${id}`;
}

export function addressEditRoute(id: number): string {
  return `${ADDRESS_ROUTE}/${id}/edit`;
}
