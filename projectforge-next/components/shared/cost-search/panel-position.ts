import type { PanelPosition } from "@/store/cost-search-panel-store";

interface Size {
  width: number;
  height: number;
}

/**
 * The position moved back into the viewport, so the panel's title bar stays reachable after the window
 * shrank (or the position was stored on a larger screen). Returns the same object when nothing changes,
 * so storing it again notifies nobody.
 */
export function clampToViewport(
  position: PanelPosition,
  panel: Size,
  viewport: Size
): PanelPosition {
  const x = Math.max(0, Math.min(position.x, viewport.width - panel.width));
  const y = Math.max(0, Math.min(position.y, viewport.height - panel.height));
  return x === position.x && y === position.y ? position : { x, y };
}
