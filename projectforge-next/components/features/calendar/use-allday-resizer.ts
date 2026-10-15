"use client";

import { useEffect, type RefObject } from "react";

/** Never let the all-day area collapse below this, so its label and the drag handle stay grabbable. */
const MIN_HEIGHT = 28;
/** Cap the grab at a share of the calendar so the timed grid always keeps room below. */
const MAX_FRACTION = 0.7;

/**
 * Makes the all-day block of the time-grid views drag-resizable. FullCalendar renders that block at its
 * natural (unbounded) height; globals.css caps it via `--pf-allday-max-height` and lets it scroll. This
 * hook turns the divider FullCalendar draws between the all-day block and the timed grid
 * (`.fc-timegrid-divider`) into a row-resize handle: dragging it rewrites that variable on the container,
 * trading vertical space between the two areas.
 *
 * It listens on the container by delegation (`closest`), not on the divider itself, so it survives the
 * DOM FullCalendar rebuilds on every view/date change without re-attaching, and it silently no-ops in the
 * month/list views where no divider exists. The dragged height is session-local — it is not persisted to
 * the calendar filter (that would cost a backend setting), so it resets on reload.
 */
export function useAllDayResizer(containerRef: RefObject<HTMLElement | null>) {
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let startY = 0;
    let startHeight = 0;

    const onPointerMove = (e: PointerEvent) => {
      const next = Math.min(
        Math.max(MIN_HEIGHT, startHeight + (e.clientY - startY)),
        container.clientHeight * MAX_FRACTION
      );
      container.style.setProperty("--pf-allday-max-height", `${next}px`);
    };

    const onPointerUp = () => {
      document.removeEventListener("pointermove", onPointerMove);
      document.removeEventListener("pointerup", onPointerUp);
      document.body.style.removeProperty("cursor");
      document.body.style.removeProperty("user-select");
    };

    const onPointerDown = (e: PointerEvent) => {
      const target = e.target as HTMLElement | null;
      if (!target?.closest(".fc-timegrid-divider")) return;
      // The all-day chunk is the only `.fc-daygrid-body` inside a time-grid view; its enclosing scroller
      // is the element the CSS caps, so its current height is the natural start of the drag.
      const scroller = container
        .querySelector(".fc-timegrid .fc-daygrid-body")
        ?.closest<HTMLElement>(".fc-scroller");
      if (!scroller) return;
      e.preventDefault();
      startY = e.clientY;
      startHeight = scroller.getBoundingClientRect().height;
      document.body.style.cursor = "row-resize";
      document.body.style.userSelect = "none";
      document.addEventListener("pointermove", onPointerMove);
      document.addEventListener("pointerup", onPointerUp);
    };

    container.addEventListener("pointerdown", onPointerDown);
    return () => {
      container.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("pointermove", onPointerMove);
      document.removeEventListener("pointerup", onPointerUp);
    };
  }, [containerRef]);
}
