"use client";

import { useCallback, useMemo, useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import {
  DEFAULT_TASK_TREE_FILTER,
  fetchTaskInfo,
  fetchTaskTree,
  taskTreeFilterOf,
  type TaskNode,
  type TaskTreeFilter,
} from "@/lib/rs/task";

/** Which node the last chevron click asked the server to open or close. */
interface Toggle {
  open?: number;
  close?: number;
  /**
   * Counts the clicks. Without it, collapsing a node the user had opened before would produce a
   * query key that was already in the cache, and the tree would flash the state it had back then
   * before the refetch replaced it.
   */
  revision: number;
}

interface UseTaskTreeOptions {
  highlightTaskId?: number | null;
  showRootForAdmins?: boolean;
  selectMode?: boolean;
  /**
   * Let the tree be re-rooted at a subtree, navigated by a breadcrumb (the select panel). When on and a
   * task is selected, the tree opens at that task's parent — its neighbourhood — rather than at the whole
   * tree; the breadcrumb then climbs back up. Off for the plain tree page, which always starts at the root.
   */
  rootNavigable?: boolean;
}

/** The visited pseudo-roots, so the breadcrumb's back/forward can step through them (`null` = whole tree). */
interface RootHistory {
  stack: (number | null)[];
  index: number;
}

/**
 * One array identity for "no rows yet".
 *
 * A fresh `[]` per render would be a new `data` for the table on every render, and TanStack resets
 * its row model when `data` changes — a state update during render, which re-renders, which produces
 * the next `[]`. That is a synchronous commit loop: the tab goes to 100% CPU and stops responding.
 */
const NO_NODES: TaskNode[] = [];

/**
 * The visible structure tree, its columns and its filter.
 *
 * Two queries, one request on mount. The first call is the only one with `initial=true`, which is
 * what makes the backend send the column defs, the grid-state urls and the filter it has in the
 * session — and what makes it *ignore* filter parameters. It never goes stale while the panel is
 * open (but is re-read on each mount, so browser-back shows the current server state — see the query
 * below), so the second query only exists from the first interaction on: as long as the user has
 * neither filtered nor expanded anything, the initial answer *is* the tree.
 *
 * Expanding is a request, not local state (see lib/rs/task.ts), so both queries answer with the
 * whole visible tree and the panel has no expansion model of its own.
 *
 * Everything returned is referentially stable across renders — the columns are memoised on the
 * toggle callback, and the table's `data` must not change identity unless the rows did (see
 * [NO_NODES]).
 */
export function useTaskTree({
  highlightTaskId,
  showRootForAdmins,
  selectMode,
  rootNavigable,
}: UseTaskTreeOptions) {
  // null while the session's filter is in effect: only the initial answer knows what that is, and
  // overriding it with a default here would drop a filter set on the legacy page.
  const [filter, setFilter] = useState<TaskTreeFilter | null>(null);
  const [toggle, setToggle] = useState<Toggle>();
  /**
   * Counts the resets, for the same reason [Toggle.revision] counts the clicks — and here it is not
   * merely cosmetic: a non-initial call *stores* its parameters as the user's filter, so a request
   * served from the cache leaves the session with the old one. Resetting after a search lands exactly
   * there (the defaults were the key of the very first call), and without this the tree would look
   * reset while the backend still held the search string.
   */
  const [resetRevision, setResetRevision] = useState(0);
  /**
   * The very filter object the last reset installed, so this render can tell "the user cleared the
   * field" from "the reset button did it" — see [searchString], which skips the debounce for the
   * latter. Compared by identity: the next `setFilter` produces a new object, so typing resumes
   * debouncing by itself.
   */
  const [resetFilterValue, setResetFilterValue] =
    useState<TaskTreeFilter | null>(null);

  // The parent of the selected task is where a re-rootable tree opens: the neighbourhood of the current
  // value rather than the whole tree. Its id is the last of the selection's ancestors (path excludes the
  // task itself), so the selection's info is fetched for it — shared by key with the breadcrumb's own fetch.
  const selectedInfo = useQuery({
    queryKey: ["taskInfo", highlightTaskId],
    queryFn: ({ signal }) => fetchTaskInfo(highlightTaskId!, signal),
    enabled: !!rootNavigable && highlightTaskId != null,
    staleTime: Infinity,
  });
  const autoRootTaskId = rootNavigable
    ? (selectedInfo.data?.path?.at(-1)?.id ?? null)
    : null;

  // null until the user navigates: until then the effective root simply follows [autoRootTaskId]. Once the
  // user clicks a breadcrumb, this history takes over so back/forward can step through the visited roots.
  const [rootHistory, setRootHistory] = useState<RootHistory | null>(null);
  const rootTaskId = rootHistory
    ? (rootHistory.stack[rootHistory.index] ?? null)
    : autoRootTaskId;

  // Depends on [autoRootTaskId] so the first navigation seeds the history with the root the tree opened
  // at — its identity may change, but unlike [toggleNode] no memoised columns hang off it.
  const navigateToRoot = useCallback(
    (id: number | null) => {
      setRootHistory((prev) => {
        const base = prev ?? { stack: [autoRootTaskId], index: 0 };
        if ((base.stack[base.index] ?? null) === id) return base;
        // Drop any forward entries the way a browser does, then append the new root.
        const stack = base.stack.slice(0, base.index + 1);
        stack.push(id);
        return { stack, index: stack.length - 1 };
      });
    },
    [autoRootTaskId]
  );
  const goBack = useCallback(
    () =>
      setRootHistory((prev) =>
        prev && prev.index > 0 ? { ...prev, index: prev.index - 1 } : prev
      ),
    []
  );
  const goForward = useCallback(
    () =>
      setRootHistory((prev) =>
        prev && prev.index < prev.stack.length - 1
          ? { ...prev, index: prev.index + 1 }
          : prev
      ),
    []
  );
  const canGoBack = !!rootHistory && rootHistory.index > 0;
  const canGoForward =
    !!rootHistory && rootHistory.index < rootHistory.stack.length - 1;

  const scope = useMemo(
    () => ({
      highlightedTaskId: highlightTaskId ?? undefined,
      showRootForAdmins: showRootForAdmins || undefined,
      // In the scope, so it is part of both query keys: the two modes get different columns and
      // must not share a cache entry.
      select: selectMode || undefined,
      // Also in the scope: re-rooting changes the whole answer, so both queries must refetch and
      // neither root's tree may be served from the other's cache entry.
      rootTaskId: rootTaskId ?? undefined,
    }),
    [highlightTaskId, showRootForAdmins, selectMode, rootTaskId]
  );

  const initial = useQuery({
    queryKey: ["taskTree", "initial", scope],
    queryFn: ({ signal }) => fetchTaskTree({ ...scope, initial: true }, signal),
    // Stable *while the panel is open* — nothing about the initial answer changes under it, so no
    // refetch on a window focus or a re-render. But a fresh mount re-reads it: expanding a node is
    // stored in the user's prefs server-side (see TaskServicesRest.getTree, which mutates the cached
    // openNodes set), so leaving the tree to open a task and coming back with the browser must show
    // the tree as the user left it, not the folded snapshot this query first cached. `interacted`
    // resets on that remount, so the panel falls back to this answer — which therefore has to be the
    // current one.
    staleTime: Infinity,
    refetchOnMount: "always",
  });

  const initFilter = initial.data?.initFilter;
  const effective: TaskTreeFilter = useMemo(
    () => filter ?? taskTreeFilterOf(initFilter),
    [filter, initFilter]
  );
  // Only the search string is debounced: a checkbox is one deliberate click, a keystroke isn't.
  const debouncedSearch = useDebouncedValue(effective.searchString, 250);
  // A reset is a click as well, and its request is what stores the cleared filter server-side — going
  // out with the string still in flight would leave the session filtered by what was just discarded.
  const searchString =
    filter !== null && filter === resetFilterValue
      ? filter.searchString
      : debouncedSearch;

  // Interacting at all switches to this query. It always carries the whole filter, even for a mere
  // expand: the backend takes every parameter of a non-initial call as the user's new filter, so
  // omitting the search string would clear the one stored in the session.
  const interacted = filter !== null || toggle !== undefined;
  const params = useMemo(
    () => ({
      ...scope,
      ...effective,
      searchString,
      open: toggle?.open,
      close: toggle?.close,
    }),
    [scope, effective, searchString, toggle]
  );
  const tree = useQuery({
    // The revision belongs in the key, not in the request: it distinguishes two identical toggles
    // for the cache, and the backend has no such parameter.
    queryKey: ["taskTree", params, toggle?.revision, resetRevision],
    queryFn: ({ signal }) => fetchTaskTree(params, signal),
    enabled: interacted,
    // The tree that was on screen stays there while the next one loads — a search narrowing row by
    // row must not flash an empty table between two answers.
    placeholderData: keepPreviousData,
  });

  // The first interaction has no previous data of *this* query to keep, so the rows of the initial
  // answer stand in until the first one arrives. Without it the table would empty out on the first
  // click and fill again a moment later.
  const nodes =
    (interacted ? tree.data : initial.data)?.nodes ??
    initial.data?.nodes ??
    NO_NODES;

  const source = interacted ? tree : initial;

  return {
    /** The rows, flat and each with its `indent` and `treeStatus`. */
    nodes,
    /** Column defs, sort model and the grid-state urls — from the initial answer only. */
    grid: initial.data,
    filter: effective,
    /**
     * The search term the visible rows were actually filtered by — the debounced (or reset) value that
     * drove the request, not the live keystroke. Used to highlight the match in the rows (see
     * HighlightedText), where the live value could highlight a term the shown rows don't yet reflect.
     */
    searchTerm: searchString,
    setFilter,
    /**
     * Puts the filter back to what `TaskFilter.reset()` produces — Wicket's "Rücksetzen" button on the
     * tree form, which does exactly that and re-renders.
     *
     * No endpoint of its own, and not `filter/reset` either: that one drops the *entity's* stored
     * `MagicFilter`, while the tree keeps a `TaskFilter` in the session under its own key (see
     * ListFilterService). Setting the defaults here has the backend store them on the next call,
     * because it reads every parameter of a non-initial request as the user's new filter — so the
     * reset outlives the page just as Wicket's does. Which is also why it bumps [resetRevision]: that
     * next call has to actually go out, cache or no cache.
     */
    resetFilter: useCallback(() => {
      const defaults = { ...DEFAULT_TASK_TREE_FILTER };
      setFilter(defaults);
      setResetFilterValue(defaults);
      setResetRevision((revision) => revision + 1);
    }, []),
    isLoading: source.isLoading,
    isFetching: source.isFetching,
    isError: initial.isError || tree.isError,
    /**
     * Open or collapse a node; the server remembers it for the user.
     *
     * Stable: the columns are memoised on it, and rebuilding them per render would remount every
     * header (and, through the table's `data`/`columns`, loop the same way [NO_NODES] describes).
     */
    toggleNode: useCallback(
      (id: number, open: boolean) =>
        setToggle((previous) => ({
          [open ? "open" : "close"]: id,
          revision: (previous?.revision ?? 0) + 1,
        })),
      []
    ),
    /** The node the tree is currently rooted at, `null` for the whole tree. Drives the breadcrumb. */
    rootTaskId,
    /** Re-root the tree at a node (or `null` for the whole tree); pushes onto the back/forward history. */
    navigateToRoot,
    goBack,
    goForward,
    canGoBack,
    canGoForward,
  };
}

/**
 * Everything the tree panel works with, so a page can own the state and hand it in instead of letting
 * the panel keep it to itself — the tree page does, because its header acts on the filter (see
 * TaskTreePanelProps.tree).
 */
export type TaskTreeState = ReturnType<typeof useTaskTree>;
