"use client";

import { useState } from "react";
import {
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  pointerWithin,
  useSensor,
  useSensors,
  type CollisionDetection,
  type DragEndEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { sortableKeyboardCoordinates } from "@dnd-kit/sortable";
import type { MenuItem } from "@/lib/rs/types";
import { applyDrop } from "./menu-tree";
import type { DragData } from "./types";
import { dropsAfter } from "./use-drop-target";

/**
 * The drop target under the pointer, the innermost first (an entry, then the group around it, then the
 * panel); the nearest one when the pointer is over none, e.g. dragging with the keyboard.
 */
const collisionDetection: CollisionDetection = (args) => {
  const within = pointerWithin(args);
  return within.length > 0 ? within : closestCenter(args);
};

/** dnd-kit wiring of the customizer: sensors, collision detection and the drop, applied via [update]. */
export function useMenuDragDrop(
  update: (change: (menu: MenuItem[]) => MenuItem[]) => void
) {
  const [dragged, setDragged] = useState<MenuItem | null>(null);
  const sensors = useSensors(
    // A small threshold, so a click (adding a template entry, removing one) isn't taken for a drag.
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  const onDragStart = ({ active }: DragStartEvent) =>
    setDragged((active.data.current as DragData | undefined)?.item ?? null);

  const onDragEnd = ({ active, over }: DragEndEvent) => {
    setDragged(null);
    const activeData = active.data.current as DragData | undefined;
    if (!activeData || !over || active.id === over.id) return;
    update((menu) =>
      applyDrop(
        menu,
        activeData,
        over.data.current as DragData | undefined,
        dropsAfter(active, over)
      )
    );
  };

  return {
    dragged,
    contextProps: {
      sensors,
      collisionDetection,
      onDragStart,
      onDragEnd,
      onDragCancel: () => setDragged(null),
    },
  };
}
