"use client";

import { useCallback, useEffect, useRef } from "react";
import { useSearchParams } from "next/navigation";
import { useRowSelection, type RowSelection } from "@/components/data-table";
import {
  cancelMultiSelection,
  MULTI_SELECTION_PARAM,
  selectEntries,
  startMultiSelection,
} from "@/lib/rs/multi-select";
import { useEntitySelection, useSelectionStore } from "@/store/selection-store";
import type { MagicFilter } from "@/lib/rs/types";

/** How long a change of the ticks waits before it is posted, as the column state waits. */
const DEBOUNCE_MS = 500;

export interface ListSelection {
  /** Whether the list is in selection mode: checkboxes, keyboard, and a click that selects. */
  active: boolean;
  /** The selection itself, or undefined while the mode is off — the table renders nothing then. */
  selection?: RowSelection;
  /** The ids the user ticked, also while the mode is off (they are remembered). */
  selectedIds: number[];
  enter: () => void;
  /** Leaves the mode, drops the ticks, and tells the backend to forget them. */
  leave: () => void;
  /** Posts a pending change of the ticks at once — before routing to the mass update page. */
  flush: () => Promise<void>;
}

/**
 * The selection mode of a list: who owns the ticks, and how they reach the HTTP session.
 *
 * Three calls of the backend's protocol, in this order and not interchangeable
 * (`MultiSelectionSupport`): `{entity}/startSelection` registers every id the filter matched and
 * *replaces* the session context, `{page}/select` narrows it to the ticked ones, `{page}/cancel`
 * drops it. So entering the mode and every later change of the filter re-register, and the ticks are
 * posted after that has landed — which is what the registration promise below is for.
 *
 * The ticks live in the store rather than here, because they outlive the list: the mass update page
 * unmounts it, and coming back must find them (see selection-store, and `listMeta.selectedIds` for
 * the reload case).
 */
