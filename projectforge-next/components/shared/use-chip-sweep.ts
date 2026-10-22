"use client";

import { useRef, type MouseEvent, type PointerEvent } from "react";
import { sweptIndices, type Point } from "@/lib/chip-sweep";

/** How far the pointer moves before a press in a gap is a sweep rather than a click. */
const SWEEP_THRESHOLD_PX = 4;

/** Whether a press starts a sweep: on the box outside the chips — its padding, the gaps, the trigger. */
function startsSweep(
  event: PointerEvent<HTMLElement> | MouseEvent<HTMLElement>
) {
  return (
    event.button === 0 && !(event.target as Element).closest("[data-chip-id]")
  );
}

/**
 * Selects chips as text is selected: pressed in a gap between them (or before the first, or behind
 * the last) and dragged to the last chip wanted. The chips swept over are selected as a whole
 * ([sweptIndices]), so the copy that follows ([useChipTransfer]) takes exactly those — a native text
 * selection would take half names and run on into the labels around the field.
 *
 * A press on a chip drags the chip to another picker instead. The trigger fills the rest of the last
 * line, so a press behind the last chip lands on it: without moving, it is the click that opens the
 * search; moved, it sweeps, and the click that follows is swallowed. A touch starts nothing, so the page
 * still scrolls. A click in a gap without moving clears the selection.
 */
export function useChipSweep({
  setRange,
  clear,
}: {
  setRange: (ids: number[]) => void;
  clear: () => void;
}) {
  const start = useRef<Point | null>(null);
  const swept = useRef(false);

  const sweep = (box: HTMLElement, current: Point) => {
    const chips = Array.from(
      box.querySelectorAll<HTMLElement>("[data-chip-id]")
    );
    const [from, to] = sweptIndices(
      chips.map((chip) => chip.getBoundingClientRect()),
      start.current!,
      current
    );
    setRange(chips.slice(from, to).map((chip) => Number(chip.dataset.chipId)));
  };

  return {
    // The press must neither select text nor leave the focus where it was: the copy keys are the box's.
    onMouseDown: (event: MouseEvent<HTMLElement>) => {
      if (!startsSweep(event)) return;
      event.preventDefault();
      event.currentTarget.focus();
    },
    onPointerDown: (event: PointerEvent<HTMLElement>) => {
      if (!startsSweep(event) || event.pointerType === "touch") return;
      start.current = { x: event.clientX, y: event.clientY };
      swept.current = false;
    },
    onPointerMove: (event: PointerEvent<HTMLElement>) => {
      const from = start.current;
      if (!from) return;
      const current = { x: event.clientX, y: event.clientY };
      if (!swept.current) {
        if (
          Math.hypot(current.x - from.x, current.y - from.y) <
          SWEEP_THRESHOLD_PX
        )
          return;
        // Captured only now: a capture from the press on would take the trigger its click.
        swept.current = true;
        event.currentTarget.setPointerCapture(event.pointerId);
      }
      sweep(event.currentTarget, current);
    },
    onPointerUp: (event: PointerEvent<HTMLElement>) => {
      if (!start.current) return;
      if (!swept.current && event.target === event.currentTarget) clear();
      start.current = null;
      if (event.currentTarget.hasPointerCapture(event.pointerId))
        event.currentTarget.releasePointerCapture(event.pointerId);
    },
    onPointerCancel: () => {
      start.current = null;
      swept.current = false;
    },
    // The click ending a sweep is none: on the trigger it would open the search, on a chip select it alone.
    onClickCapture: (event: MouseEvent<HTMLElement>) => {
      if (!swept.current) return;
      swept.current = false;
      event.preventDefault();
      event.stopPropagation();
    },
  };
}
