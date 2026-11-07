"use client";

import { createContext } from "react";

/**
 * The filter row is folded away on a cramped screen (see ListToolbar): [FilterPills] then shows only the pills
 * with a value, and [FilterFavoritesMenu] only an applied favorite — what narrows the list stays in sight, in one
 * line, without the pills to add one.
 */
export const FilterPillsCollapsed = createContext(false);
