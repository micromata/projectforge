"use client";

import { useMediaQuery } from "@/hooks/use-media-query";

/**
 * Whether the primary pointer is coarse — a finger rather than a mouse. True on phones and tablets,
 * false on a desktop with a mouse or trackpad.
 *
 * The one signal for "there is no hover here": a hover-only affordance (a tooltip that opens on
 * `pointerover`) reaches nothing on such a device, so the touch-reachable variant (a tap-to-open
 * popover) is layered on where this is true. `(pointer: coarse)` rather than a width breakpoint,
 * because the question is the input device, not the screen size — a small window on a desktop still
 * has a mouse, a large tablet still has none.
 *
 * Returns `false` during SSR and the first client render (see {@link useMediaQuery}); the hover
 * behaviour is the default and the tap behaviour is layered over it, so that default is the safe one.
 */
export function useCoarsePointer(): boolean {
  return useMediaQuery("(pointer: coarse)");
}
