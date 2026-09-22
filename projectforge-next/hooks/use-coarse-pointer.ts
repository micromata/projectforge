"use client";

import { useSyncExternalStore } from "react";
import { useMediaQuery } from "@/hooks/use-media-query";

/** `navigator.maxTouchPoints` never changes at runtime, so the store has nothing to subscribe to. */
const NEVER_CHANGES = () => () => {};

/**
 * Whether a finger may be the pointer here — the device has a touchscreen. True on phones and tablets,
 * false on a desktop with a mouse or trackpad.
 *
 * The one signal for "there is no hover here": a hover-only affordance (a tooltip that opens on
 * `pointerover`) reaches nothing on such a device, so the touch-reachable variant (a tap-to-open
 * popover) is layered on where this is true. A media query rather than a width breakpoint, because
 * the question is the input device, not the screen size — a small window on a desktop still has a
 * mouse, a large tablet still has none.
 *
 * `(any-pointer: coarse)`, not `(pointer: coarse)`: the latter asks after the *primary* pointer, and
 * iPadOS Safari reports that as a fine mouse — it masquerades as desktop (Request-Desktop is its
 * default) even in pure finger use, and an iPad with a trackpad genuinely has a fine primary while the
 * finger still cannot hover. `any-pointer` asks whether a coarse pointer is *available at all*, which
 * holds whenever a touchscreen is present. `navigator.maxTouchPoints` is the backstop for the same
 * masquerade: WebKit cannot hide the touch points, so it stays > 0 on every iPad and iPhone regardless
 * of the desktop disguise.
 *
 * Returns `false` during SSR and the first client render (see {@link useMediaQuery}); the hover
 * behaviour is the default and the tap behaviour is layered over it, so that default is the safe one.
 */
export function useCoarsePointer(): boolean {
  const anyCoarsePointer = useMediaQuery("(any-pointer: coarse)");
  const hasTouchPoints = useSyncExternalStore(
    NEVER_CHANGES,
    () => navigator.maxTouchPoints > 0,
    () => false
  );
  return anyCoarsePointer || hasTouchPoints;
}
