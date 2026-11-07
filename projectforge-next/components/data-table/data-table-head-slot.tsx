"use client";

import { createContext, type ReactNode } from "react";

/**
 * Content a page puts at the top of a DataTable's own scroll area, above the rows: it scrolls away with them, while
 * the sticky column header stays. ListPageShell puts the list's header there on a phone, where it would leave the
 * table no room and nested scrolling doesn't hand a swipe on (iOS) — so there is one scroller only.
 *
 * Read by the next DataTable below the provider only: that one resets it for the tables inside it (an embedded table
 * of a row detail).
 */
export const DataTableHeadSlot = createContext<ReactNode>(null);
