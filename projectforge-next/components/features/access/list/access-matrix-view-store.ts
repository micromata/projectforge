import { create } from "zustand";

/**
 * How the access list renders the permission matrix per row: the compact single line of icons
 * ([AccessMatrixSummary], the default) or the full labelled matrix inline ([AccessMatrixCell]).
 *
 * A store rather than local state because the two readers sit in unrelated trees — the pill lives in
 * the toolbar's `listActions` slot, the matrix in the table's column cells, and neither is handed the
 * other's state (see PageDef.listActions / EntityListPage). Feature-local (not under store/) since only
 * the access list reads it. The module-level store keeps the view across client navigation (opening an
 * entry and coming back), so the choice is remembered for the page; it is deliberately **not**
 * persisted, so a full reload falls back to the compact default.
 */
export type AccessMatrixView = "compact" | "detail";

interface AccessMatrixViewState {
  view: AccessMatrixView;
  setView: (view: AccessMatrixView) => void;
}

export const useAccessMatrixViewStore = create<AccessMatrixViewState>(
  (set) => ({
    view: "compact",
    setView: (view) => set({ view }),
  })
);