export function useListSelection({
  entity,
  endpoint,
  filter,
  restoredIds,
  displayedRowIds,
}: {
  entity: string;
  /** REST base of the mass update page (`invoiceSelected`); undefined for a list without one. */
  endpoint?: string;
  /** The filter the list is showing, i.e. the entries that may be picked from. */
  filter: MagicFilter;
  /** What the session still had ticked, from `listMeta` — restored once, see the store's `restore`. */
  restoredIds?: number[];
  displayedRowIds: () => string[];
}): ListSelection {
  const { active, rows } = useEntitySelection(entity);
  // Selected one by one: the actions are stable, so nothing here re-renders on another list's ticks.
  const setRows = useSelectionStore((state) => state.setRows);
  const enterMode = useSelectionStore((state) => state.enter);
  const leaveMode = useSelectionStore((state) => state.leave);
  const restoreRows = useSelectionStore((state) => state.restore);

  const setState = useCallback<RowSelection["setState"]>(
    (value) => setRows(entity, value),
    [setRows, entity]
  );
  const selection = useRowSelection(displayedRowIds, {
    state: rows,
    setState,
  });
  const { selectedIds } = selection;

  /**
   * The filter the session is currently registered for. Guards the registration effect below against
   * re-running `startSelection` when nothing about the registration changed — a `startSelection`
   * rebuilds the whole session context and resets its `selectedIdList` (`MultiSelectionSupport
   * .registerEntityIdsForSelection`, "Clear session"), so a redundant one only risks dropping the
   * ticks it would then have to restate.
   */
  const registeredFilter = useRef<string | null>(null);

  // The restore itself decides whether it applies (it declines once the entity has an entry), so this
  // may run again when `listMeta` is refetched.
  const serializedFilter = JSON.stringify(filter);
  useEffect(() => {
    if (!endpoint || !restoredIds?.length) return;
    restoreRows(entity, restoredIds);
    // A session that still has a selection is already registered for the filter the list just loaded
    // with (both are the persisted magic filter); adopt that as the current registration so the
    // effect below does not re-register — which, on a reload, would clear the very ids restored here
    // and lose them if the reload interrupted it before the paired `select` landed.
    registeredFilter.current ??= serializedFilter;
  }, [entity, endpoint, restoredIds, restoreRows, serializedFilter]);

  // A list opened *for* a mass update comes with the mode already on (see MULTI_SELECTION_PARAM).
  const requested = useMultiSelectionRequest();
  const honoured = useRef(false);
  useEffect(() => {
    if (!endpoint || !requested || honoured.current) return;
    // Once per mount, so leaving the mode is final: the parameter stays in the url until the user
    // navigates away, and without this the very next render would switch the mode straight back on.
    honoured.current = true;
    enterMode(entity);
  }, [endpoint, entity, enterMode, requested]);

  /** The `startSelection` currently in flight, which every `select` waits for. */
  const registration = useRef<Promise<unknown> | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Read inside the timeout, so a flush always posts the newest ticks rather than the ones the effect
  // that scheduled it saw.
  const latestIds = useRef(selectedIds);
  useEffect(() => {
    latestIds.current = selectedIds;
  }, [selectedIds]);

  // Registering what may be picked: on entering the mode, and again whenever the filter changed while
  // it is on — the registered set is the result set, and that is what the filter decides.
  useEffect(() => {
    if (!active || !endpoint) return;
    // Nothing to do when the session is already registered for this filter — a fresh mount that
    // restored an existing selection (see above), or a re-render that did not change the filter. Only
    // a genuine change re-registers, so the ticks restored from the session are not thrown away.
    if (registeredFilter.current === serializedFilter) return;
    registeredFilter.current = serializedFilter;
    // Chained onto the registration in flight rather than fired alongside it, so the calls reach the
    // backend in the order the filters changed. `startSelection` *replaces* the whole session context,
    // and two registrations racing (a slow one over a wide filter, a fast one over a narrow) do not
    // finish in the order they started — so an older, slower one could land last and leave the session
    // describing a filter the list no longer shows, dropping the ticks with it (the reason a reload
    // right after a filter change would come back with nothing selected). Serialized, the newest
    // filter is always the last to register, and its `select` the last to narrow.
    const previous = registration.current ?? Promise.resolve();
    const running = previous
      .catch(() => {})
      .then(() => startMultiSelection(entity, filter))
      // The ticks are restated right after, because `startSelection` replaces the whole session
      // context and takes the ticked subset with it (`MultiSelectionSupport
      // .registerEntityIdsForSelection`, "Clear session"). Without this, changing the filter while
      // the mode is on would silently drop what the user had picked — and the selection is meant to
      // hold across a filter change, entries outside the new result set included.
      .then(() => selectEntries(endpoint, latestIds.current))
      .catch(() => {
        // A failed registration surfaces on the mass update page, which reports what it found; the
        // list itself must not throw a toast at a user who only ticked a row.
      });
    registration.current = running;
    // The filter is serialized into the key rather than compared by identity: it is rebuilt on every
    // render of the query hook.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, endpoint, entity, serializedFilter]);

  const post = useCallback(async () => {
    if (!endpoint) return;
    timer.current = null;
    // The narrowing is only meaningful over a registered set, and `startSelection` replaces the whole
    // session context — so a `select` that overtook it would be thrown away again.
    await registration.current;
    await selectEntries(endpoint, latestIds.current).catch(() => {
      // Same as the registration: the mass update page is where a broken selection is reported.
    });
  }, [endpoint]);
  // Read by the unmount flush below, which must not be re-registered on every render — that is what
  // would turn its cleanup into "post on every change" instead of "post when leaving".
  const postRef = useRef(post);
  useEffect(() => {
    postRef.current = post;
  }, [post]);

  // Debounced: clicking through a range of twenty rows posts once, not twenty times.
  const serializedIds = selectedIds.join(",");
  useEffect(() => {
    if (!active || !endpoint) return;
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => void postRef.current(), DEBOUNCE_MS);
  }, [active, endpoint, serializedIds]);

  // And flushed when the list goes away, so navigating to the mass update page (or to Wicket) cannot
  // outrun the write that page reads. Mount-only, hence the ref above.
  useEffect(
    () => () => {
      if (!timer.current) return;
      clearTimeout(timer.current);
      void postRef.current();
    },
    []
  );

  const enter = useCallback(() => enterMode(entity), [enterMode, entity]);

  const leave = useCallback(() => {
    // Before the store is cleared: a pending write would otherwise post the ticks the cancel below is
    // about to drop, and the session would come back with them.
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    leaveMode(entity);
    registration.current = null;
    // So re-entering the mode registers afresh rather than trusting a session the cancel below drops.
    registeredFilter.current = null;
    if (endpoint) void cancelMultiSelection(endpoint).catch(() => {});
  }, [leaveMode, entity, endpoint]);

  const flush = useCallback(async () => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    await post();
  }, [post]);

  return {
    active,
    selection: active ? selection : undefined,
    selectedIds,
    enter,
    leave,
    flush,
  };
}

/**
 * Whether the url asks for the selection mode — `?multiSelectionMode=true`, as the backend links to a
 * list that is opened in order to mass update from it (`PagesResolver.getMultiSelectionPageUrl`).
 *
 * `useSearchParams()` needs a `Suspense` boundary above it under `output: "export"`; the one in the
 * authenticated layout covers every list page.
 */
function useMultiSelectionRequest(): boolean {
  return useSearchParams().get(MULTI_SELECTION_PARAM) === "true";
}
