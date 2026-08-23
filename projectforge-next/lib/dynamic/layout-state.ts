import type { DataObject } from "./path";
import type {
  DynamicUIResponse,
  InitialListData,
  MagicFilter,
  ServerData,
  ValidationError,
} from "@/lib/rs/types";

/** Everything a dynamic page holds between two server calls. */
export interface LayoutState {
  ui: DynamicUIResponse;
  data: DataObject;
  variables: DataObject;
  validationErrors: ValidationError[];
  serverData?: ServerData;
  /**
   * The filter of a list page, kept beside the data because it is not part of it: the search button
   * sends it as the whole request body (see useDynamicActions), and the backend answers `filterReset`
   * with the filter it fell back to.
   *
   * Only list pages have one — an edit page's state leaves it unset.
   */
  filter?: MagicFilter;
}

/**
 * Turns a server answer into the initial state of the page.
 *
 * Typed as the list answer, which is the wider one: every field it adds is optional, so an edit
 * page's `DynamicPageResponse` fits it — and a list page's filter arrives instead of being dropped.
 */
export function seedState(response: InitialListData): LayoutState {
  return {
    ui: response.ui,
    data: response.data ?? {},
    variables: response.variables ?? {},
    validationErrors: response.validationErrors ?? [],
    serverData: response.serverData,
    filter: response.filter,
  };
}

/**
 * Applies the payload of an UPDATE action.
 *
 * The payload carries the known keys at its top level (see AbstractPagesRest, which sends them as
 * variables named "data", "ui" and "variables"); everything else is a plain variable the layout
 * may reference. `merge` decides whether the values extend the current state or replace it - the
 * backend sets it when it only sends the parts it changed.
 */
export function mergeUpdate(
  state: LayoutState,
  payload: DataObject,
  merge?: boolean
): LayoutState {
  const {
    data: newData,
    ui: newUi,
    variables: newVariables,
    serverData: newServerData,
    validationErrors: newErrors,
    filter: newFilter,
    ...rest
  } = payload;

  const extraVariables = {
    ...(newVariables as DataObject | undefined),
    ...rest,
  };
  return {
    ui: (newUi as DynamicUIResponse | undefined) ?? state.ui,
    data: newData
      ? merge
        ? { ...state.data, ...(newData as DataObject) }
        : (newData as DataObject)
      : state.data,
    variables:
      Object.keys(extraVariables).length > 0
        ? merge
          ? { ...state.variables, ...extraVariables }
          : extraVariables
        : state.variables,
    validationErrors:
      (newErrors as ValidationError[] | undefined) ?? state.validationErrors,
    serverData: (newServerData as ServerData | undefined) ?? state.serverData,
    // Not a variable of the layout: a list answer (`list`, `filterReset`) carries the filter it
    // actually used, and that is what the next search has to send.
    filter: (newFilter as MagicFilter | undefined) ?? state.filter,
  };
}
