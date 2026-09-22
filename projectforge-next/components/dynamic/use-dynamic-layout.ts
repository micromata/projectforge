"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { applyPatch, type DataObject } from "@/lib/dynamic/path";
import {
  mergeUpdate,
  seedState,
  type LayoutState,
} from "@/lib/dynamic/layout-state";
import { triggeredWatchFields } from "@/lib/dynamic/watch-fields";
import type { DynamicPageResponse, ValidationError } from "@/lib/rs/types";

/** Delay before a changed watch field is reported, so typing does not fire a call per keystroke. */
const WATCH_FIELDS_DEBOUNCE_MS = 150;

export interface DynamicLayoutStore extends LayoutState {
  isFetching: boolean;
  /** The committed state - for action callbacks, which must not send a stale form. */
  readState: () => LayoutState;
  setData: (patch: DataObject) => void;
  setVariables: (patch: DataObject) => void;
  setValidationErrors: (errors: ValidationError[]) => void;
  setIsFetching: (fetching: boolean) => void;
  /** Applies an UPDATE payload: replaces the state, or merges into it when the server says so. */
  applyUpdate: (payload: DataObject, merge?: boolean) => void;
}

/**
 * State of one dynamic page: the server's layout plus the data the user edits.
 *
 * `ui` comes from TanStack Query and is re-seeded whenever the query returns; `data` is local state
 * (it changes on every keystroke and must not land in the query cache). Writes go through dotted
 * paths - the backend addresses `task.id`, not a flat key - and any write to a field listed in
 * `ui.watchFields` reports back to the server via [onWatchFields].
 */
export function useDynamicLayoutState(
  initial: DynamicPageResponse,
  onWatchFields?: (triggered: string[]) => void
): DynamicLayoutStore {
  const [state, setState] = useState<LayoutState>(() => seedState(initial));
  const [isFetching, setIsFetching] = useState(false);

  // A fresh query result (initial load, refetch after a RELOAD) wins over the local copy. Done as
  // a render phase update rather than in an effect, so no render ever shows the stale state.
  const [seed, setSeed] = useState(initial);
  if (seed !== initial) {
    setSeed(initial);
    setState(seedState(initial));
  }

  // Callbacks are handed to the action interpreter and to the input components, which may hold on
  // to them; the refs give them the committed values instead of the ones of their own render.
  const stateRef = useRef(state);
  const watchFieldsRef = useRef(onWatchFields);
  useEffect(() => {
    stateRef.current = state;
    watchFieldsRef.current = onWatchFields;
  });

  const readState = useCallback(() => stateRef.current, []);

  const pendingWatchFields = useRef<Set<string>>(new Set());
  const watchTimer = useRef<number | null>(null);

  const flushWatchFields = useCallback(() => {
    watchTimer.current = null;
    const triggered = [...pendingWatchFields.current];
    pendingWatchFields.current.clear();
    if (triggered.length > 0) watchFieldsRef.current?.(triggered);
  }, []);

  useEffect(
    () => () => {
      if (watchTimer.current !== null) window.clearTimeout(watchTimer.current);
    },
    []
  );

  const setData = useCallback(
    (patch: DataObject) => {
      setState((prev) => ({ ...prev, data: applyPatch(prev.data, patch) }));

      const triggered = triggeredWatchFields(
        patch,
        stateRef.current.ui.watchFields
      );
      if (triggered.length === 0) return;
      // Collect the fields touched while typing and report them in one call.
      triggered.forEach((field) => pendingWatchFields.current.add(field));
      if (watchTimer.current !== null) window.clearTimeout(watchTimer.current);
      watchTimer.current = window.setTimeout(
        flushWatchFields,
        WATCH_FIELDS_DEBOUNCE_MS
      );
    },
    [flushWatchFields]
  );

  const setVariables = useCallback((patch: DataObject) => {
    setState((prev) => ({
      ...prev,
      variables: { ...prev.variables, ...patch },
    }));
  }, []);

  const setValidationErrors = useCallback((errors: ValidationError[]) => {
    setState((prev) => ({ ...prev, validationErrors: errors }));
  }, []);

  const applyUpdate = useCallback((payload: DataObject, merge?: boolean) => {
    setState((prev) => mergeUpdate(prev, payload, merge));
  }, []);

  return {
    ...state,
    isFetching,
    readState,
    setData,
    setVariables,
    setValidationErrors,
    setIsFetching,
    applyUpdate,
  };
}
