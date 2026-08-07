import { request } from "./client";

/**
 * The column state the backend keeps in the user's prefs. TanStack Table's own
 * state shape, mirroring GridState / DataTableStateRequest — so the wire format
 * needs no translation in either direction.
 */
export interface ColumnStateDto {
  columnOrder?: string[];
  columnSizing?: Record<string, number>;
  columnVisibility?: Record<string, boolean>;
  columnPinning?: { left?: string[]; right?: string[] };
  sorting?: { id: string; desc: boolean }[];
  columnFilters?: { id: string; value: unknown }[];
}

/**
 * Reads the state from an explicit URL.
 *
 * The URL primitives exist because a UILayout page doesn't reliably have an
 * entity category: it gets `onColumnStatesChangedUrl` / `resetGridStateUrl` from
 * the layout, and for a service endpoint like TaskServicesRest those don't follow
 * the `/rs/<category>/…` shape the entity variants below assume.
 */
export function fetchColumnStatesFromUrl(
  url: string,
  signal?: AbortSignal
): Promise<ColumnStateDto> {
  return request<ColumnStateDto>(url, { method: "GET" }, signal);
}

export function saveColumnStatesToUrl(
  url: string,
  state: ColumnStateDto,
  signal?: AbortSignal
): Promise<unknown> {
  return request<unknown>(
    url,
    { method: "POST", body: JSON.stringify(state) },
    signal
  );
}

export function fetchColumnStates(
  entity: string,
  signal?: AbortSignal
): Promise<ColumnStateDto> {
  return fetchColumnStatesFromUrl(`/rs/${entity}/columnStates`, signal);
}

export function saveColumnStates(
  entity: string,
  state: ColumnStateDto,
  signal?: AbortSignal
): Promise<unknown> {
  return saveColumnStatesToUrl(`/rs/${entity}/setColumnStates`, state, signal);
}
