"use client";

import { useMediaQuery } from "@/hooks/use-media-query";

/** The `cramped` variant of app/globals.css: narrow or low, a phone in either orientation. */
const CRAMPED_QUERY = "(max-width: 39.99rem), (max-height: 31.99rem)";

/**
 * Whether the screen is `cramped` (see app/globals.css), for what CSS alone can't switch, e.g. where an element is
 * rendered. False during server rendering and the first client render.
 */
export function useCramped(): boolean {
  return useMediaQuery(CRAMPED_QUERY);
}
