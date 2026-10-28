"use client";

import { useCallback, useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { DndContext, type DragEndEvent } from "@dnd-kit/core";
import { restrictToWindowEdges } from "@dnd-kit/modifiers";
import { useIsMobile } from "@/hooks/use-mobile";
import { useCostSearchPanelStore } from "@/store/cost-search-panel-store";
import { CostSearchConverter } from "./cost-search-converter";
import { CostSearchPanelFrame } from "./cost-search-panel-frame";
import { clampToViewport } from "./panel-position";

const viewport = () => ({
  width: window.innerWidth,
  height: window.innerHeight,
});

/**
 * The cost search floating over whatever page is shown, for finance users who need a cost unit's
 * number while working elsewhere. Dragged by its title bar (desktop), docked to the bottom on a phone.
 * Where it was put is stored (useCostSearchPanelStore) and pulled back into the window when that
 * shrinks, so the title bar never ends up out of reach.
 */
export function CostSearchPanel() {
  const t = useTranslations();
  const isMobile = useIsMobile();
  const position = useCostSearchPanelStore((s) => s.position);
  const setPosition = useCostSearchPanelStore((s) => s.setPosition);
  const collapsed = useCostSearchPanelStore((s) => s.collapsed);
  const nodeRef = useRef<HTMLElement | null>(null);

  const clamp = useCallback(() => {
    const node = nodeRef.current;
    const current = useCostSearchPanelStore.getState().position;
    if (!node || !current) return;
    const rect = node.getBoundingClientRect();
    const clamped = clampToViewport(current, rect, viewport());
    if (clamped !== current) setPosition(clamped);
  }, [setPosition]);

  useEffect(() => {
    if (isMobile) return;
    clamp();
    window.addEventListener("resize", clamp);
    return () => window.removeEventListener("resize", clamp);
  }, [clamp, isMobile]);

  function handleDragEnd(event: DragEndEvent) {
    const rect = event.active.rect.current.translated;
    if (!rect) return;
    setPosition(
      clampToViewport({ x: rect.left, y: rect.top }, rect, viewport())
    );
  }

  return (
    <DndContext modifiers={[restrictToWindowEdges]} onDragEnd={handleDragEnd}>
      <CostSearchPanelFrame
        title={t("menu.fibu.kostSearch")}
        position={position}
        draggable={!isMobile}
        collapsed={collapsed}
        nodeRef={nodeRef}
      >
        <CostSearchConverter />
      </CostSearchPanelFrame>
    </DndContext>
  );
}
