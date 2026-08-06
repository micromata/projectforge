import type { DataObject } from "./path";
import type {
  DynamicPageResponse,
  DynamicUIResponse,
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
}

/** Turns a server answer into the initial state of the page. */
export function seedState(response: DynamicPageResponse): LayoutState {
  return {
    ui: response.ui,
    data: response.data ?? {},
    variables: response.variables ?? {},
    validationErrors: response.validationErrors ?? [],
    serverData: response.serverData,
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
  };
}
